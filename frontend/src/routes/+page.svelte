<script lang="ts">
	import { onMount } from 'svelte';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { feedCacheStore } from '$lib/stores/feed-cache.store.svelte';
	import { UseComposer } from '$lib/features/posts/useComposer.svelte';
	import { infiniteScroll } from '$lib/actions/infiniteScroll';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';
	import { Post as PostCard } from '$lib/components/post';
	import PostCardSkeleton from '$lib/components/PostCardSkeleton.svelte';
	import FeedComposeBox from '$lib/components/FeedComposeBox.svelte';
	import { t } from '$lib/i18n';

	const SKELETON_COUNT = 5;

	const composer = new UseComposer();

	const posts = $derived(feedCacheStore.posts);
	const hasNext = $derived(feedCacheStore.hasNext);
	const error = $derived(feedCacheStore.error);
	// Show skeletons only on truly cold load. If we have cached posts, render
	// them immediately and revalidate in the background.
	const showSkeleton = $derived(!feedCacheStore.hasData && error === null);

	onMount(() => {
		// Stale-while-revalidate: returns instantly if fresh, otherwise refetches.
		feedCacheStore.load();
	});

	async function loadMore(): Promise<void> {
		await feedCacheStore.loadMore();
	}

	function reloadFeed(): void {
		feedCacheStore.load({ force: true });
	}

	async function submitPost(): Promise<void> {
		// Two-phase: insert bare post on creation, patch photos in on upload.
		await composer.submit({
			onCreated: (post) => feedCacheStore.prependPost(post),
			onPhotosReady: (postId, photos) => {
				if (photos.length > 0) feedCacheStore.updatePost(postId, { photos });
			}
		});
	}

	function handlePostDeleted(postId: string): void {
		feedCacheStore.removePost(postId);
	}
</script>

<svelte:head>
	<title>Critiqal — {t('nav.feed')}</title>
	<meta name="description" content="Your Critiqal feed" />
</svelte:head>

<div class="page-layout">
	<div class="col-left">
		<LeftSidebar />
	</div>

	<main class="col-center" aria-label={t('nav.feed')}>
		<header class="feed-header">
			<h1 class="feed-title">{t('nav.feed')}</h1>
		</header>

		{#if authStore.isAuthenticated}
			<FeedComposeBox {composer} onSubmit={submitPost} />
		{/if}

		{#if showSkeleton}
			<div aria-busy="true" aria-label={t('common.loading')}>
				{#each { length: SKELETON_COUNT } as _, i (i)}
					<PostCardSkeleton />
				{/each}
			</div>
		{:else if error && posts.length === 0}
			<div class="state-box" role="alert">
				<p class="state-title">{t('common.error')}</p>
				<p class="state-body">{error}</p>
				<button class="retry-btn" onclick={reloadFeed}>{t('common.retry')}</button>
			</div>
		{:else if posts.length === 0}
			<div class="state-box">
				<p class="state-title">{t('feed.empty')}</p>
				<p class="state-body">{t('feed.emptyHint')}</p>
			</div>
		{:else}
			<div class="post-list">
				{#each posts as post (post.id)}
					<PostCard {post} onDeleted={handlePostDeleted} />
				{/each}
			</div>

			{#if hasNext}
				<div
					class="feed-sentinel"
					use:infiniteScroll={{ onTrigger: loadMore, disabled: feedCacheStore.isLoadingMore }}
				></div>
			{/if}
			{#if feedCacheStore.isLoadingMore}
				<div class="loading-more">{t('common.loading')}</div>
			{/if}
		{/if}
	</main>

	<aside class="col-right" aria-hidden="true"></aside>
</div>

<style>
	.page-layout {
		height: 100vh;
		overflow: hidden;
	}

	.col-left {
		position: fixed;
		/* anchor the sidebar's right edge to the centered feed's left edge
		   (feed is 42rem wide → half = 21rem from viewport center) */
		right: calc(50% + 21rem);
		top: 0;
		bottom: 0;
		width: 16rem;
		overflow-y: auto;
		padding: 0 1.5rem 0 1rem;
		z-index: 20;
	}

	.col-center {
		height: 100vh;
		max-width: 42rem;
		margin: 0 auto;
		overflow-y: auto;
		padding: 0 2rem 4rem;
		scrollbar-width: none;
	}

	.col-center::-webkit-scrollbar {
		display: none;
	}

	.col-right {
		display: none;
	}

	.feed-header {
		padding: 1.25rem 0 1.75rem;
		margin-bottom: -0.75rem;
		position: sticky;
		top: 0;
		/* Theme-aware glass via the shared tokens (.glass would add a border;
		   we want the seamless mask-faded edge, so we inline the surface but
		   reuse the same tokens for both themes). */
		background: var(--glass-bg-soft);
		backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
		-webkit-mask-image: linear-gradient(to bottom, #000 0%, #000 55%, transparent 100%);
		mask-image: linear-gradient(to bottom, #000 0%, #000 55%, transparent 100%);
		z-index: 10;
	}

	@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
		.feed-header {
			background: var(--color-bg);
		}
	}

	.feed-title {
		font-size: 1rem;
		font-weight: 600;
		color: var(--color-text-primary);
		letter-spacing: -0.015em;
	}

	.feed-sentinel {
		height: 1px;
	}

	.loading-more {
		padding: 1.5rem;
		text-align: center;
		font-size: 0.8125rem;
		color: var(--color-text-muted);
	}

	.state-box {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
		padding: 4rem 1rem;
		text-align: center;
	}

	.state-title {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text-primary);
	}

	.state-body {
		font-size: 0.875rem;
		color: var(--color-text-muted);
	}

	.retry-btn {
		margin-top: 0.75rem;
		padding: 0.5rem 1.25rem;
		border-radius: 0.5rem;
		border: 1px solid var(--color-border);
		background: none;
		color: var(--color-text-primary);
		font-size: 0.875rem;
		font-weight: 500;
		cursor: pointer;
		transition: background-color 0.15s ease, transform 0.1s ease;
	}

	.retry-btn:hover {
		background-color: var(--color-surface-raised);
	}

	.retry-btn:active {
		transform: scale(0.97);
	}

	@media (max-width: 900px) {
		.col-left {
			width: 4.5rem;
			padding: 0 0.5rem;
		}
	}

	@media (max-width: 640px) {
		.col-left {
			display: none;
		}

		.col-center {
			padding: 0 1rem 4rem;
		}
	}
</style>
