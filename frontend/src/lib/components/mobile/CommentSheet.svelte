<script lang="ts">
	import { onMount } from 'svelte';
	import type { Comment } from '$lib/types';
	import { postService } from '$lib/services';
	import { getTelegramWebApp } from '$lib/telegram';
	import { openProfile } from '$lib/stores/profile-nav.store.svelte';
	import { openSheet } from '$lib/stores/sheet.store.svelte';
	import { Sheet } from '$lib/ui';
	import { formatRelativeTime } from '$lib/utils/formatRelativeTime';
	import { getInitials } from '$lib/utils/getInitials';

	interface Props {
		postId: number;
		open: boolean;
		onClose: () => void;
	}

	let { postId, open, onClose }: Props = $props();

	let comments = $state<Comment[]>([]);
	let isLoading = $state(false);
	let error = $state<string | null>(null);
	let commentText = $state('');
	let isSubmitting = $state(false);

	// Reply state per comment
	let replyDrafts = $state(new Map<number, string>());
	let replyOpen = $state(new Map<number, boolean>());
	let replyLoading = $state(new Map<number, boolean>());
	let commentReplies = $state(new Map<number, Comment[]>());

	let inputEl = $state<HTMLInputElement | null>(null);

	async function loadComments(): Promise<void> {
		if (!postId) return;
		isLoading = true;
		error = null;
		try {
			comments = await postService.getComments(postId);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load comments';
		} finally {
			isLoading = false;
		}
	}

	async function handleSubmit(): Promise<void> {
		const text = commentText.trim();
		if (!text || isSubmitting) return;
		isSubmitting = true;
		try {
			const newComment = await postService.addComment(postId, { content: text });
			comments = [...comments, newComment];
			commentText = '';
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to post comment';
		} finally {
			isSubmitting = false;
		}
	}

	function handleKeydown(e: KeyboardEvent): void {
		if (e.key === 'Enter' && !e.shiftKey) {
			e.preventDefault();
			handleSubmit();
		}
	}

	function toggleReplyComposer(commentId: number): void {
		const isOpen = replyOpen.get(commentId) ?? false;
		replyOpen = new Map(replyOpen).set(commentId, !isOpen);
	}

	function toggleReplies(commentId: number): void {
		const isOpen = replyOpen.get(commentId) ?? false;
		if (!isOpen && !commentReplies.has(commentId)) {
			const existing = comments.find((c) => c.id === commentId)?.replies ?? [];
			commentReplies = new Map(commentReplies).set(commentId, existing);
		}
		replyOpen = new Map(replyOpen).set(commentId, !isOpen);
	}

	async function submitReply(commentId: number): Promise<void> {
		const text = replyDrafts.get(commentId)?.trim();
		if (!text) return;
		replyLoading = new Map(replyLoading).set(commentId, true);
		try {
			const reply = await postService.addReply(postId, commentId, { content: text });
			const existing = commentReplies.get(commentId) ?? [];
			commentReplies = new Map(commentReplies).set(commentId, [...existing, reply]);
			replyDrafts = new Map(replyDrafts).set(commentId, '');
		} catch {
			// Non-critical: ignore reply submission errors silently
		} finally {
			replyLoading = new Map(replyLoading).set(commentId, false);
		}
	}

	// Telegram BackButton + load on open + hide bottom nav
	let _closeSheet: (() => void) | null = null;
	$effect(() => {
		const tg = getTelegramWebApp();
		if (open) {
			loadComments();
			_closeSheet = openSheet();
			if (tg) {
				tg.BackButton.show();
				tg.BackButton.onClick(onClose);
			}
		} else {
			_closeSheet?.();
			_closeSheet = null;
			if (tg) {
				tg.BackButton.hide();
				tg.BackButton.offClick(onClose);
			}
		}
	});
</script>

