<script lang="ts">
	import { onMount } from 'svelte';
	import type { Post } from '$lib/types';
	import { postService } from '$lib/services';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';
	import PostCard from '$lib/components/PostCard.svelte';
	import PostCardSkeleton from '$lib/components/PostCardSkeleton.svelte';

	type FeedState = 'loading' | 'loaded' | 'error' | 'empty';

	let posts = $state<Post[]>([]);
	let feedState = $state<FeedState>('loading');
	let errorMessage = $state('');

	const SKELETON_COUNT = 5;

	onMount(() => {
		loadFeed();
	});

	async function loadFeed(): Promise<void> {
		feedState = 'loading';
		errorMessage = '';
		try {
			const data = await postService.getFeed();
			posts = data;
			feedState = data.length === 0 ? 'empty' : 'loaded';
		} catch (err: unknown) {
			errorMessage = err instanceof Error ? err.message : 'Failed to load feed.';
			feedState = 'error';
		}
	}
</script>

<svelte:head>
	<title>Critiqal — Feed</title>
	<meta name="description" content="Your Critiqal feed" />
</svelte:head>

<div class="page-layout">
	<div class="col-left">
		<LeftSidebar />
	</div>

	<main class="col-center" aria-label="Feed">
		<header class="feed-header">
			<h1 class="feed-title">Feed</h1>
		</header>

		{#if feedState === 'loading'}
			<div aria-busy="true" aria-label="Loading feed">
				{#each { length: SKELETON_COUNT } as _, i (i)}
					<PostCardSkeleton />
				{/each}
			</div>
		{:else if feedState === 'error'}
			<div class="state-box" role="alert">
				<p class="state-title">Something went wrong</p>
				<p class="state-body">{errorMessage}</p>
				<button class="retry-btn" onclick={loadFeed}>Try again</button>
			</div>
		{:else if feedState === 'empty'}
			<div class="state-box">
				<p class="state-title">Nothing here yet</p>
				<p class="state-body">Be the first to post something.</p>
			</div>
		{:else}
			<div class="post-list">
				{#each posts as post (post.id)}
					<PostCard {post} />
				{/each}
			</div>
		{/if}
	</main>

	<aside class="col-right" aria-label="Additional info">
		<div class="info-panel">
			<p class="info-label">Critiqal</p>
			<p class="info-body">A minimalist social network for people who care about quality.</p>
		</div>
	</aside>
</div>

<style>
	.page-layout {
		display: grid;
		grid-template-columns: 16rem 1fr 14rem;
		max-width: 68rem;
		margin: 0 auto;
		min-height: 100vh;
		gap: 0;
	}

	.col-left {
		position: sticky;
		top: 0;
		height: 100vh;
		overflow-y: auto;
		padding: 0 1.5rem 0 1rem;
		border-right: 1px solid var(--color-border);
	}

	.col-center {
		padding: 0 2rem;
		min-height: 100vh;
		border-right: 1px solid var(--color-border);
	}

	.col-right {
		padding: 1.5rem 1rem 1.5rem 1.5rem;
	}

	.feed-header {
		padding: 1.25rem 0;
		border-bottom: 1px solid var(--color-border);
		margin-bottom: 0;
		position: sticky;
		top: 0;
		background-color: var(--color-bg);
		z-index: 10;
	}

	.feed-title {
		font-size: 1.0625rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.01em;
	}

	.post-list {
		animation: fadeIn 0.2s ease-out;
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
		transition:
			background-color 0.15s ease,
			transform 0.1s ease;
	}

	.retry-btn:hover {
		background-color: var(--color-surface-raised);
	}

	.retry-btn:active {
		transform: scale(0.97);
	}

	.info-panel {
		padding: 1rem;
		border-radius: 0.75rem;
		background: var(--color-surface-raised);
		display: flex;
		flex-direction: column;
		gap: 0.375rem;
		margin-top: 1.5rem;
	}

	.info-label {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--color-text-primary);
	}

	.info-body {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		line-height: 1.5;
	}

	@keyframes fadeIn {
		from {
			opacity: 0;
			transform: translateY(0.375rem);
		}
		to {
			opacity: 1;
			transform: translateY(0);
		}
	}

	@media (max-width: 1024px) {
		.page-layout {
			grid-template-columns: 4.5rem 1fr;
		}

		.col-right {
			display: none;
		}
	}

	@media (max-width: 640px) {
		.page-layout {
			grid-template-columns: 1fr;
		}

		.col-left {
			display: none;
		}

		.col-center {
			padding: 0 1rem;
			border-right: none;
		}
	}
</style>
