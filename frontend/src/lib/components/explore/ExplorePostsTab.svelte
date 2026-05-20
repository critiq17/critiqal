<script lang="ts">
	import type { UseSearch } from '$lib/features/explore/useSearch.svelte';
	import { Post as PostComponent } from '$lib/components/post';
	import ExploreEmptyState from './ExploreEmptyState.svelte';

	const SKELETON_COUNT = 3;

	interface Props {
		search: UseSearch;
		query: string;
		onRetry: () => void;
		onAuthorClick: (username: string) => void;
		onOpenComments?: (postId: string) => void;
		onPostDeleted?: (id: string) => void;
	}

	let { search, onRetry, onAuthorClick, onOpenComments, onPostDeleted }: Props = $props();

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
	<ExploreEmptyState variant="error" message={search.postsError} {onRetry} />
{:else if search.posts.length === 0}
	<ExploreEmptyState variant="empty" message="Try a different search" />
{:else}
	<div class="results-list" aria-label="Post results">
		{#each search.posts as post (post.id)}
			<PostComponent
				{post}
				variant="mobile"
				onAuthorClick={(username) => onAuthorClick(username)}
				onOpenComments={(postId) => onOpenComments?.(postId)}
				onDeleted={(id) => onPostDeleted?.(id)}
			/>
		{/each}
	</div>

	{#if search.postsHasNext}
		<div class="sentinel" use:infiniteScroll></div>
	{/if}
	{#if search.postsLoadingMore}
		<div class="loading-more">Loading…</div>
	{/if}
{/if}

<style>
	.results-list {
		display: flex;
		flex-direction: column;
	}

	.skeleton-list { display: flex; flex-direction: column; padding-top: 4px; }

	.post-skeleton {
		padding: 14px 16px;
		border-bottom: 1px solid var(--color-border, var(--surface-tint-soft));
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
		color: var(--text-tertiary);
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