<Sheet {open} onclose={onClose} title="Comments" maxHeight="70vh">
	<!-- Comments list -->
	<div class="comments-list">
		{#if isLoading}
			<div class="state-message">Loading comments…</div>
		{:else if error}
			<div class="state-message error">{error}</div>
		{:else if comments.length === 0}
			<div class="state-message muted">No comments yet. Be the first!</div>
		{:else}
			{#each comments as comment (comment.id)}
				<div class="comment-item">
					<button
						class="comment-avatar-btn"
						onclick={() => openProfile(comment.author.username)}
						aria-label="View {comment.author.username}'s profile"
					>
						{#if comment.author.avatarUrl}
							<img src={comment.author.avatarUrl} alt={comment.author.username} class="avatar-img" />
						{:else}
							<div class="avatar-initials">
								{getInitials(comment.author.name, comment.author.username)}
							</div>
						{/if}
					</button>
					<div class="comment-body">
						<div class="comment-meta">
							<button
								class="comment-username-btn"
								onclick={() => openProfile(comment.author.username)}
								aria-label="View {comment.author.username}'s profile"
							>@{comment.author.username}</button>
							<span class="comment-time">{formatRelativeTime(comment.createdAt)}</span>
						</div>
						<p class="comment-content">{comment.content}</p>
					</div>
				</div>

				<!-- Reply row -->
				<div class="comment-actions">
					<button class="reply-btn" onclick={() => toggleReplyComposer(comment.id)}>
						Reply
					</button>
					{#if comment.replies && comment.replies.length > 0}
						<button class="show-replies-btn" onclick={() => toggleReplies(comment.id)}>
							{replyOpen.get(comment.id) ? 'Hide' : `${comment.replies.length} repl${comment.replies.length === 1 ? 'y' : 'ies'}`}
						</button>
					{/if}
				</div>

				{#if replyOpen.get(comment.id)}
					<!-- Reply composer -->
					<div class="reply-composer">
						<input
							class="reply-input"
							placeholder="Reply to @{comment.author.username}…"
							value={replyDrafts.get(comment.id) ?? ''}
							oninput={(e) => { replyDrafts = new Map(replyDrafts).set(comment.id, (e.target as HTMLInputElement).value); }}
						/>
						<button
							class="reply-send-btn"
							disabled={!replyDrafts.get(comment.id)?.trim() || (replyLoading.get(comment.id) ?? false)}
							onclick={() => submitReply(comment.id)}
						>Send</button>
					</div>

					<!-- Nested replies -->
					{#each commentReplies.get(comment.id) ?? [] as reply (reply.id)}
						<div class="reply-item">
							<button
								class="reply-avatar-btn"
								onclick={() => openProfile(reply.author.username)}
								aria-label="View {reply.author.username}'s profile"
							>{reply.author.username.charAt(0).toUpperCase()}</button>
							<div class="reply-content">
								<button
									class="reply-author-btn"
									onclick={() => openProfile(reply.author.username)}
									aria-label="View {reply.author.username}'s profile"
								>@{reply.author.username}</button>
								<span class="reply-text">{reply.content}</span>
							</div>
						</div>
					{/each}
				{/if}
			{/each}
		{/if}
	</div>

	<!-- Input row -->
	<div class="comment-input-row">
		<input
			bind:this={inputEl}
			bind:value={commentText}
			class="comment-input"
			type="text"
			placeholder="Add a comment…"
			maxlength={500}
			onkeydown={handleKeydown}
			disabled={isSubmitting}
		/>
		<button
			class="send-btn"
			onclick={handleSubmit}
			disabled={!commentText.trim() || isSubmitting}
			aria-label="Send comment"
		>
			<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
				<line x1="22" y1="2" x2="11" y2="13"></line>
				<polygon points="22 2 15 22 11 13 2 9 22 2"></polygon>
			</svg>
		</button>
	</div>
</Sheet>

<style>
	.comments-list {
		flex: 1;
		overflow-y: auto;
		-webkit-overflow-scrolling: touch;
		padding: 4px 0;
	}

	.state-message {
		padding: 32px 16px;
		text-align: center;
		color: var(--color-text-primary, #f0f0f0);
		font-size: 14px;
	}

	.state-message.muted {
		color: var(--color-text-secondary, rgba(240, 240, 240, 0.5));
	}

	.state-message.error {
		color: #e05252;
	}

	.comment-item {
		display: flex;
		gap: 12px;
		padding: 10px 16px;
	}

	.comment-avatar-btn {
		flex-shrink: 0;
		background: none;
		border: none;
		padding: 0;
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
	}

	.comment-username-btn {
		background: none;
		border: none;
		padding: 0;
		cursor: pointer;
		font-size: 13px;
		font-weight: 600;
		color: var(--tg-text, #f0f0f0);
		-webkit-tap-highlight-color: transparent;
	}

	.avatar-img {
		width: 32px;
		height: 32px;
		border-radius: 50%;
		object-fit: cover;
		display: block;
	}

	.avatar-initials {
		width: 32px;
		height: 32px;
		border-radius: 50%;
		background: var(--tg-accent, #e05252);
		color: #fff;
		font-size: 12px;
		font-weight: 600;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.comment-body {
		flex: 1;
		min-width: 0;
	}

	.comment-meta {
		display: flex;
		align-items: baseline;
		gap: 8px;
		margin-bottom: 3px;
	}

	.comment-time {
		font-size: 11px;
		color: var(--color-text-secondary, rgba(240, 240, 240, 0.5));
	}

	.comment-content {
		font-size: 14px;
		line-height: 1.45;
		color: var(--color-text-primary, #f0f0f0);
		margin: 0;
		word-break: break-word;
	}

	.comment-input-row {
		position: sticky;
		bottom: 0;
		background: var(--color-surface, #1a1a1a);
		padding: 12px 16px calc(12px + env(safe-area-inset-bottom, 0px));
		display: flex;
		gap: 8px;
		border-top: 1px solid var(--color-border, rgba(255, 255, 255, 0.08));
	}

	.comment-input {
		flex: 1;
		height: 40px;
		border-radius: 20px;
		background: var(--color-surface-raised, #242424);
		border: none;
		padding: 0 16px;
		color: var(--color-text-primary, #f0f0f0);
		font-size: 14px;
		outline: none;
	}

	.comment-input::placeholder {
		color: var(--color-text-secondary, rgba(240, 240, 240, 0.4));
	}

	.comment-input:disabled {
		opacity: 0.6;
	}

	.send-btn {
		flex-shrink: 0;
		width: 40px;
		height: 40px;
		border-radius: 50%;
		background: var(--tg-accent, #e05252);
		border: none;
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		color: #fff;
		transition: opacity 0.15s ease;
	}

	.send-btn:disabled {
		opacity: 0.4;
		cursor: not-allowed;
	}

	.send-btn:not(:disabled):active {
		opacity: 0.8;
	}

	/* Reply elements */
	.comment-actions {
		display: flex;
		gap: 12px;
		padding: 2px 0 6px 44px;
	}

	.reply-btn,
	.show-replies-btn {
		background: none;
		border: none;
		font-size: 12px;
		color: rgba(240, 240, 240, 0.4);
		cursor: pointer;
		padding: 0;
	}

	.reply-btn:active,
	.show-replies-btn:active {
		color: rgba(240, 240, 240, 0.7);
	}

	.reply-composer {
		display: flex;
		gap: 8px;
		padding: 6px 16px 6px 44px;
		align-items: center;
	}

	.reply-input {
		flex: 1;
		height: 34px;
		border-radius: 17px;
		background: var(--color-surface-raised, #242424);
		border: none;
		padding: 0 12px;
		font-size: 13px;
		color: var(--color-text-primary, #f0f0f0);
		outline: none;
	}

	.reply-input::placeholder {
		color: var(--color-text-secondary, rgba(240, 240, 240, 0.4));
	}

	.reply-send-btn {
		background: var(--tg-accent, #e05252);
		color: white;
		border: none;
		border-radius: 14px;
		padding: 6px 12px;
		font-size: 12px;
		font-weight: 600;
		cursor: pointer;
	}

	.reply-send-btn:disabled {
		opacity: 0.4;
		cursor: not-allowed;
	}

	.reply-item {
		display: flex;
		gap: 8px;
		padding: 6px 16px 0 44px;
		align-items: flex-start;
	}

	.reply-content {
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	.reply-avatar-btn {
		width: 24px;
		height: 24px;
		border-radius: 50%;
		background: var(--color-surface-raised, #242424);
		color: rgba(240, 240, 240, 0.6);
		font-size: 11px;
		font-weight: 600;
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		border: none;
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
	}

	.reply-author-btn {
		background: none;
		border: none;
		padding: 0;
		cursor: pointer;
		font-size: 12px;
		font-weight: 600;
		color: rgba(240, 240, 240, 0.6);
		-webkit-tap-highlight-color: transparent;
	}

	.reply-text {
		font-size: 13px;
		color: rgba(240, 240, 240, 0.8);
		line-height: 1.4;
	}
</style>
