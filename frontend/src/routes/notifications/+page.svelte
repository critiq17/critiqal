<script lang="ts">
	import { onMount } from 'svelte';
	import type { Post } from '$lib/types';
	import { postService } from '$lib/services';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { infiniteScroll } from '$lib/actions/infiniteScroll';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';
	import { Post as PostCard } from '$lib/components/post';
	import PostCardSkeleton from '$lib/components/PostCardSkeleton.svelte';

	const SKELETON_COUNT = 3;

	let posts = $state<Post[]>([]);
	let isLoading = $state(false);
	let isLoadingMore = $state(false);
	let hasNext = $state(false);
	let currentPage = $state(0);
	let error = $state<string | null>(null);

	onMount(() => {
		if (authStore.isAuthenticated) load();
	});

	async function load(): Promise<void> {
		isLoading = true;
		error = null;
		posts = [];
		hasNext = false;
		currentPage = 0;
		try {
			const res = await postService.getFollowingFeed(0);
			posts = res.content;
			hasNext = res.hasNext;
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load feed.';
		} finally {
			isLoading = false;
		}
	}

	async function loadMore(): Promise<void> {
		if (!hasNext || isLoadingMore) return;
		isLoadingMore = true;
		try {
			const nextPage = currentPage + 1;
			const res = await postService.getFollowingFeed(nextPage);
			posts = [...posts, ...res.content];
			hasNext = res.hasNext;
			currentPage = nextPage;
		} catch {
			// non-fatal
		} finally {
			isLoadingMore = false;
		}
	}
</script>

<svelte:head>
	<title>Following — Critiqal</title>
</svelte:head>

<div class="page-layout">
	<div class="col-left">
		<LeftSidebar />
	</div>

	<main class="col-center" aria-label="Following feed">
		<header class="page-header">
			<h1 class="page-title">Following</h1>
			<p class="page-subtitle">Posts from people you follow</p>
		</header>

		{#if !authStore.isAuthenticated}
			<div class="empty-state">
				<p>Sign in to see posts from people you follow.</p>
				<a href="/login" class="btn-primary">Sign in</a>
			</div>
		{:else if isLoading}
			<div class="feed">
				{#each { length: SKELETON_COUNT } as _, i (i)}
					<PostCardSkeleton />
				{/each}
			</div>
		{:else if error}
			<div class="error-state" role="alert">
				<p>{error}</p>
				<button class="btn-retry" onclick={load}>Try again</button>
			</div>
		{:else if posts.length === 0}
			<div class="empty-state">
				<p class="empty-title">Nothing here yet</p>
				<p class="empty-body">Follow people to see their posts here.</p>
				<a href="/explore" class="btn-primary">Find people</a>
			</div>
		{:else}
			<div class="feed">
				{#each posts as post (post.id)}
					<PostCard {post} variant="desktop" />
				{/each}

				{#if hasNext}
					<div class="sentinel" use:infiniteScroll={{ onTrigger: loadMore, disabled: isLoadingMore }}></div>
				{/if}

				{#if isLoadingMore}
					<div class="loading-more" aria-busy="true">Loading…</div>
				{/if}
			</div>
		{/if}
	</main>
</div>

<style>
	.page-layout {
		display: grid;
		grid-template-columns: 280px 1fr;
		min-height: 100vh;
		max-width: 900px;
		margin: 0 auto;
	}

	.col-left {
		position: sticky;
		top: 0;
		height: 100vh;
	}

	.col-center {
		border-left: 1px solid var(--color-border);
		min-height: 100vh;
	}

	.page-header {
		padding: 24px 24px 16px;
		border-bottom: 1px solid var(--color-border);
	}

	.page-title {
		font-size: 1.25rem;
		font-weight: 700;
		color: var(--color-text-primary);
		margin: 0 0 4px;
	}

	.page-subtitle {
		font-size: 0.8rem;
		color: var(--color-text-muted);
		margin: 0;
	}

	.feed {
		padding: 0 8px;
	}

	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 12px;
		padding: 80px 24px;
		text-align: center;
	}

	.empty-title {
		font-size: 1rem;
		font-weight: 600;
		color: var(--color-text-primary);
		margin: 0;
	}

	.empty-body {
		font-size: 0.875rem;
		color: var(--color-text-muted);
		margin: 0;
	}

	.btn-primary {
		display: inline-block;
		padding: 10px 24px;
		background: var(--color-accent);
		color: #fff;
		border-radius: var(--radius-full);
		font-size: 0.875rem;
		font-weight: 600;
		text-decoration: none;
		transition: opacity 0.15s ease;
	}

	.btn-primary:hover {
		opacity: 0.85;
	}

	.error-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 12px;
		padding: 60px 24px;
		color: var(--color-accent);
		font-size: 0.875rem;
		text-align: center;
	}

	.btn-retry {
		padding: 8px 20px;
		border-radius: var(--radius-full);
		border: 1px solid var(--color-border);
		background: none;
		color: var(--color-text-primary);
		font-size: 0.875rem;
		cursor: pointer;
		font-family: inherit;
	}

	.sentinel {
		height: 1px;
	}

	.loading-more {
		padding: 16px;
		text-align: center;
		font-size: 0.8rem;
		color: var(--color-text-muted);
	}
</style>
