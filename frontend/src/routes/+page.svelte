<script lang="ts">
	import { onMount } from 'svelte';
	import type { PostPhoto } from '$lib/types';
	import { postService, mediaService } from '$lib/services';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { feedCacheStore } from '$lib/stores/feed-cache.store.svelte';
	import { infiniteScroll } from '$lib/actions/infiniteScroll';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';
	import { Post as PostCard } from '$lib/components/post';
	import PostCardSkeleton from '$lib/components/PostCardSkeleton.svelte';
	import FeedComposeBox from '$lib/components/FeedComposeBox.svelte';

	interface PendingPhoto {
		file: File;
		previewUrl: string;
	}

	const MAX_PHOTOS = 3;
	const SKELETON_COUNT = 5;

	let composeText = $state('');
	let isPosting = $state(false);
	let pendingPhotos = $state<PendingPhoto[]>([]);
	let isUploadingPhotos = $state(false);

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

	function handlePhotoSelect(e: Event): void {
		const input = e.target as HTMLInputElement;
		const files = input.files;
		if (!files || files.length === 0) return;

		const remaining = MAX_PHOTOS - pendingPhotos.length;
		const toAdd = Array.from(files).slice(0, remaining);
		const newPending: PendingPhoto[] = toAdd.map((file) => ({
			file,
			previewUrl: URL.createObjectURL(file)
		}));

		pendingPhotos = [...pendingPhotos, ...newPending];
		input.value = '';
	}

	function removePhoto(index: number): void {
		const photo = pendingPhotos[index];
		if (photo) URL.revokeObjectURL(photo.previewUrl);
		pendingPhotos = pendingPhotos.filter((_, i) => i !== index);
	}

	function clearPendingPhotos(): void {
		pendingPhotos.forEach((p) => URL.revokeObjectURL(p.previewUrl));
		pendingPhotos = [];
	}

	async function submitPost(): Promise<void> {
		const content = composeText.trim();
		if (!content || isPosting) return;
		isPosting = true;
		try {
			const newPost = await postService.create({ content });
			const photosToUpload = [...pendingPhotos];

			if (photosToUpload.length > 0) {
				isUploadingPhotos = true;
				try {
					const photos: PostPhoto[] = [];
					for (const p of photosToUpload) {
						try {
							const photo = await mediaService.uploadPostPhoto(newPost.id, p.file);
							photos.push(photo);
						} catch {
							// skip failed photo
						}
					}
					feedCacheStore.prependPost({ ...newPost, photos });
				} finally {
					isUploadingPhotos = false;
				}
			} else {
				feedCacheStore.prependPost(newPost);
			}

			composeText = '';
			clearPendingPhotos();
		} catch {
			// ignore
		} finally {
			isPosting = false;
		}
	}

	function handlePostDeleted(postId: string): void {
		feedCacheStore.removePost(postId);
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

		{#if authStore.isAuthenticated}
			<FeedComposeBox
				text={composeText}
				{pendingPhotos}
				{isPosting}
				{isUploadingPhotos}
				maxPhotos={MAX_PHOTOS}
				onTextChange={(v) => { composeText = v; }}
				onPhotoSelect={handlePhotoSelect}
				onRemovePhoto={removePhoto}
				onSubmit={submitPost}
			/>
		{/if}

		{#if showSkeleton}
			<div aria-busy="true" aria-label="Loading feed">
				{#each { length: SKELETON_COUNT } as _, i (i)}
					<PostCardSkeleton />
				{/each}
			</div>
		{:else if error && posts.length === 0}
			<div class="state-box" role="alert">
				<p class="state-title">Something went wrong</p>
				<p class="state-body">{error}</p>
				<button class="retry-btn" onclick={reloadFeed}>Try again</button>
			</div>
		{:else if posts.length === 0}
			<div class="state-box">
				<p class="state-title">Nothing here yet</p>
				<p class="state-body">Be the first to post something.</p>
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
				<div class="loading-more">Loading more…</div>
			{/if}
		{/if}
	</main>

	<aside class="col-right" aria-hidden="true"></aside>
</div>

<style>
	.page-layout {
		display: grid;
		grid-template-columns: 16rem 42rem;
		justify-content: center;
		height: 100vh;
		overflow: hidden;
	}

	.col-left {
		overflow-y: auto;
		padding: 0 1.5rem 0 1rem;
	}

	.col-center {
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
		padding: 1.25rem 0;
		margin-bottom: 0;
		position: sticky;
		top: 0;
		background-color: rgba(12, 12, 12, 0.85);
		backdrop-filter: blur(12px);
		-webkit-backdrop-filter: blur(12px);
		z-index: 10;
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
		.page-layout {
			grid-template-columns: 4.5rem 1fr;
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
			padding: 0 1rem 4rem;
		}
	}
</style>
