<script lang="ts">
	import type { UseSearch } from '$lib/features/explore/useSearch.svelte';
	import type { Post } from '$lib/types';
	import { getInitials } from '$lib/utils/getInitials';
	import { formatRelativeTime } from '$lib/utils/formatRelativeTime';
	import ExploreEmptyState from './ExploreEmptyState.svelte';

	const SKELETON_COUNT = 3;

	interface Props {
		search: UseSearch;
		query: string;
		onRetry: () => void;
		onPostTap: (post: Post) => void;
	}

	let { search, query, onRetry, onPostTap }: Props = $props();

	function truncateContent(content: string, maxLen = 120): string {
		return content.length <= maxLen ? content : `${content.slice(0, maxLen).trimEnd()}…`;
	}

	function infiniteScroll(el: HTMLElement): { destroy: () => void } {
		const obs = new IntersectionObserver(
			([entry]) => {
				if (entry?.isIntersecting && !search.postsLoadingMore) search.loadMorePosts();
			},
			{ threshold: 0.1 }
		);
		obs.observe(el);
		return { destroy: () => obs.disconnect() };
	}
</script>

{#if search.postsLoading}
	<div class="skeleton-list" aria-busy="true" aria-label="Loading posts">
		{#each { length: SKELETON_COUNT } as _, i (i)}
			<div class="post-skeleton" style:animation-delay="{i * 60}ms">
				<div class="post-skeleton-header">
					<div class="skeleton-avatar"></div>
					<div class="skeleton-meta">
						<div class="skeleton-line skeleton-name"></div>
						<div class="skeleton-line skeleton-time"></div>
					</div>
				</div>
				<div class="skeleton-line skeleton-body"></div>
				<div class="skeleton-line skeleton-body-short"></div>
			</div>
		{/each}
	</div>
{:else if search.postsError}
	<ExploreEmptyState variant="error" message={search.postsError} onRetry={onRetry} />
{:else if search.posts.length === 0}
	<ExploreEmptyState variant="empty" message="Try a different search" />
{:else}
	<ul class="results-list" aria-label="Post results">
		{#each search.posts as post, i (post.id)}
			<li
				class="post-item"
				style:animation-delay="{i * 30}ms"
				onclick={() => onPostTap(post)}
				onkeydown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); onPostTap(post); } }}
				role="button"
				tabindex="0"
				aria-label="Post by {post.author.name ?? post.author.username}"
			>
				<div class="post-meta">
					<div class="post-avatar" aria-hidden="true">
						{#if post.author.avatarUrl}
							<img src={post.author.avatarUrl} alt={post.author.username} class="avatar-img" />
						{:else}
							<span class="avatar-initial">{getInitials(post.author.name, post.author.username)}</span>
						{/if}
					</div>
					<div class="post-author-info">
						<span class="author-name">{post.author.name ?? post.author.username}</span>
						<span class="post-time">{formatRelativeTime(post.createdAt)}</span>
					</div>
				</div>
				<p class="post-excerpt">{truncateContent(post.content)}</p>
			</li>
		{/each}
	</ul>

	{#if search.postsHasNext}
		<div class="sentinel" use:infiniteScroll></div>
	{/if}
	{#if search.postsLoadingMore}
		<div class="loading-more">Loading…</div>
	{/if}
{/if}

<style>
	.results-list {
		list-style: none;
		margin: 0;
		padding: 0;
		display: flex;
		flex-direction: column;
	}

	.post-item {
		padding: 14px 16px;
		border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.06));
		cursor: pointer;
		outline: none;
		transition: background-color 0.15s ease;
		animation: fadeSlideUp 0.25s ease both;
	}

	.post-item:hover,
	.post-item:focus-visible { background-color: var(--color-surface-raised, #1a1a1a); }

	.post-item:focus-visible { box-shadow: inset 0 0 0 2px var(--color-accent, #e05252); }

	.post-meta {
		display: flex;
		align-items: center;
		gap: 10px;
		margin-bottom: 8px;
	}

	.post-author-info {
		display: flex;
		align-items: baseline;
		gap: 6px;
		min-width: 0;
	}

	.author-name {
		font-size: 14px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.post-time {
		font-size: 12px;
		color: rgba(255, 255, 255, 0.4);
		flex-shrink: 0;
	}

	.post-excerpt {
		font-size: 14px;
		line-height: 1.5;
		color: rgba(255, 255, 255, 0.75);
		margin: 0;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow: hidden;
	}

	.post-avatar {
		width: 32px;
		height: 32px;
		border-radius: 50%;
		background: var(--color-surface-raised, #242424);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.avatar-img { width: 100%; height: 100%; object-fit: cover; }

	.avatar-initial {
		font-size: 13px;
		font-weight: 600;
		color: rgba(255, 255, 255, 0.5);
		user-select: none;
	}

	.skeleton-list { display: flex; flex-direction: column; padding-top: 4px; }

	.post-skeleton {
		padding: 14px 16px;
		border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.06));
		animation: fadeSlideUp 0.22s ease both;
	}

	.post-skeleton-header {
		display: flex;
		align-items: center;
		gap: 10px;
		margin-bottom: 10px;
	}

	.skeleton-avatar {
		width: 32px;
		height: 32px;
		border-radius: 50%;
		background: var(--color-surface-raised, #242424);
		flex-shrink: 0;
		animation: shimmer 1.4s ease-in-out infinite;
	}

	.skeleton-meta { flex: 1; display: flex; flex-direction: column; gap: 6px; }

	.skeleton-line {
		border-radius: 4px;
		background: var(--color-surface-raised, #242424);
		animation: shimmer 1.4s ease-in-out infinite;
	}

	.skeleton-name { height: 13px; width: 40%; }
	.skeleton-time { height: 11px; width: 20%; animation-delay: 0.1s; }
	.skeleton-body { height: 13px; width: 90%; animation-delay: 0.05s; }
	.skeleton-body-short { height: 13px; width: 65%; margin-top: 6px; animation-delay: 0.15s; }

	.sentinel { height: 1px; }

	.loading-more {
		padding: 16px;
		text-align: center;
		font-size: 13px;
		color: rgba(255, 255, 255, 0.5);
	}

	@keyframes fadeSlideUp {
		from { opacity: 0; transform: translateY(12px); }
		to { opacity: 1; transform: translateY(0); }
	}

	@keyframes shimmer {
		0%, 100% { opacity: 0.5; }
		50% { opacity: 1; }
	}
</style>
