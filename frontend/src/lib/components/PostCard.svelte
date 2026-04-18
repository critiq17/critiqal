<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { slide, fade } from 'svelte/transition';
	import type { Post, Comment, ReactionsMap, ReactionType } from '$lib/types';
	import { postService } from '$lib/services';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import PhotoCarousel from '$lib/components/PhotoCarousel.svelte';
	import { viewTracker } from '$lib/utils/viewTracker';

	interface Props {
		post: Post;
		onDeleted?: (postId: number) => void;
	}

	let { post, onDeleted }: Props = $props();

	// --- Reactions ---
	let reactions = $state<ReactionsMap>({ GIGACHAD: 0, THE_ROCK: 0, DAVID: 0 });
	let myReaction = $state<ReactionType | null>(null);
	let reactionsLoaded = $state(false);
	let isReacting = $state(false);
	let reactionsLoading = $state(false);

	// --- Comments ---
	let isExpanded = $state(false);
	let comments = $state<Comment[]>([]);
	let commentsLoaded = $state(false);
	let commentsLoading = $state(false);
	let newComment = $state('');
	let isSubmitting = $state(false);
	let deletingId = $state<number | null>(null);
	let deletingReplyId = $state<number | null>(null);

	// --- Reply state — keyed by root comment id ---
	interface ReplyState {
		replies: Comment[];
		loaded: boolean;
		loading: boolean;
		expanded: boolean;
		composerOpen: boolean;
		draft: string;
		submitting: boolean;
	}

	function emptyReplyState(): ReplyState {
		return {
			replies: [],
			loaded: false,
			loading: false,
			expanded: false,
			composerOpen: false,
			draft: '',
			submitting: false
		};
	}

	let replyStates = $state<Map<number, ReplyState>>(new Map());

	function getReplyState(commentId: number): ReplyState {
		return replyStates.get(commentId) ?? emptyReplyState();
	}

	function setReplyState(commentId: number, patch: Partial<ReplyState>): void {
		const prev = getReplyState(commentId);
		const next = new Map(replyStates);
		next.set(commentId, { ...prev, ...patch });
		replyStates = next;
	}

	// --- Post delete ---
	let isDeleting = $state(false);

	// --- View tracking ---
	let articleElement = $state<HTMLElement | null>(null);
	let cleanupViewTracker: (() => void) | null = null;

	const isOwnPost = $derived(authStore.user?.id === post.author.id);

	onMount(() => {
		loadReactions();
		if (articleElement) {
			cleanupViewTracker = viewTracker.observe(articleElement, post.id, authStore.isAuthenticated);
		}
	});

	onDestroy(() => {
		cleanupViewTracker?.();
	});

	async function loadReactions(): Promise<void> {
		if (reactionsLoading) return;
		reactionsLoading = true;
		try {
			const reactionsPromise = postService.getReactions(post.id);
			const myReactionPromise = authStore.isAuthenticated
				? postService.getMyReaction(post.id)
				: Promise.resolve(undefined);

			const [data, mine] = await Promise.all([reactionsPromise, myReactionPromise]);
			const defaults: ReactionsMap = { GIGACHAD: 0, THE_ROCK: 0, DAVID: 0 };
			reactions = { ...defaults, ...data };
			myReaction = mine ?? null;
			reactionsLoaded = true;
		} catch {
			reactionsLoaded = true;
		} finally {
			reactionsLoading = false;
		}
	}

	async function handleReaction(type: ReactionType): Promise<void> {
		if (!authStore.isAuthenticated || isReacting) return;

		const prevReaction = myReaction;
		const prevReactions = { ...reactions };

		if (myReaction === type) {
			myReaction = null;
			reactions = { ...reactions, [type]: Math.max(0, reactions[type] - 1) };
			isReacting = true;
			try {
				await postService.removeReaction(post.id);
			} catch {
				myReaction = prevReaction;
				reactions = prevReactions;
			} finally {
				isReacting = false;
			}
		} else {
			if (myReaction) {
				reactions = { ...reactions, [myReaction]: Math.max(0, reactions[myReaction] - 1) };
			}
			myReaction = type;
			reactions = { ...reactions, [type]: reactions[type] + 1 };
			isReacting = true;
			try {
				await postService.react(post.id, type);
			} catch {
				myReaction = prevReaction;
				reactions = prevReactions;
			} finally {
				isReacting = false;
			}
		}
	}

	async function toggleComments(): Promise<void> {
		isExpanded = !isExpanded;
		if (isExpanded && !commentsLoaded) {
			await loadComments();
		}
	}

	async function loadComments(): Promise<void> {
		commentsLoading = true;
		try {
			comments = await postService.getComments(post.id);
			commentsLoaded = true;
		} catch {
			commentsLoaded = true;
		} finally {
			commentsLoading = false;
		}
	}

	async function submitComment(): Promise<void> {
		const text = newComment.trim();
		if (!text || isSubmitting) return;
		isSubmitting = true;
		try {
			const comment = await postService.addComment(post.id, { content: text });
			comments = [...comments, comment];
			newComment = '';
		} catch {
			// ignore
		} finally {
			isSubmitting = false;
		}
	}

	function handleCommentKey(e: KeyboardEvent): void {
		if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
			submitComment();
		}
	}

	async function deleteComment(commentId: number): Promise<void> {
		if (deletingId !== null) return;
		deletingId = commentId;
		try {
			await postService.deleteComment(post.id, commentId);
			comments = comments.filter((c) => c.id !== commentId);
			// also drop its reply state
			const next = new Map(replyStates);
			next.delete(commentId);
			replyStates = next;
		} catch {
			// ignore
		} finally {
			deletingId = null;
		}
	}

	async function deleteReply(commentId: number, replyId: number): Promise<void> {
		if (deletingReplyId !== null) return;
		deletingReplyId = replyId;
		try {
			await postService.deleteComment(post.id, replyId);
			const state = getReplyState(commentId);
			setReplyState(commentId, { replies: state.replies.filter((r) => r.id !== replyId) });
		} catch {
			// ignore
		} finally {
			deletingReplyId = null;
		}
	}

	async function toggleReplies(commentId: number): Promise<void> {
		const state = getReplyState(commentId);

		if (!state.loaded) {
			setReplyState(commentId, { loading: true });
			try {
				const replies = await postService.getReplies(post.id, commentId);
				// auto-expand: always show after load
				setReplyState(commentId, { replies, loaded: true, loading: false, expanded: true });
			} catch {
				setReplyState(commentId, { loading: false, loaded: true, expanded: true });
			}
		} else {
			setReplyState(commentId, { expanded: !state.expanded });
		}
	}

	function openReplyComposer(commentId: number): void {
		setReplyState(commentId, { composerOpen: true });
	}

	function closeReplyComposer(commentId: number): void {
		setReplyState(commentId, { composerOpen: false, draft: '' });
	}

	function handleReplyDraftChange(commentId: number, value: string): void {
		setReplyState(commentId, { draft: value });
	}

	function handleReplyKey(e: KeyboardEvent, commentId: number): void {
		if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
			submitReply(commentId);
		}
		if (e.key === 'Escape') {
			closeReplyComposer(commentId);
		}
	}

	async function submitReply(commentId: number): Promise<void> {
		const state = getReplyState(commentId);
		const text = state.draft.trim();
		if (!text || state.submitting) return;

		setReplyState(commentId, { submitting: true });
		try {
			const reply = await postService.addReply(post.id, commentId, { content: text });
			// Re-read state after await: toggleReplies may have loaded replies concurrently.
			const currentState = getReplyState(commentId);
			const updatedReplies = [...currentState.replies, reply];
			setReplyState(commentId, {
				replies: updatedReplies,
				loaded: true,
				expanded: true,
				composerOpen: false,
				draft: '',
				submitting: false
			});
		} catch {
			setReplyState(commentId, { submitting: false });
		}
	}

	async function deletePost(): Promise<void> {
		if (isDeleting) return;
		isDeleting = true;
		try {
			await postService.delete(post.id);
			onDeleted?.(post.id);
		} catch {
			isDeleting = false;
		}
	}

	function formatTime(isoString: string): string {
		const date = new Date(isoString);
		const now = new Date();
		const diffMs = now.getTime() - date.getTime();
		const diffMins = Math.floor(diffMs / 60_000);
		const diffHours = Math.floor(diffMins / 60);
		const diffDays = Math.floor(diffHours / 24);

		if (diffMins < 1) return 'just now';
		if (diffMins < 60) return `${diffMins}m`;
		if (diffHours < 24) return `${diffHours}h`;
		if (diffDays < 7) return `${diffDays}d`;
		return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
	}

	function getInitial(user: Post['author']): string {
		return (user.name ?? user.username).charAt(0).toUpperCase();
	}

	function formatViews(n: number): string {
		if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
		if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
		return String(n);
	}

	const totalComments = $derived(commentsLoaded ? comments.length : 0);

	const hasActiveReactions = $derived(
		myReaction !== null ||
		(reactionsLoaded && (reactions.GIGACHAD > 0 || reactions.THE_ROCK > 0 || reactions.DAVID > 0))
	);
