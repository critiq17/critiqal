<script lang="ts">
	import { onMount } from 'svelte';
	import type { Comment } from '$lib/types';
	import { postService } from '$lib/services';
	import { getTelegramWebApp } from '$lib/telegram';

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

	// Drag-to-dismiss state
	let dragY = $state(0);
	let isDragging = $state(false);
	let dragStartY = 0;

	let sheetEl = $state<HTMLElement | null>(null);
	let inputEl = $state<HTMLInputElement | null>(null);

	function formatRelativeTime(dateStr: string): string {
		const diff = Date.now() - new Date(dateStr).getTime();
		const minutes = Math.floor(diff / 60000);
		if (minutes < 1) return 'just now';
		if (minutes < 60) return `${minutes}m`;
		const hours = Math.floor(minutes / 60);
		if (hours < 24) return `${hours}h`;
		const days = Math.floor(hours / 24);
		if (days < 7) return `${days}d`;
		return new Date(dateStr).toLocaleDateString();
	}

	function getInitials(name: string | null, username: string): string {
		const src = name ?? username;
		return src.slice(0, 2).toUpperCase();
	}

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

	// Drag-to-dismiss handlers
	function onTouchStart(e: TouchEvent): void {
		dragStartY = e.touches[0].clientY;
		isDragging = true;
		if (sheetEl) sheetEl.classList.add('dragging');
	}

	function onTouchMove(e: TouchEvent): void {
		if (!isDragging) return;
		const delta = e.touches[0].clientY - dragStartY;
		dragY = Math.max(0, delta);
		if (sheetEl) {
			sheetEl.style.transform = `translateY(${dragY}px)`;
		}
	}

	function onTouchEnd(): void {
		isDragging = false;
		if (sheetEl) sheetEl.classList.remove('dragging');
		if (dragY > 120) {
			dragY = 0;
			if (sheetEl) sheetEl.style.transform = '';
			onClose();
		} else {
			dragY = 0;
			if (sheetEl) sheetEl.style.transform = '';
		}
	}

	// Telegram BackButton + load on open
	$effect(() => {
		const tg = getTelegramWebApp();
		if (open) {
			loadComments();
			if (tg) {
				tg.BackButton.show();
				tg.BackButton.onClick(onClose);
			}
		} else {
			if (tg) {
				tg.BackButton.hide();
				tg.BackButton.offClick(onClose);
			}
		}
	});
</script>

<!-- Backdrop -->
<div
	class="backdrop"
	class:open
	role="presentation"
	onclick={onClose}
></div>

<!-- Sheet -->
<div
	class="sheet"
	class:open
	bind:this={sheetEl}
	role="dialog"
	aria-modal="true"
	aria-label="Comments"
>
	<!-- Drag handle -->
	<div
		class="sheet-handle-area"
		role="presentation"
		ontouchstart={onTouchStart}
		ontouchmove={onTouchMove}
		ontouchend={onTouchEnd}
	>
		<div class="drag-handle"></div>
		<h2 class="sheet-title">Comments</h2>
	</div>

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
					<div class="comment-avatar">
						{#if comment.author.avatarUrl}
							<img src={comment.author.avatarUrl} alt={comment.author.username} class="avatar-img" />
						{:else}
							<div class="avatar-initials">
								{getInitials(comment.author.name, comment.author.username)}
							</div>
						{/if}
					</div>
					<div class="comment-body">
						<div class="comment-meta">
							<span class="comment-username">@{comment.author.username}</span>
							<span class="comment-time">{formatRelativeTime(comment.createdAt)}</span>
						</div>
						<p class="comment-content">{comment.content}</p>
					</div>
				</div>
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
</div>

<style>
	.backdrop {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.5);
		backdrop-filter: blur(4px);
		z-index: 149;
		opacity: 0;
		pointer-events: none;
		transition: opacity 350ms ease;
	}

	.backdrop.open {
		opacity: 1;
		pointer-events: auto;
	}

	.sheet {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		height: 70dvh;
		border-radius: 24px 24px 0 0;
		background: var(--color-surface, #1a1a1a);
		z-index: 150;
		transform: translateY(100%);
		transition: transform 350ms cubic-bezier(0.32, 0.72, 0, 1);
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.sheet.open {
		transform: translateY(0);
	}

	/* Remove transition during drag so it follows finger exactly */
	:global(.sheet.dragging) {
		transition: none !important;
	}

	.sheet-handle-area {
		flex-shrink: 0;
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 12px 16px 8px;
		cursor: grab;
		user-select: none;
		-webkit-user-select: none;
	}

	.drag-handle {
		width: 36px;
		height: 4px;
		border-radius: 2px;
		background: var(--color-border, rgba(255, 255, 255, 0.2));
		margin-bottom: 12px;
	}

	.sheet-title {
		font-size: 16px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
		margin: 0;
		align-self: flex-start;
	}

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

	.comment-avatar {
		flex-shrink: 0;
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

	.comment-username {
		font-size: 13px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
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
</style>
