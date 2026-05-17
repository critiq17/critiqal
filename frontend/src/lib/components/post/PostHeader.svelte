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

	let { post, showOptions, onauthorclick, onoptionsclick }: Props = $props();
</script>

<div class="p-head">
	<button class="author-btn" onclick={onauthorclick} aria-label="View {post.author.username}'s profile">
		<span class="avatar" aria-hidden="true">
			{#if post.author.avatarUrl}
				<img src={post.author.avatarUrl} alt={post.author.username} />
			{:else}
				<span class="avatar-initial">{getInitials(post.author.name, post.author.username)}</span>
			{/if}
		</span>
		<span class="p-who">
			<span class="p-name">{post.author.name ?? post.author.username}</span>
			<span class="p-meta-row">
				<span class="p-handle">@{post.author.username}</span>
				<time class="p-time" datetime={post.createdAt}>{formatRelativeTime(post.createdAt)}</time>
			</span>
		</span>
	</button>

	{#if showOptions}
		<button class="p-more" onclick={onoptionsclick} aria-label="Post options" type="button">
			<svg viewBox="0 0 24 24" fill="currentColor" width="16" height="16" aria-hidden="true">
				<circle cx="6" cy="12" r="1.6" />
				<circle cx="12" cy="12" r="1.6" />
				<circle cx="18" cy="12" r="1.6" />
			</svg>
		</button>
	{/if}
</div>

<style>
	.p-head {
		display: flex;
		align-items: center;
		gap: 10px;
		margin-bottom: 8px;
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
		transition: opacity 0.15s ease;
	}

	.author-btn:hover {
		opacity: 0.78;
	}

	.avatar {
		width: 34px;
		height: 34px;
		border-radius: 999px;
		overflow: hidden;
		flex: none;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow:
			0 0 0 0.5px rgba(255, 255, 255, 0.1),
			inset 0 0 0 0.5px rgba(0, 0, 0, 0.3);
	}

	.avatar img {
		width: 100%;
		height: 100%;
		object-fit: cover;
		display: block;
	}

	.avatar-initial {
		font-size: 0.8125rem;
		font-weight: 600;
		color: var(--color-text-muted);
		user-select: none;
	}

	.p-who {
		display: flex;
		flex-direction: column;
		min-width: 0;
		line-height: 1.15;
	}

	.p-name {
		font-size: 14px;
		font-weight: 600;
		color: var(--color-text-primary);
		letter-spacing: -0.005em;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.p-meta-row {
		display: flex;
		align-items: baseline;
		gap: 5px;
		color: var(--color-text-muted);
		font-size: 12.5px;
	}

	.p-handle {
		color: var(--color-text-muted);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.p-time {
		font-size: 11.5px;
		color: var(--color-text-muted);
		opacity: 0.7;
		white-space: nowrap;
		flex: none;
	}

	.p-time::before {
		content: '·';
		margin-right: 5px;
		opacity: 0.7;
	}

	.p-more {
		margin-left: auto;
		width: 30px;
		height: 30px;
		border-radius: 999px;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-muted);
		flex: none;
		transition:
			background 160ms ease,
			color 160ms ease,
			transform 380ms cubic-bezier(0.34, 1.56, 0.64, 1);
	}

	.p-more:hover {
		background: rgba(255, 255, 255, 0.05);
		color: var(--color-text-primary);
	}

	.p-more:active {
		transform: scale(0.86);
		transition: transform 100ms ease;
	}

	@media (prefers-reduced-motion: reduce) {
		.p-more {
			transition: none;
		}
	}
</style>
