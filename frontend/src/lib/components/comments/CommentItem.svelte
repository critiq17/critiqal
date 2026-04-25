<script lang="ts">
	import { slide } from 'svelte/transition';
	import { getInitials } from '$lib/utils/getInitials';
	import { formatRelativeTime } from '$lib/utils/formatRelativeTime';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import type { UseComments } from '$lib/features/posts/useComments.svelte';
	import type { Comment } from '$lib/types';

	interface Props {
		comment: Comment;
		comments: UseComments;
	}

	let { comment, comments }: Props = $props();

	const rs = $derived(comments.getReplyState(comment.id));

	function handleReplyKey(e: KeyboardEvent): void {
		if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
			comments.submitReply(comment.id);
		}
		if (e.key === 'Escape') {
			comments.closeReplyComposer(comment.id);
		}
	}
</script>

<li class="comment-item" class:deleting={comments.deletingId === comment.id}>
	<div class="comment-avatar" aria-hidden="true">
		{#if comment.author.avatarUrl}
			<img
				src={comment.author.avatarUrl}
				alt={comment.author.username}
				class="comment-avatar-img"
			/>
		{:else}
			<span class="comment-avatar-initial">
				{getInitials(comment.author.name, comment.author.username)}
			</span>
		{/if}
	</div>
	<div class="comment-body">
		<div class="comment-header">
			<a href="/{comment.author.username}" class="comment-author">
				{comment.author.name ?? comment.author.username}
			</a>
			<time class="comment-time" datetime={comment.createdAt}>
				{formatRelativeTime(comment.createdAt)}
			</time>
			{#if authStore.user?.id === comment.author.id}
				<button
					class="comment-delete-btn"
					onclick={() => comments.deleteComment(comment.id)}
					disabled={comments.deletingId === comment.id}
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

		<div class="comment-actions">
			{#if authStore.isAuthenticated && !rs.composerOpen}
				<button
					class="reply-trigger"
					onclick={() => comments.openReplyComposer(comment.id)}
					aria-label="Reply to comment"
				>
					reply
				</button>
			{/if}
			{#if rs.loading}
				<span class="replies-loading" aria-label="Loading replies">
					<span class="loading-dot"></span>
					<span class="loading-dot"></span>
					<span class="loading-dot"></span>
				</span>
			{:else if !rs.loaded}
				<button
					class="replies-toggle"
					onclick={() => comments.toggleReplies(comment.id)}
					aria-label="Load replies"
				>
					Replies ›
				</button>
			{:else if rs.loaded && rs.replies.length >= 2 && !rs.expanded}
				<button
					class="replies-toggle"
					onclick={() => comments.toggleReplies(comment.id)}
					aria-expanded="false"
				>
					&#9658; {rs.replies.length} replies
				</button>
			{:else if rs.loaded && rs.expanded && rs.replies.length >= 2}
				<button
					class="replies-toggle"
					onclick={() => comments.toggleReplies(comment.id)}
					aria-expanded="true"
				>
					&#9660; hide replies
				</button>
			{/if}
		</div>

		{#if rs.loaded && rs.replies.length > 0 && (rs.replies.length === 1 || rs.expanded)}
			<ul class="replies-list" aria-label="Replies" transition:slide={{ duration: 180 }}>
				{#each rs.replies as reply (reply.id)}
					<li class="reply-item">
						<div class="reply-avatar" aria-hidden="true">
							{#if reply.author.avatarUrl}
								<img
									src={reply.author.avatarUrl}
									alt={reply.author.username}
									class="reply-avatar-img"
								/>
							{:else}
								<span class="reply-avatar-initial">
									{getInitials(reply.author.name, reply.author.username)}
								</span>
							{/if}
						</div>
						<div class="reply-body">
							<div class="reply-header">
								<a href="/{reply.author.username}" class="reply-author">
									{reply.author.name ?? reply.author.username}
								</a>
								<time class="reply-time" datetime={reply.createdAt}>
									{formatRelativeTime(reply.createdAt)}
								</time>
								{#if authStore.user?.id === reply.author.id}
									<button
										class="comment-delete-btn"
										onclick={() => comments.deleteReply(comment.id, reply.id)}
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

		{#if rs.composerOpen}
			<div class="reply-compose" transition:slide={{ duration: 150 }}>
				<div class="reply-compose-inner">
					<textarea
						class="reply-input"
						value={rs.draft}
						oninput={(e) =>
							comments.setReplyDraft(comment.id, (e.target as HTMLTextAreaElement).value)}
						onkeydown={handleReplyKey}
						placeholder="Reply… (Esc to cancel)"
						rows={1}
						disabled={rs.submitting}
						aria-label="Write a reply"
					></textarea>
					<button
						class="reply-submit-btn"
						onclick={() => comments.submitReply(comment.id)}
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

<style>
	.comment-item {
		display: flex;
		gap: 0.625rem;
		transition: opacity 0.15s ease;
	}

	.comment-item.deleting {
		opacity: 0.5;
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
		align-items: center;
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

	@keyframes blink {
		0%,
		100% {
			opacity: 0.3;
		}
		50% {
			opacity: 1;
		}
	}

	.replies-list {
		list-style: none;
		padding: 0 0 0 0.75rem;
		margin: 0.5rem 0 0;
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
		border-left: 2px solid var(--color-border);
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
</style>
