<script lang="ts">
	import type { Post } from '$lib/types';
	import { formatRelativeTime } from '$lib/utils/formatRelativeTime';

	interface Props {
		posts: Post[];
		postsLoading?: boolean;
	}

	let { posts, postsLoading = false }: Props = $props();
</script>

{#if postsLoading && posts.length === 0}
	<div class="posts-skeleton" aria-busy="true">
		<div class="skeleton-post"></div>
		<div class="skeleton-post"></div>
	</div>
{:else if posts.length === 0}
	<div class="empty-posts">
		<p>No posts yet</p>
	</div>
{:else}
	<div class="posts-list">
		{#each posts as post (post.id)}
			<article class="post-card">
				<div class="post-card-inner">
					<div class="post-meta">
						<span class="post-time">{formatRelativeTime(post.createdAt)}</span>
					</div>
					<p class="post-content">{post.content}</p>
					{#if post.photos && post.photos.length > 0}
						<div class="post-photos">
							{#each [...post.photos].sort((a, b) => a.position - b.position).slice(0, 1) as photo (photo.id)}
								<img src={photo.url} alt="Post photo" class="post-photo" loading="lazy" />
							{/each}
							{#if post.photos.length > 1}
								<span class="photo-count">+{post.photos.length - 1}</span>
							{/if}
						</div>
					{/if}
				</div>
			</article>
		{/each}
	</div>
{/if}

<style>
	.posts-list {
		display: flex;
		flex-direction: column;
	}

	.post-card {
		border-bottom: 1px solid rgba(255, 255, 255, 0.05);
	}

	.post-card-inner {
		padding: 14px 16px;
	}

	.post-meta {
		margin-bottom: 6px;
	}

	.post-time {
		font-size: 12px;
		color: var(--tg-hint, rgba(240, 240, 240, 0.5));
	}

	.post-content {
		font-size: 15px;
		color: var(--tg-text, #f0f0f0);
		line-height: 1.5;
		margin: 0;
		white-space: pre-wrap;
		word-break: break-word;
	}

	.post-photos {
		margin-top: 10px;
		position: relative;
		border-radius: 12px;
		overflow: hidden;
	}

	.post-photo {
		width: 100%;
		max-height: 240px;
		object-fit: cover;
		display: block;
	}

	.photo-count {
		position: absolute;
		bottom: 8px;
		right: 8px;
		background: rgba(0, 0, 0, 0.6);
		color: #fff;
		font-size: 12px;
		font-weight: 600;
		padding: 3px 8px;
		border-radius: 10px;
	}

	.posts-skeleton {
		display: flex;
		flex-direction: column;
		gap: 1px;
	}

	.skeleton-post {
		height: 72px;
		background: var(--color-surface-raised, #1e1e1e);
		animation: pulse 1.4s ease-in-out infinite;
	}

	@keyframes pulse {
		0%, 100% { opacity: 1; }
		50% { opacity: 0.4; }
	}

	.empty-posts {
		padding: 40px 16px;
		text-align: center;
		color: var(--tg-hint, rgba(240, 240, 240, 0.5));
		font-size: 14px;
	}
</style>
