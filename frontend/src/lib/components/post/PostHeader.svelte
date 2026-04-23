<script lang="ts">
	import { getInitials } from '$lib/utils/getInitials';
	import { formatRelativeTime } from '$lib/utils/formatRelativeTime';
	import type { Post } from '$lib/types';

	interface Props {
		post: Post;
		showOptions: boolean;
		isOwnPost: boolean;
		onauthorclick: () => void;
		onoptionsclick: () => void;
	}

	let { post, showOptions, isOwnPost, onauthorclick, onoptionsclick }: Props = $props();
</script>

<div class="post-header">
	<button class="author-btn" onclick={onauthorclick} aria-label="View {post.author.username}'s profile">
		<div class="avatar" aria-hidden="true">
			{#if post.author.avatarUrl}
				<img src={post.author.avatarUrl} alt={post.author.username} class="avatar-img" />
			{:else}
				<span class="avatar-initial">{getInitials(post.author.name, post.author.username)}</span>
			{/if}
		</div>
		<div class="author-info">
			<span class="author-name">{post.author.name ?? post.author.username}</span>
			<span class="author-username">@{post.author.username}</span>
		</div>
	</button>
	<div class="header-right">
		<time class="post-time" datetime={post.createdAt}>{formatRelativeTime(post.createdAt)}</time>
		{#if showOptions}
			<button class="options-btn" onclick={onoptionsclick} aria-label="Post options">
				<svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
					<circle cx="12" cy="6" r="1.5" />
					<circle cx="12" cy="12" r="1.5" />
					<circle cx="12" cy="18" r="1.5" />
				</svg>
			</button>
		{/if}
	</div>
</div>

<style>
	.post-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 12px 16px 8px;
		gap: 8px;
	}

	.author-btn {
		display: flex;
		align-items: center;
		gap: 10px;
		background: none;
		border: none;
		cursor: pointer;
		text-align: left;
		flex: 1;
		min-width: 0;
		padding: 0;
		border-radius: var(--radius-sm, 4px);
		transition: opacity 0.15s ease;
	}

	.author-btn:hover {
		opacity: 0.75;
	}

	.avatar {
		width: 36px;
		height: 36px;
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

	.author-info {
		display: flex;
		flex-direction: column;
		min-width: 0;
	}

	.author-name {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--color-text-primary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.author-username {
		font-size: 0.75rem;
		color: var(--color-text-muted);
	}

	.header-right {
		display: flex;
		align-items: center;
		gap: 6px;
		flex-shrink: 0;
	}

	.post-time {
		font-size: 0.75rem;
		color: var(--color-text-muted);
		white-space: nowrap;
	}

	.options-btn {
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-muted);
		padding: 4px;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		transition: color 0.15s ease, background-color 0.15s ease;
	}

	.options-btn:hover {
		color: var(--color-text-primary);
		background: var(--color-surface-raised);
	}
</style>
