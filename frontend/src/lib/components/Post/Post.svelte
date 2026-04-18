<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { slide, fade } from 'svelte/transition';
	import type { Post, Comment, ReactionsMap, ReactionType } from '$lib/types';
	import { postService } from '$lib/services';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { viewTracker } from '$lib/utils/viewTracker';
	import { getInitials } from '$lib/utils/getInitials';
	import { formatRelativeTime } from '$lib/utils/formatRelativeTime';
	import { REACTION_TYPES, REACTION_VISUALS, DEFAULT_REACTIONS } from '$lib/reactions';

	export type PostVariant = 'desktop' | 'mobile';
	export type PostSize = 'full' | 'compact';

	interface Props {
		post: Post;
		variant?: PostVariant;
		size?: PostSize;
		showOptions?: boolean;
		onDeleted?: (postId: number) => void;
		onEdited?: (post: Post) => void;
		onAuthorClick?: (username: string) => void;
	}

	let {
		post,
		variant = 'desktop',
		size = 'full',
		showOptions,
		onDeleted,
		onEdited,
		onAuthorClick
	}: Props = $props();

	const isMobile = variant === 'mobile';
	const isCompact = size === 'compact';
	const showReactionsBar = !isMobile;
	const showCommentsPanel = !isMobile || isCompact === false;
	const showViewTracking = !isMobile;
	const isOwnPost = $derived(authStore.user?.id === post.author.id);
	const shouldShowOptions = $derived(showOptions ?? (isOwnPost || isMobile));
	const canEdit = false; // Future: implement editing

	// Options menu state
	let showOptionsMenu = $state(false);
	let optionsMenuRef = $state<HTMLDivElement | null>(null);

	function toggleOptionsMenu(): void {
		showOptionsMenu = !showOptionsMenu;
	}

	function closeOptionsMenu(): void {
		showOptionsMenu = false;
	}

	function handleEditClick(): void {
		closeOptionsMenu();
		onEdited?.(post);
	}

	async function handleDeleteClick(): Promise<void> {
		closeOptionsMenu();
		await deletePost();
	}

	// --- State ---
	let reactions = $state<ReactionsMap>({ ...DEFAULT_REACTIONS });
	let myReaction = $state<ReactionType | null>(null);
	let reactionsLoaded = $state(false);
	let isReacting = $state(false);

	let isExpanded = $state(false);
	let comments = $state<Comment[]>([]);
	let commentsLoaded = $state(false);
	let newComment = $state('');
	let isSubmitting = $state(false);
	let deletingId = $state<number | null>(null);

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

	let isDeleting = $state(false);
	let articleElement = $state<HTMLElement | null>(null);
	let cleanupViewTracker: (() => void) | null = null;

	const hasActiveReactions = $derived(
		myReaction !== null ||
		(reactionsLoaded && (reactions.GIGACHAD > 0 || reactions.THE_ROCK > 0 || reactions.DAVID > 0))
	);
	const totalComments = $derived(commentsLoaded ? comments.length : 0);

	// --- Lifecycle ---
	onMount(() => {
		loadReactions();
		if (showViewTracking && articleElement && authStore.isAuthenticated) {
			cleanupViewTracker = viewTracker.observe(articleElement, post.id, authStore.isAuthenticated);
		}

		// Click outside to close options menu (desktop only)
		function handleClickOutside(e: MouseEvent): void {
			if (showOptionsMenu && optionsMenuRef && !optionsMenuRef.contains(e.target as Node)) {
				closeOptionsMenu();
			}
		}
		document.addEventListener('click', handleClickOutside);
		return () => document.removeEventListener('click', handleClickOutside);
	});

	onDestroy(() => {
		cleanupViewTracker?.();
	});

	// --- Actions ---
	async function loadReactions(): Promise<void> {
		if (reactionsLoaded) return;
		try {
			const [data, mine] = await Promise.all([
				postService.getReactions(post.id),
				authStore.isAuthenticated ? postService.getMyReaction(post.id).catch(() => undefined) : Promise.resolve(undefined)
			]);
			reactions = { ...DEFAULT_REACTIONS, ...data };
			myReaction = mine ?? null;
		} catch {
			// ignore
		} finally {
			reactionsLoaded = true;
		}
	}

	async function handleReaction(type: ReactionType): Promise<void> {
		if (!authStore.isAuthenticated || isReacting) return;

		const prevReaction = myReaction;
		const prevReactions = { ...reactions };

		if (myReaction === type) {
			myReaction = null;
			reactions = { ...reactions, [type]: Math.max(0, reactions[type] - 1) };
		} else {
			if (myReaction) {
				reactions = { ...reactions, [myReaction]: Math.max(0, reactions[myReaction] - 1) };
			}
			myReaction = type;
			reactions = { ...reactions, [type]: reactions[type] + 1 };
		}

		isReacting = true;
		try {
			if (prevReaction === type) {
				await postService.removeReaction(post.id);
			} else {
				await postService.react(post.id, type);
			}
		} catch {
			myReaction = prevReaction;
			reactions = prevReactions;
		} finally {
			isReacting = false;
		}
	}

	async function toggleComments(): Promise<void> {
		isExpanded = !isExpanded;
		if (isExpanded && !commentsLoaded) {
			await loadComments();
		}
	}

	async function loadComments(): Promise<void> {
		if (commentsLoaded) return;
		try {
			comments = await postService.getComments(post.id);
		} catch {
			// ignore
		} finally {
			commentsLoaded = true;
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
		try {
			await postService.deleteComment(post.id, replyId);
			const state = getReplyState(commentId);
			setReplyState(commentId, { replies: state.replies.filter((r) => r.id !== replyId) });
		} catch {
			// ignore
		}
	}

	async function toggleReplies(commentId: number): Promise<void> {
		const state = getReplyState(commentId);
		if (!state.loaded) {
			setReplyState(commentId, { loading: true });
			try {
				const replies = await postService.getReplies(post.id, commentId);
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
			const currentState = getReplyState(commentId);
			setReplyState(commentId, {
				replies: [...currentState.replies, reply],
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

	function formatViews(n: number): string {
		if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
		if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
		return String(n);
	}

	function handleAuthorClick(): void {
		if (onAuthorClick) {
			onAuthorClick(post.author.username);
		}
	}
</script>

<article
	class="post-card"
	class:mobile={isMobile}
	class:compact={isCompact}
	bind:this={articleElement}
>
	<!-- Header / Author row -->
	{#if isMobile}
		<div class="author-row">
			<button
				class="author-tap"
				onclick={handleAuthorClick}
				aria-label="View {post.author.username}'s profile"
			>
				<div class="author-avatar">
					{#if post.author.avatarUrl}
						<img src={post.author.avatarUrl} alt={post.author.username} class="avatar-img" />
					{:else}
						<div class="avatar-initials">{getInitials(post.author.name, post.author.username)}</div>
					{/if}
				</div>
				<div class="author-info">
					<span class="author-display">{post.author.name ?? post.author.username}</span>
					<span class="author-username">@{post.author.username}</span>
				</div>
			</button>
			<span class="post-time">{formatRelativeTime(post.createdAt)}</span>
			{#if shouldShowOptions}
				<button class="options-btn" onclick={toggleOptionsMenu} aria-label="Post options">
					<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
						<circle cx="12" cy="6" r="1.5" />
						<circle cx="12" cy="12" r="1.5" />
						<circle cx="12" cy="18" r="1.5" />
					</svg>
				</button>
			{/if}
		</div>
	{:else}
		<div class="post-header">
			<a href="/{post.author.username}" class="author-link" aria-label="View {post.author.username}'s profile">
				<div class="avatar">
					{#if post.author.avatarUrl}
						<img src={post.author.avatarUrl} alt={post.author.username} class="avatar-img" />
					{:else}
						<span class="avatar-initial">{getInitials(post.author.name, post.author.username)}</span>
					{/if}
				</div>
				<div class="author-meta">
					<span class="author-name">{post.author.name ?? post.author.username}</span>
					<span class="author-username">@{post.author.username}</span>
				</div>
			</a>
			{#if shouldShowOptions}
				<div class="options-menu-wrapper" bind:this={optionsMenuRef}>
					<button class="options-btn" onclick={toggleOptionsMenu} aria-label="Post options">
						<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
							<circle cx="12" cy="6" r="1.5" />
							<circle cx="12" cy="12" r="1.5" />
							<circle cx="12" cy="18" r="1.5" />
						</svg>
					</button>
					{#if showOptionsMenu}
						<div class="options-menu" role="menu">
							<button class="options-menu-item" onclick={handleEditClick} role="menuitem" disabled={!canEdit}>
								<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
									<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" />
									<path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9-9z" />
								</svg>
								Edit
							</button>
							<button class="options-menu-item delete" onclick={handleDeleteClick} role="menuitem">
								<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
									<polyline points="3 6 5 6 21 6" />
									<path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6" />
									<path d="M10 11v6M14 11v6" />
									<path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2" />
								</svg>
								Delete
							</button>
						</div>
					{/if}
				</div>
			{/if}
		</div>
	{/if}

	<!-- Content -->
	<div class="post-body">
		<p class="post-content">{post.content}</p>
	</div>

	<!-- Photos -->
	{#if post.photos && post.photos.length > 0}
		{#if isMobile}
			<div class="photo-strip" role="region" aria-label="Post photos">
				{#each [...post.photos].sort((a, b) => a.position - b.position) as photo (photo.id)}
					<div class="photo-item">
						<img src={photo.url} alt="" loading="lazy" />
					</div>
				{/each}
			</div>
			{#if post.photos.length > 1}
				<div class="photo-dots" role="presentation">
					{#each post.photos as _photo, i (i)}
						<div class="photo-dot"></div>
					{/each}
				</div>
			{/if}
		{:else}
			<div class="post-photo-wrap">
				{#each post.photos as photo, i (photo.id)}
					<a href={photo.url} target="_blank" rel="noopener noreferrer" class="photo-link" style="z-index: {post.photos.length - i}">
						<img src={photo.url} alt="" class="post-photo" loading="lazy" />
					</a>
				{/each}
			</div>
		{/if}
	{/if}

	<!-- Footer / Actions -->
	<div class="post-footer">
		{#if showReactionsBar}
			<div class="reactions-bar" class:reactions-active={hasActiveReactions}>
				{#each REACTION_TYPES as type (type)}
					{@const visual = REACTION_VISUALS[type]}
					<button
						class="reaction-btn"
						class:active={myReaction === type}
						onclick={() => handleReaction(type)}
						disabled={!authStore.isAuthenticated || isReacting}
						aria-label="{type} reaction"
						aria-pressed={myReaction === type}
						title={authStore.isAuthenticated ? type : 'Sign in to react'}
					>
						{#if visual.assetPath}
							<img src={visual.assetPath} alt={visual.label} class="reaction-img" />
						{:else}
							<span class="reaction-emoji">{visual.fallbackEmoji}</span>
						{/if}
						{#if reactionsLoaded && reactions[type] > 0}
							<span class="reaction-count">{reactions[type]}</span>
						{/if}
					</button>
				{/each}
			</div>
		{:else}
			<div class="mobile-reactions">
				{#each REACTION_TYPES as type (type)}
					{@const visual = REACTION_VISUALS[type]}
					<button
						class="reaction-btn"
						class:active={myReaction === type}
						onclick={() => handleReaction(type)}
						disabled={!authStore.isAuthenticated || isReacting}
						aria-label="{type} reaction"
					>
						{#if visual.assetPath}
							<img src={visual.assetPath} alt={visual.label} class="reaction-img" />
						{:else}
							<span class="reaction-emoji">{visual.fallbackEmoji}</span>
						{/if}
						{#if reactionsLoaded && reactions[type] > 0}
							<span class="reaction-count">{reactions[type]}</span>
						{/if}
					</button>
				{/each}
				{#if !isCompact}
					<button class="action-btn comment-btn" onclick={toggleComments} aria-label="Comments">
						<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
							<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
						</svg>
					</button>
				{/if}
			</div>
		{/if}

		{#if !isMobile}
			<button class="action-btn comment-btn" onclick={toggleComments} aria-label="Toggle comments">
				<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
					<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
				</svg>
				{#if totalComments > 0}
					<span class="action-count">{totalComments}</span>
				{/if}
			</button>

			<div class="post-meta">
				<span class="view-count">
					<svg class="icon-sm" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
						<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
						<circle cx="12" cy="12" r="3" />
					</svg>
					{formatViews(post.viewCount)}
				</span>
				<time class="post-timestamp" datetime={post.createdAt}>
					{formatRelativeTime(post.createdAt)}
				</time>
			</div>
		{/if}
	</div>

	<!-- Comments panel (desktop or mobile non-compact) -->
	{#if showCommentsPanel && isExpanded}
		<div class="comments-panel" role="region" aria-label="Comments" transition:slide={{ duration: 200 }}>
			{#if commentsLoaded === false}
				<div class="comments-loading">
					<span class="loading-dot"></span>
					<span class="loading-dot"></span>
					<span class="loading-dot"></span>
				</div>
			{:else if comments.length > 0}
				<ul class="comments-list">
					{#each comments as comment (comment.id)}
						{@const rs = getReplyState(comment.id)}
						<li class="comment-item">
							<div class="comment-avatar" aria-hidden="true">
								{#if comment.author.avatarUrl}
									<img src={comment.author.avatarUrl} alt={comment.author.username} class="comment-avatar-img" />
								{:else}
									<span class="comment-avatar-initial">{getInitials(comment.author.name, comment.author.username)}</span>
								{/if}
							</div>
							<div class="comment-body">
								<div class="comment-header">
									<a href="/{comment.author.username}" class="comment-author">{comment.author.name ?? comment.author.username}</a>
									<time class="comment-time" datetime={comment.createdAt}>{formatRelativeTime(comment.createdAt)}</time>
									{#if authStore.user?.id === comment.author.id}
										<button class="comment-delete-btn" onclick={() => deleteComment(comment.id)} disabled={deletingId === comment.id} aria-label="Delete comment">
											<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
												<line x1="18" y1="6" x2="6" y2="18" />
												<line x1="6" y1="6" x2="18" y2="18" />
											</svg>
										</button>
									{/if}
								</div>
								<p class="comment-content">{comment.content}</p>

								<div class="comment-actions">
									{#if authStore.isAuthenticated && !rs.composerOpen}
										<button class="reply-trigger" onclick={() => openReplyComposer(comment.id)}>reply</button>
									{/if}
									{#if rs.loading}
										<span class="replies-loading"><span class="loading-dot"></span><span class="loading-dot"></span><span class="loading-dot"></span></span>
									{:else if !rs.loaded}
										<button class="replies-toggle" onclick={() => toggleReplies(comment.id)}>Replies ›</button>
									{:else if rs.loaded && rs.replies.length >= 2 && !rs.expanded}
										<button class="replies-toggle" onclick={() => toggleReplies(comment.id)}>› {rs.replies.length} replies</button>
									{:else if rs.loaded && rs.expanded && rs.replies.length >= 2}
										<button class="replies-toggle" onclick={() => setReplyState(comment.id, { expanded: false })}>› hide</button>
									{/if}
								</div>

								{#if rs.loaded && rs.replies.length > 0 && (rs.replies.length === 1 || rs.expanded)}
									<ul class="replies-list" aria-label="Replies" transition:slide={{ duration: 180 }}>
										{#each rs.replies as reply (reply.id)}
											<li class="reply-item">
												<div class="reply-avatar">
													{#if reply.author.avatarUrl}
														<img src={reply.author.avatarUrl} alt={reply.author.username} class="reply-avatar-img" />
													{:else}
														<span class="reply-avatar-initial">{getInitials(reply.author.name, reply.author.username)}</span>
													{/if}
												</div>
												<div class="reply-body">
													<div class="reply-header">
														<a href="/{reply.author.username}" class="reply-author">{reply.author.name ?? reply.author.username}</a>
														<time class="reply-time" datetime={reply.createdAt}>{formatRelativeTime(reply.createdAt)}</time>
														{#if authStore.user?.id === reply.author.id}
															<button class="comment-delete-btn" onclick={() => deleteReply(comment.id, reply.id)}>×</button>
														{/if}
													</div>
													<p class="reply-content">{reply.content}</p>
												</div>
											</li>
										{/each}
									</ul>
								{/if}

								{#if rs.composerOpen}
									<div class="reply-compose" transition:slide={{ duration: 150 }}>
										<div class="reply-compose-inner">
											<textarea class="reply-input" value={rs.draft} oninput={(e) => handleReplyDraftChange(comment.id, (e.target as HTMLTextAreaElement).value)} onkeydown={(e) => handleReplyKey(e, comment.id)} placeholder="Reply… (Esc to cancel)" rows="1" disabled={rs.submitting}></textarea>
											<button class="reply-submit-btn" onclick={() => submitReply(comment.id)} disabled={!rs.draft.trim() || rs.submitting}>{rs.submitting ? '…' : 'Reply'}</button>
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
						<textarea class="comment-input" bind:value={newComment} onkeydown={handleCommentKey} placeholder="Add a comment…" rows="1" disabled={isSubmitting}></textarea>
						<button class="comment-submit-btn" onclick={submitComment} disabled={!newComment.trim() || isSubmitting}>{isSubmitting ? '…' : 'Post'}</button>
					</div>
				</div>
			{/if}
		</div>
	{/if}
</article>

<style>
	.post-card {
		border-bottom: 1px solid var(--color-border);
	}

	/* ============= DESKTOP VARIANT ============= */
	.post-card:not(.mobile) {
		padding: 1.25rem 0.5rem;
		border-radius: 0.5rem;
		transition: background-color 0.15s ease;
	}

	.post-card:not(.mobile):hover {
		background-color: var(--color-surface-raised);
	}

	.post-card:not(.mobile):last-child {
		border-bottom: none;
	}

	.post-card:not(.mobile) .post-header {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		margin-bottom: 0.75rem;
	}

	.post-card:not(.mobile) .author-link {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		text-decoration: none;
		border-radius: 0.375rem;
		transition: opacity 0.15s ease;
	}

	.post-card:not(.mobile) .author-link:hover {
		opacity: 0.75;
	}

	.post-card:not(.mobile) .avatar {
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

	.post-card:not(.mobile) .avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.post-card:not(.mobile) .avatar-initial {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--color-text-muted);
	}

	.post-card:not(.mobile) .author-meta {
		display: flex;
		flex-direction: column;
		gap: 0.125rem;
	}

	.post-card:not(.mobile) .author-name {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text-primary);
	}

	.post-card:not(.mobile) .author-username {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
	}

	.post-card:not(.mobile) .post-body {
		padding-left: 3.25rem;
		margin-bottom: 0.875rem;
	}

	.post-card:not(.mobile) .post-photo-wrap {
		margin: 0.75rem 0 0.875rem 3.25rem;
		border-radius: 0.75rem;
		overflow: hidden;
		background: var(--color-surface-raised);
	}

	.post-card:not(.mobile) .photo-link {
		display: block;
	}

	.post-card:not(.mobile) .post-photo {
		width: 100%;
		max-height: 500px;
		object-fit: cover;
	}

	.post-card:not(.mobile) .post-footer {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		padding-left: 3.25rem;
	}

	/* Desktop reactions bar (hidden until hover) */
	.post-card:not(.mobile) .reactions-bar {
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

	.post-card:not(.mobile):hover .reactions-bar,
	.post-card:not(.mobile) .reactions-bar.reactions-active {
		max-width: 8rem;
		opacity: 1;
		transform: translateY(0);
		pointer-events: auto;
	}

	.post-card:not(.mobile) .post-meta {
		display: flex;
		align-items: center;
		gap: 0.625rem;
		margin-left: auto;
	}

	/* Options menu (desktop) */
	.options-menu-wrapper {
		position: relative;
		margin-left: auto;
	}

	.options-btn {
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-muted);
		padding: 0.375rem;
		border-radius: 0.25rem;
		display: flex;
		align-items: center;
		justify-content: center;
		transition: background-color 0.15s ease, color 0.15s ease;
	}

	.options-btn svg {
		width: 1.125rem;
		height: 1.125rem;
	}

	.options-btn:hover {
		background-color: var(--color-surface-raised);
		color: var(--color-text-primary);
	}

	.options-menu {
		position: absolute;
		top: 100%;
		right: 0;
		margin-top: 0.25rem;
		min-width: 140px;
		background: var(--color-bg);
		border: 1px solid var(--color-border);
		border-radius: 0.5rem;
		overflow: hidden;
		z-index: 100;
		box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
	}

	.options-menu-item {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		width: 100%;
		padding: 0.625rem 0.75rem;
		background: none;
		border: none;
		cursor: pointer;
		font-size: 0.875rem;
		color: var(--color-text-primary);
		text-align: left;
		transition: background-color 0.15s ease;
	}

	.options-menu-item svg {
		width: 1rem;
		height: 1rem;
		flex-shrink: 0;
	}

	.options-menu-item:hover {
		background-color: var(--color-surface-raised);
	}

	.options-menu-item:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	.options-menu-item.delete:hover {
		background-color: rgba(224, 82, 82, 0.15);
		color: var(--color-accent);
	}

	/* ============= MOBILE VARIANT ============= */
	.post-card.mobile {
		padding: 0;
	}

	.post-card.mobile .author-row {
		display: flex;
		align-items: center;
		gap: 0.625rem;
		margin-bottom: 0.25rem;
	}

	.post-card.mobile .author-tap {
		display: flex;
		align-items: center;
		gap: 0.625rem;
		flex: 1;
		min-width: 0;
		background: none;
		border: none;
		padding: 0;
		cursor: pointer;
		text-align: left;
	}

	.post-card.mobile .author-avatar {
		flex-shrink: 0;
	}

	.post-card.mobile .avatar-img {
		width: 2.25rem;
		height: 2.25rem;
		border-radius: 50%;
		object-fit: cover;
	}

	.post-card.mobile .avatar-initials {
		width: 2.25rem;
		height: 2.25rem;
		border-radius: 50%;
		background: var(--tg-accent, #e05252);
		color: #fff;
		font-size: 0.8125rem;
		font-weight: 600;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.post-card.mobile .author-info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.post-card.mobile .author-display {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--color-text-primary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.post-card.mobile .author-username {
		font-size: 0.75rem;
		color: var(--color-text-secondary);
	}

	.post-card.mobile .post-time {
		flex-shrink: 0;
		font-size: 0.75rem;
		color: var(--color-text-secondary);
	}

	/* Mobile options button */
	.post-card.mobile .options-btn {
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-secondary);
		padding: 0.25rem;
		border-radius: 0.25rem;
		display: flex;
		align-items: center;
		justify-content: center;
		transition: background-color 0.15s ease;
	}

	.post-card.mobile .options-btn svg {
		width: 1.25rem;
		height: 1.25rem;
	}

	.post-card.mobile .options-btn:active {
		background-color: var(--color-surface-raised);
	}

	/* Mobile options menu (bottom sheet style) */
	.post-card.mobile .options-menu {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		background: var(--color-bg);
		border-top: 1px solid var(--color-border);
		border-radius: 1rem 1rem 0 0;
		overflow: hidden;
		z-index: 200;
		padding-bottom: env(safe-area-inset-bottom, 0.5rem);
	}

	.post-card.mobile .options-menu-item {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 0.75rem;
		width: 100%;
		padding: 1rem;
		background: none;
		border: none;
		cursor: pointer;
		font-size: 1rem;
		color: var(--color-text-primary);
		transition: background-color 0.15s ease;
	}

	.post-card.mobile .options-menu-item svg {
		width: 1.25rem;
		height: 1.25rem;
	}

	.post-card.mobile .options-menu-item:active {
		background-color: var(--color-surface-raised);
	}

	.post-card.mobile .options-menu-item.delete {
		color: var(--color-accent);
	}

	.post-card.mobile .options-menu-item.delete:active {
		background-color: rgba(224, 82, 82, 0.15);
	}

	.post-card.mobile .post-body {
		padding: 0 1rem;
	}

	.post-card.mobile .post-content {
		font-size: 0.9375rem;
		line-height: 1.5;
		color: var(--color-text-primary);
		margin: 0.5rem 0 0;
		word-break: break-word;
	}

	.post-card.mobile .photo-strip {
		display: flex;
		overflow-x: auto;
		scroll-snap-type: x mandatory;
		scroll-behavior: smooth;
		-webkit-overflow-scrolling: touch;
		scrollbar-width: none;
		margin-top: 0.5rem;
	}

	.post-card.mobile .photo-strip::-webkit-scrollbar {
		display: none;
	}

	.post-card.mobile .photo-item {
		flex-shrink: 0;
		width: 100%;
		aspect-ratio: 4 / 3;
		scroll-snap-align: start;
	}

	.post-card.mobile .photo-item img {
		width: 100%;
		height: 100%;
		object-fit: cover;
		display: block;
	}

	.post-card.mobile .photo-dots {
		display: flex;
		justify-content: center;
		gap: 0.3125rem;
		padding: 0.5rem 0 0.25rem;
	}

	.post-card.mobile .photo-dot {
		height: 0.375rem;
		border-radius: 0.1875rem;
		background: rgba(255, 255, 255, 0.3);
		width: 0.375rem;
		transition: width 0.2s ease, background 0.2s ease;
	}

	.post-card.mobile .mobile-reactions {
		display: flex;
		align-items: center;
		gap: 0.25rem;
		padding: 0.25rem 0.625rem 0.5rem;
		border-top: 1px solid var(--color-border);
	}

	.post-card.mobile .action-btn {
		margin-left: auto;
	}

	/* ============= SHARED STYLES ============= */
	.post-content {
		font-size: 0.9375rem;
		line-height: 1.55;
		color: var(--color-text-primary);
		margin: 0;
		white-space: pre-wrap;
		word-break: break-word;
	}

	.reaction-btn {
		display: flex;
		align-items: center;
		gap: 0.3rem;
		background: none;
		border: none;
		cursor: pointer;
		padding: 0.25rem 0.375rem;
		border-radius: 0.375rem;
		transition: background-color 0.15s ease;
	}

	.reaction-btn:disabled {
		cursor: default;
	}

	.reaction-btn:not(:disabled):hover {
		background-color: var(--color-surface-raised);
	}

	.reaction-img {
		width: 2rem;
		height: 2rem;
		object-fit: contain;
	}

	.reaction-emoji {
		font-size: 1.25rem;
	}

	.reaction-count {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
	}

	.reaction-btn.active .reaction-count {
		color: var(--color-text-primary);
		font-weight: 600;
	}

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
		transition: color 0.15s ease;
	}

	.action-btn:hover {
		color: var(--color-text-primary);
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
	}

	.post-timestamp,
	.comment-time,
	.reply-time {
		font-size: 0.75rem;
		color: var(--color-text-muted);
	}

	/* Comments panel */
	.comments-panel {
		padding: 0.875rem 0 0;
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

	.loading-dot:nth-child(2) { animation-delay: 0.2s; }
	.loading-dot:nth-child(3) { animation-delay: 0.4s; }

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
		transition: opacity 0.15s ease;
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

	.comment-actions {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		margin-top: 0.25rem;
	}

	.reply-trigger,
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

	.reply-trigger:hover,
	.replies-toggle:hover {
		color: var(--color-text-primary);
	}

	.replies-loading {
		display: flex;
		gap: 0.2rem;
	}

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
	}

	.reply-content {
		font-size: 0.8125rem;
		line-height: 1.45;
		color: var(--color-text-primary);
		margin: 0;
		word-break: break-word;
	}

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
	}

	.reply-submit-btn:disabled {
		opacity: 0.35;
		cursor: not-allowed;
	}

	.no-comments {
		font-size: 0.875rem;
		color: var(--color-text-muted);
		margin: 0 0 0.875rem;
	}

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
	}

	.comment-submit-btn:disabled {
		opacity: 0.35;
		cursor: not-allowed;
	}

	@keyframes blink {
		0%, 100% { opacity: 0.3; }
		50% { opacity: 1; }
	}
</style>