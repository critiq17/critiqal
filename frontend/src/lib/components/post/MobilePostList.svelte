<script lang="ts">
	import type { Snippet } from 'svelte';
	import type { Post } from '$lib/types';
	import PostComponent from './Post.svelte';

	interface Props {
		posts: Post[];
		loading?: boolean;
		error?: string | null;
		onOpenComments: (postId: string) => void;
		onAuthorClick?: (username: string) => void;
		onDeleted?: (id: string) => void;
		onRetry?: () => void;
		empty?: Snippet;
	}

	let {
		posts,
		loading = false,
		error = null,
		onOpenComments,
		onAuthorClick,
		onDeleted,
		onRetry,
		empty
	}: Props = $props();
</script>

{#if error && posts.length === 0}
	<div class="state-box" role="alert">
		<p class="state-text state-text--err">{error}</p>
		{#if onRetry}
			<button class="retry-btn" type="button" onclick={onRetry}>Try again</button>
		{/if}
	</div>
{:else if loading && posts.length === 0}
	<div class="skeleton-list" aria-busy="true" aria-label="Loading posts">
		{#each [96, 72, 110] as h (h)}
			<div class="skeleton-post" style="--h:{h}px"></div>
		{/each}
	</div>
{:else if posts.length === 0}
	{#if empty}
		{@render empty()}
	{:else}
		<div class="state-box">
			<p class="state-text">No posts yet</p>
		</div>
	{/if}
{:else}
	<div class="post-list">
		{#each posts as post (post.id)}
			<PostComponent
				{post}
				variant="mobile"
				{onAuthorClick}
				{onDeleted}
				{onOpenComments}
			/>
		{/each}
	</div>
{/if}

<style>
	.post-list {
		display: flex;
		flex-direction: column;
	}

	.state-box {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 10px;
		padding: 48px 16px;
	}

	.state-text {
		margin: 0;
		font-size: 14px;
		color: rgba(255, 255, 255, 0.35);
		text-align: center;
	}

	.state-text--err {
		color: #e05252;
	}

	.retry-btn {
		padding: 8px 20px;
		border-radius: 10px;
		border: 1px solid rgba(255, 255, 255, 0.1);
		background: none;
		color: rgba(255, 255, 255, 0.7);
		font-size: 14px;
		font-weight: 500;
		font-family: inherit;
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
	}

	.retry-btn:active {
		opacity: 0.7;
	}

	.skeleton-list {
		display: flex;
		flex-direction: column;
		gap: 1px;
	}

	.skeleton-post {
		height: var(--h, 88px);
		background: rgba(255, 255, 255, 0.04);
		animation: pulse 1.4s ease-in-out infinite;
	}

	@keyframes pulse {
		0%,
		100% {
			opacity: 1;
		}
		50% {
			opacity: 0.4;
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.skeleton-post {
			animation: none;
		}
	}
</style>