</script>

<article class="post-card" bind:this={articleElement}>
	<div class="post-header">
		<a href="/{post.author.username}" class="author-link" aria-label="View {post.author.username}'s profile">
			<div class="avatar" aria-hidden="true">
				{#if post.author.avatarUrl}
					<img src={post.author.avatarUrl} alt={post.author.username} class="avatar-img" />
				{:else}
					<span class="avatar-initial">{getInitial(post.author)}</span>
				{/if}
			</div>
			<div class="author-meta">
				<span class="author-name">{post.author.name ?? post.author.username}</span>
				<span class="author-username">@{post.author.username}</span>
			</div>
		</a>
		{#if isOwnPost}
			<button
				class="delete-post-btn"
				onclick={deletePost}
				disabled={isDeleting}
				aria-label="Delete post"
				title="Delete post"
			>
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
					<polyline points="3 6 5 6 21 6" />
					<path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
					<path d="M10 11v6M14 11v6" />
					<path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
				</svg>
			</button>
		{/if}
	</div>

	<div class="post-body">
		<p class="post-content">{post.content}</p>
	</div>

	{#if post.photos && post.photos.length > 0}
		<div class="post-photo-wrap">
			<PhotoCarousel photos={post.photos} />
		</div>
	{/if}

	<div class="post-footer">
		<div class="reactions-bar" class:reactions-active={hasActiveReactions}>
			<!-- GIGACHAD reaction -->
			<button
				class="reaction-btn"
				class:active={myReaction === 'GIGACHAD'}
				onclick={() => handleReaction('GIGACHAD')}
				disabled={!authStore.isAuthenticated || isReacting}
				aria-label="GIGACHAD reaction"
				aria-pressed={myReaction === 'GIGACHAD'}
				title={authStore.isAuthenticated ? 'GIGACHAD' : 'Sign in to react'}
			>
				<img src="/assets/reactions/GIGACHAD.png" alt="GIGACHAD" class="reaction-img" />
				{#if reactionsLoaded && reactions.GIGACHAD > 0}
					<span class="reaction-count">{reactions.GIGACHAD}</span>
				{/if}
			</button>

			<!-- THE_ROCK reaction -->
			<button
				class="reaction-btn"
				class:active={myReaction === 'THE_ROCK'}
				onclick={() => handleReaction('THE_ROCK')}
				disabled={!authStore.isAuthenticated || isReacting}
				aria-label="THE ROCK reaction"
				aria-pressed={myReaction === 'THE_ROCK'}
				title={authStore.isAuthenticated ? 'THE ROCK' : 'Sign in to react'}
			>
				<img src="/assets/reactions/THEROCK.png" alt="THE ROCK" class="reaction-img" />
				{#if reactionsLoaded && reactions.THE_ROCK > 0}
					<span class="reaction-count">{reactions.THE_ROCK}</span>
				{/if}
			</button>
		</div>

		<!-- Comments toggle -->
		<button class="action-btn comment-btn" onclick={toggleComments} aria-label="Toggle comments">
			<svg
				class="icon"
				viewBox="0 0 24 24"
				fill="none"
				stroke="currentColor"
				stroke-width="2"
				aria-hidden="true"
			>
				<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
			</svg>
			{#if totalComments > 0}
				<span class="action-count">{totalComments}</span>
			{/if}
		</button>

		<div class="post-meta">
			<span class="view-count" title="Views">
				<svg class="icon-sm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
					<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
					<circle cx="12" cy="12" r="3" />
				</svg>
				{formatViews(post.viewCount)}
			</span>
			<time class="post-timestamp" datetime={post.createdAt}>
				{formatTime(post.createdAt)}
			</time>
		</div>
	</div>

	{#if isExpanded}
		<div class="comments-panel" role="region" aria-label="Comments" transition:slide={{ duration: 200 }}>
			{#if commentsLoading}
				<div class="comments-loading">
					<span class="loading-dot"></span>
					<span class="loading-dot"></span>
					<span class="loading-dot"></span>
				</div>
			{:else}
				{#if comments.length > 0}
					<ul class="comments-list">
						{#each comments as comment (comment.id)}
							{@const rs = getReplyState(comment.id)}
							<li class="comment-item">
								<div class="comment-avatar" aria-hidden="true">
									{#if comment.author.avatarUrl}
										<img src={comment.author.avatarUrl} alt={comment.author.username} class="comment-avatar-img" />
									{:else}
										<span class="comment-avatar-initial">
											{(comment.author.name ?? comment.author.username).charAt(0).toUpperCase()}
										</span>
									{/if}
								</div>
								<div class="comment-body">
									<div class="comment-header">
										<a href="/{comment.author.username}" class="comment-author">{comment.author.name ?? comment.author.username}</a>
										<time class="comment-time" datetime={comment.createdAt}>{formatTime(comment.createdAt)}</time>
										{#if authStore.user?.id === comment.author.id}
											<button
												class="comment-delete-btn"
												onclick={() => deleteComment(comment.id)}
												disabled={deletingId === comment.id}
												aria-label="Delete comment"
											>
												<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
													<line x1="18" y1="6" x2="6" y2="18" />
													<line x1="6" y1="6" x2="18" y2="18" />
												</svg>
											</button>
										{/if}
									</div>
									<p class="comment-content">{comment.content}</p>

									<!-- Reply actions row -->
									<div class="comment-actions">
										{#if authStore.isAuthenticated && !rs.composerOpen}
											<button
												class="reply-trigger"
												onclick={() => openReplyComposer(comment.id)}
												aria-label="Reply to comment"
											>
												reply
											</button>
										{/if}
										{#if rs.loading}
											<span class="replies-loading">
												<span class="loading-dot"></span>
												<span class="loading-dot"></span>
												<span class="loading-dot"></span>
											</span>
										{:else if !rs.loaded}
											<button
												class="replies-toggle"
												onclick={() => toggleReplies(comment.id)}
												aria-label="Load replies"
											>
												Replies ›
											</button>
										{:else if rs.loaded && rs.replies.length >= 2 && !rs.expanded}
											<button
												class="replies-toggle"
												onclick={() => toggleReplies(comment.id)}
												aria-expanded="false"
											>
												&#9658; {rs.replies.length} replies
											</button>
										{:else if rs.loaded && rs.expanded && rs.replies.length >= 2}
											<button
												class="replies-toggle"
												onclick={() => setReplyState(comment.id, { expanded: false })}
												aria-expanded="true"
											>
												&#9660; hide replies
											</button>
										{/if}
									</div>

									<!-- Inline replies (always visible when exactly 1, or when expanded) -->
									{#if rs.loaded && rs.replies.length > 0 && (rs.replies.length === 1 || rs.expanded)}
										<ul class="replies-list" aria-label="Replies" transition:slide={{ duration: 180 }}>
											{#each rs.replies as reply (reply.id)}
												<li class="reply-item" transition:fade={{ duration: 150 }}>
													<div class="reply-avatar" aria-hidden="true">
														{#if reply.author.avatarUrl}
															<img src={reply.author.avatarUrl} alt={reply.author.username} class="reply-avatar-img" />
														{:else}
															<span class="reply-avatar-initial">
																{(reply.author.name ?? reply.author.username).charAt(0).toUpperCase()}
															</span>
														{/if}
													</div>
													<div class="reply-body">
														<div class="reply-header">
															<a href="/{reply.author.username}" class="reply-author">{reply.author.name ?? reply.author.username}</a>
															<time class="reply-time" datetime={reply.createdAt}>{formatTime(reply.createdAt)}</time>
															{#if authStore.user?.id === reply.author.id}
																<button
																	class="comment-delete-btn"
																	onclick={() => deleteReply(comment.id, reply.id)}
																	disabled={deletingReplyId === reply.id}
																	aria-label="Delete reply"
																>
																	<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
																		<line x1="18" y1="6" x2="6" y2="18" />
																		<line x1="6" y1="6" x2="18" y2="18" />
																	</svg>
																</button>
															{/if}
														</div>
														<p class="reply-content">{reply.content}</p>
													</div>
												</li>
											{/each}
										</ul>
									{/if}

									<!-- Inline reply composer -->
									{#if rs.composerOpen}
										<div class="reply-compose" transition:slide={{ duration: 150 }}>
											<div class="reply-compose-inner">
												<textarea
													class="reply-input"
													value={rs.draft}
													oninput={(e) => handleReplyDraftChange(comment.id, (e.target as HTMLTextAreaElement).value)}
													onkeydown={(e) => handleReplyKey(e, comment.id)}
													placeholder="Reply… (Esc to cancel)"
													rows={1}
													disabled={rs.submitting}
													aria-label="Write a reply"
												></textarea>
												<button
													class="reply-submit-btn"
													onclick={() => submitReply(comment.id)}
													disabled={!rs.draft.trim() || rs.submitting}
													aria-label="Send reply"
												>
													{rs.submitting ? '…' : 'Reply'}
												</button>
											</div>
										</div>
									{/if}
								</div>
							</li>
						{/each}
					</ul>
				{:else}
					<p class="no-comments">No comments yet.</p>
				{/if}

				{#if authStore.isAuthenticated}
					<div class="comment-compose" transition:slide={{ duration: 200 }}>
						<div class="comment-compose-inner">
							<textarea
								class="comment-input"
								bind:value={newComment}
								onkeydown={handleCommentKey}
								placeholder="Add a comment…"
								rows={1}
								disabled={isSubmitting}
								aria-label="Write a comment"
							></textarea>
							<button
								class="comment-submit-btn"
								onclick={submitComment}
								disabled={!newComment.trim() || isSubmitting}
								aria-label="Post comment"
							>
								{isSubmitting ? '…' : 'Post'}
							</button>
						</div>
					</div>
				{/if}
			{/if}
		</div>
	{/if}
</article>

<style>
	.post-card {
		padding: 1.25rem 0.5rem;
		margin: 0 -0.5rem;
		border-radius: 0.5rem;
		transition: background-color 0.15s ease;
	}

	.post-card:hover {
		background-color: var(--color-surface-raised);
	}

	/* Header */
	.post-header {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		margin-bottom: 0.75rem;
	}

	.author-link {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		text-decoration: none;
		border-radius: 0.375rem;
		transition: opacity 0.15s ease;
	}

	.author-link:hover {
		opacity: 0.75;
	}

	.avatar {
		width: 2.5rem;
		height: 2.5rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.avatar-initial {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--color-text-muted);
		user-select: none;
	}

	.author-meta {
		display: flex;
		flex-direction: column;
		gap: 0.125rem;
		flex: 1;
	}

	.author-name {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text-primary);
		line-height: 1.2;
	}

	.author-username {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		line-height: 1.2;
	}

	.delete-post-btn {
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-muted);
		padding: 0.25rem;
		border-radius: 0.25rem;
		display: flex;
		align-items: center;
		opacity: 0;
		transition: opacity 0.15s ease, color 0.15s ease;
	}

	.delete-post-btn svg {
		width: 0.9375rem;
		height: 0.9375rem;
	}

	.post-card:hover .delete-post-btn {
		opacity: 1;
	}

	.delete-post-btn:hover {
		color: var(--color-accent);
	}

	/* Body */
	.post-body {
		padding-left: 3.25rem;
		margin-bottom: 0.875rem;
	}

	.post-content {
		font-size: 0.9375rem;
		line-height: 1.55;
		color: var(--color-text-primary);
		margin: 0;
		white-space: pre-wrap;
		word-break: break-word;
	}

	.post-photo-wrap {
		margin: 0.75rem 0 0.875rem 3.25rem;
		border-radius: 0.75rem;
		overflow: hidden;
		background: var(--color-surface-raised);
	}

	@keyframes fadeIn {
		from {
			opacity: 0;
		}
		to {
			opacity: 1;
		}
	}

	/* Footer */
	.post-footer {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		padding-left: 3.25rem;
	}

	/* Reactions bar — hidden by default, revealed on hover or when active */
	.reactions-bar {
		display: flex;
		align-items: center;
		gap: 0.125rem;
		max-width: 0;
		overflow: hidden;
		opacity: 0;
		transform: translateY(4px);
		transition: opacity 0.18s ease, transform 0.18s ease, max-width 0.2s ease;
		pointer-events: none;
	}

	.post-card:hover .reactions-bar,
	.reactions-bar.reactions-active {
		max-width: 8rem;
		opacity: 1;
		transform: translateY(0);
		pointer-events: auto;
	}

	/* Reaction buttons */
	.reaction-btn {
		display: flex;
		align-items: center;
		gap: 0.3rem;
		background: none;
		border: none;
		cursor: pointer;
		padding: 0.25rem 0.375rem;
		border-radius: 0.375rem;
		transition: background-color 0.15s ease, transform 0.1s ease;
	}

	.reaction-btn:disabled {
		cursor: default;
	}

	.reaction-btn:not(:disabled):hover {
		background-color: var(--color-surface-raised);
	}

	.reaction-btn:not(:disabled):active {
		transform: scale(0.9);
	}

	.reaction-img {
		width: 2rem;
		height: 2rem;
		border-radius: 50%;
		object-fit: cover;
		display: block;
		opacity: 0.75;
		transition: opacity 0.15s ease, transform 0.15s ease;
	}

	.reaction-btn.active .reaction-img,
	.reaction-btn:not(:disabled):hover .reaction-img {
		opacity: 1;
		transform: scale(1.1);
	}

	.reaction-count {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		min-width: 0.75rem;
	}

	.reaction-btn.active .reaction-count {
		color: var(--color-text-primary);
		font-weight: 600;
	}

	/* Action buttons (comments) */
	.action-btn {
		display: flex;
		align-items: center;
		gap: 0.3125rem;
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-muted);
		font-size: 0.8125rem;
		padding: 0.25rem 0.375rem;
		border-radius: 0.375rem;
		transition: color 0.15s ease, background-color 0.15s ease, transform 0.1s ease;
	}

	.action-btn:hover {
		color: var(--color-text-primary);
		background-color: var(--color-surface-raised);
	}

	.action-btn:active {
		transform: scale(0.92);
	}

	.icon {
		width: 1rem;
		height: 1rem;
		flex-shrink: 0;
	}

	.action-count {
		min-width: 0.75rem;
	}

	/* Meta (views + timestamp) */
	.post-meta {
		display: flex;
		align-items: center;
		gap: 0.625rem;
		margin-left: auto;
	}

	.view-count {
		display: flex;
		align-items: center;
		gap: 0.25rem;
		font-size: 0.8125rem;
		color: var(--color-text-muted);
	}

	.icon-sm {
		width: 0.875rem;
		height: 0.875rem;
		flex-shrink: 0;
	}

	.post-timestamp {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
	}

	/* Comments panel */
	.comments-panel {
		padding: 0.875rem 0 0 3.25rem;
		overflow: hidden;
	}

	.comments-loading {
		display: flex;
		gap: 0.25rem;
		align-items: center;
		padding: 0.5rem 0;
	}

	.loading-dot {
		width: 0.3125rem;
		height: 0.3125rem;
		border-radius: 50%;
		background: var(--color-text-muted);
		animation: blink 1.2s ease-in-out infinite;
	}

	.loading-dot:nth-child(2) {
		animation-delay: 0.2s;
	}

	.loading-dot:nth-child(3) {
		animation-delay: 0.4s;
	}

	.comments-list {
		list-style: none;
		padding: 0;
		margin: 0 0 0.875rem;
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}

	.comment-item {
		display: flex;
		gap: 0.625rem;
	}

	.comment-avatar {
		width: 1.75rem;
		height: 1.75rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.comment-avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.comment-avatar-initial {
		font-size: 0.6875rem;
		font-weight: 600;
		color: var(--color-text-muted);
		user-select: none;
	}

	.comment-body {
		flex: 1;
		min-width: 0;
	}

	.comment-header {
		display: flex;
		align-items: baseline;
		gap: 0.375rem;
		margin-bottom: 0.1875rem;
	}

	.comment-author {
		font-size: 0.8125rem;
		font-weight: 600;
		color: var(--color-text-primary);
		text-decoration: none;
		transition: opacity 0.15s ease;
	}

	.comment-author:hover {
		opacity: 0.7;
	}

	.comment-time {
		font-size: 0.75rem;
		color: var(--color-text-muted);
	}

	.comment-delete-btn {
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-muted);
		padding: 0;
		display: flex;
		align-items: center;
		margin-left: auto;
		opacity: 0;
		transition: opacity 0.15s ease, color 0.15s ease;
	}

	.comment-delete-btn svg {
		width: 0.75rem;
		height: 0.75rem;
	}

	.comment-item:hover .comment-delete-btn {
		opacity: 1;
	}

	.comment-delete-btn:hover {
		color: var(--color-accent);
	}

	.comment-content {
		font-size: 0.875rem;
		line-height: 1.5;
		color: var(--color-text-primary);
		margin: 0;
		word-break: break-word;
	}

	/* Comment actions row (reply trigger + replies toggle) */
	.comment-actions {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		margin-top: 0.25rem;
	}

	.reply-trigger {
		background: none;
		border: none;
		cursor: pointer;
		font-size: 0.75rem;
		color: var(--color-text-muted);
		padding: 0;
		font-family: inherit;
		transition: color 0.15s ease;
	}

	.reply-trigger:hover {
		color: var(--color-text-primary);
	}

	.replies-toggle {
		background: none;
		border: none;
		cursor: pointer;
		font-size: 0.75rem;
		color: var(--color-text-muted);
		padding: 0;
		font-family: inherit;
		transition: color 0.15s ease;
	}

	.replies-toggle:hover {
		color: var(--color-text-primary);
	}


	.replies-loading {
		display: flex;
		gap: 0.2rem;
		align-items: center;
	}

	/* Inline replies list */
	.replies-list {
		list-style: none;
		padding: 0;
		margin: 0.5rem 0 0;
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
		border-left: 2px solid var(--color-border);
		padding-left: 0.75rem;
	}

	.reply-item {
		display: flex;
		gap: 0.5rem;
	}

	.reply-avatar {
		width: 1.375rem;
		height: 1.375rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.reply-avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.reply-avatar-initial {
		font-size: 0.5625rem;
		font-weight: 600;
		color: var(--color-text-muted);
		user-select: none;
	}

	.reply-body {
		flex: 1;
		min-width: 0;
	}

	.reply-header {
		display: flex;
		align-items: baseline;
		gap: 0.375rem;
		margin-bottom: 0.125rem;
	}

	.reply-author {
		font-size: 0.75rem;
		font-weight: 600;
		color: var(--color-text-primary);
		text-decoration: none;
		transition: opacity 0.15s ease;
	}

	.reply-author:hover {
		opacity: 0.7;
	}

	.reply-time {
		font-size: 0.6875rem;
		color: var(--color-text-muted);
	}

	.reply-content {
		font-size: 0.8125rem;
		line-height: 1.45;
		color: var(--color-text-primary);
		margin: 0;
		word-break: break-word;
	}

	/* Reply composer — pill style */
	.reply-compose {
		margin-top: 0.5rem;
	}

	.reply-compose-inner {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		border: 1px solid var(--color-border);
		border-radius: 1.5rem;
		padding: 0.3125rem 0.375rem 0.3125rem 0.75rem;
		background: var(--color-surface-raised);
		transition: border-color 0.15s ease;
	}

	.reply-compose-inner:focus-within {
		border-color: var(--color-text-muted);
	}

	.reply-input {
		flex: 1;
		background: none;
		border: none;
		outline: none;
		font-size: 0.8125rem;
		color: var(--color-text-primary);
		font-family: inherit;
		resize: none;
		line-height: 1.5;
		min-height: 1.5em;
		padding: 0;
		box-sizing: border-box;
	}

	.reply-input::placeholder {
		color: var(--color-text-muted);
		opacity: 0.5;
	}

	.reply-input:disabled {
		opacity: 0.6;
	}

	.reply-submit-btn {
		background: var(--color-text-primary);
		color: var(--color-bg);
		border: none;
		border-radius: 9999px;
		padding: 0.25rem 0.625rem;
		font-size: 0.75rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		white-space: nowrap;
		flex-shrink: 0;
		transition: opacity 0.15s ease, transform 0.1s ease;
	}

	.reply-submit-btn:disabled {
		opacity: 0.35;
		cursor: not-allowed;
	}

	.reply-submit-btn:not(:disabled):hover {
		opacity: 0.85;
	}

	.reply-submit-btn:not(:disabled):active {
		transform: scale(0.96);
	}

	.no-comments {
		font-size: 0.875rem;
		color: var(--color-text-muted);
		margin: 0 0 0.875rem;
	}

	/* Comment compose — pill style */
	.comment-compose {
		padding-top: 0.5rem;
	}

	.comment-compose-inner {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		border: 1px solid var(--color-border);
		border-radius: 1.5rem;
		padding: 0.4375rem 0.5rem 0.4375rem 1rem;
		background: var(--color-surface-raised);
		transition: border-color 0.15s ease;
	}

	.comment-compose-inner:focus-within {
		border-color: var(--color-text-muted);
	}

	.comment-input {
		flex: 1;
		background: none;
		border: none;
		outline: none;
		font-size: 0.875rem;
		color: var(--color-text-primary);
		font-family: inherit;
		resize: none;
		line-height: 1.5;
		min-height: 1.5em;
		padding: 0;
	}

	.comment-input::placeholder {
		color: var(--color-text-muted);
		opacity: 0.5;
	}

	.comment-input:disabled {
		opacity: 0.6;
	}

	.comment-submit-btn {
		background: var(--color-text-primary);
		color: var(--color-bg);
		border: none;
		border-radius: 9999px;
		padding: 0.3125rem 0.75rem;
		font-size: 0.8125rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		white-space: nowrap;
		flex-shrink: 0;
		transition: opacity 0.15s ease, transform 0.1s ease;
	}

	.comment-submit-btn:disabled {
		opacity: 0.35;
		cursor: not-allowed;
	}

	.comment-submit-btn:not(:disabled):hover {
		opacity: 0.85;
	}

	.comment-submit-btn:not(:disabled):active {
		transform: scale(0.96);
	}

	@keyframes blink {
		0%, 100% { opacity: 0.3; }
		50% { opacity: 1; }
	}
</style>
