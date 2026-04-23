<script lang="ts">
	import { onMount } from 'svelte';
	import { fade } from 'svelte/transition';
	import type { Post } from '$lib/types';
	import { postService, mediaService } from '$lib/services';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';
	import { Post as PostCard } from '$lib/components/post';
	import PostCardSkeleton from '$lib/components/PostCardSkeleton.svelte';

	interface PendingPhoto {
		file: File;
		previewUrl: string;
	}

	const MAX_PHOTOS = 3;
	const SKELETON_COUNT = 5;

	let posts = $state<Post[]>([]);
	let isLoading = $state(false);
	let isLoadingMore = $state(false);
	let hasNext = $state(false);
	let currentPage = $state(0);
	let error = $state<string | null>(null);

	let composeText = $state('');
	let isPosting = $state(false);
	let pendingPhotos = $state<PendingPhoto[]>([]);
	let isUploadingPhotos = $state(false);

	onMount(() => {
		loadFeed();
	});

	async function loadFeed(): Promise<void> {
		isLoading = true;
		error = null;
		posts = [];
		hasNext = false;
		currentPage = 0;
		try {
			const res = await postService.getFeed(0);
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
			const res = await postService.getFeed(nextPage);
			posts = [...posts, ...res.content];
			currentPage = nextPage;
			hasNext = res.hasNext;
		} catch {
			// non-fatal
		} finally {
			isLoadingMore = false;
		}
	}

	function infiniteScroll(el: HTMLElement): { destroy: () => void } {
		const obs = new IntersectionObserver(
			([entry]) => {
				if (entry?.isIntersecting && !isLoadingMore) loadMore();
			},
			{ threshold: 0.1 }
		);
		obs.observe(el);
		return { destroy: () => obs.disconnect() };
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
					const photos: import('$lib/types').PostPhoto[] = [];
					for (const p of photosToUpload) {
						try {
							const photo = await mediaService.uploadPostPhoto(newPost.id, p.file);
							photos.push(photo);
						} catch {
							// skip failed photo, continue with the rest
						}
					}
					posts = [{ ...newPost, photos }, ...posts];
				} finally {
					isUploadingPhotos = false;
				}
			} else {
				posts = [newPost, ...posts];
			}

			composeText = '';
			clearPendingPhotos();
		} catch {
			// ignore
		} finally {
			isPosting = false;
		}
	}

	function handleComposeKey(e: KeyboardEvent): void {
		if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
			submitPost();
		}
	}

	function handlePostDeleted(postId: number): void {
		posts = posts.filter((p) => p.id !== postId);
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
			<div class="compose-box">
				<div class="compose-avatar" aria-hidden="true">
					{#if authStore.user?.avatarUrl}
						<img src={authStore.user.avatarUrl} alt={authStore.user.username} class="compose-avatar-img" />
					{:else}
						<span class="compose-avatar-initial">
							{(authStore.user?.name ?? authStore.user?.username ?? '?').charAt(0).toUpperCase()}
						</span>
					{/if}
				</div>
				<div class="compose-right">
					<textarea
						class="compose-input"
						bind:value={composeText}
						onkeydown={handleComposeKey}
						placeholder="What's on your mind?"
						rows={3}
						disabled={isPosting}
						aria-label="Write a post"
					></textarea>
					{#if pendingPhotos.length > 0}
						<div class="photo-thumbnail-row">
							{#each pendingPhotos as photo, i (photo.previewUrl)}
								<div class="photo-thumbnail-wrap">
									<img src={photo.previewUrl} alt="Selected photo {i + 1}" class="photo-thumbnail-img" />
									<button
										class="photo-remove-btn"
										onclick={() => removePhoto(i)}
										aria-label="Remove photo {i + 1}"
										type="button"
									>×</button>
								</div>
							{/each}
						</div>
					{/if}
					<div class="compose-actions">
						<div class="compose-actions-left">
							{#if pendingPhotos.length < MAX_PHOTOS}
								<label class="compose-photo-label" aria-label="Attach photo" title="Attach photo (up to {MAX_PHOTOS})">
									<input
										type="file"
										accept="image/jpeg,image/png,image/webp"
										class="compose-photo-input"
										onchange={handlePhotoSelect}
										disabled={isPosting || isUploadingPhotos}
										multiple
									/>
									<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18" aria-hidden="true">
										<path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
										<circle cx="12" cy="13" r="4" />
									</svg>
								</label>
							{/if}
							<span class="compose-hint">
								{#if pendingPhotos.length > 0}
									{pendingPhotos.length}/{MAX_PHOTOS} photos · Ctrl+Enter to post
								{:else}
									Ctrl+Enter to post
								{/if}
							</span>
						</div>
						<button
							class="compose-submit"
							onclick={submitPost}
							disabled={!composeText.trim() || isPosting || isUploadingPhotos}
						>
							{#if isUploadingPhotos}
								Uploading…
							{:else if isPosting}
								Posting…
							{:else}
								Post
							{/if}
						</button>
					</div>
				</div>
			</div>
		{/if}

		{#if isLoading}
			<div aria-busy="true" aria-label="Loading feed">
				{#each { length: SKELETON_COUNT } as _, i (i)}
					<PostCardSkeleton />
				{/each}
			</div>
		{:else if error}
			<div class="state-box" role="alert">
				<p class="state-title">Something went wrong</p>
				<p class="state-body">{error}</p>
				<button class="retry-btn" onclick={loadFeed}>Try again</button>
			</div>
		{:else if posts.length === 0}
			<div class="state-box">
				<p class="state-title">Nothing here yet</p>
				<p class="state-body">Be the first to post something.</p>
			</div>
		{:else}
			<div class="post-list" transition:fade={{ duration: 150 }}>
				{#each posts as post (post.id)}
					<PostCard {post} onDeleted={handlePostDeleted} />
				{/each}
			</div>

			{#if hasNext}
				<div class="feed-sentinel" use:infiniteScroll></div>
			{/if}
			{#if isLoadingMore}
				<div class="loading-more">Loading more…</div>
			{/if}
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
		grid-template-columns: 16rem 42rem 14rem;
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
		overflow-y: auto;
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

	/* Compose box */
	.compose-box {
		display: flex;
		gap: 0.75rem;
		padding: 1.25rem 0;
		border-bottom: 1px solid var(--color-border);
	}

	.compose-avatar {
		width: 2.5rem;
		height: 2.5rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.compose-avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.compose-avatar-initial {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--color-text-muted);
		user-select: none;
	}

	.compose-right {
		flex: 1;
		display: flex;
		flex-direction: column;
		gap: 0.625rem;
	}

	.compose-input {
		width: 100%;
		background: none;
		border: none;
		outline: none;
		font-size: 1rem;
		color: var(--color-text-primary);
		font-family: inherit;
		resize: none;
		line-height: 1.5;
		padding: 0;
	}

	.compose-input::placeholder {
		color: var(--color-text-muted);
		opacity: 0.5;
	}

	.compose-input:disabled {
		opacity: 0.6;
	}

	.compose-actions {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.compose-hint {
		font-size: 0.75rem;
		color: var(--color-text-muted);
		opacity: 0.6;
	}

	.compose-actions-left {
		display: flex;
		align-items: center;
		gap: 0.625rem;
	}

	.compose-photo-label {
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		color: var(--color-text-muted);
		padding: 0.25rem;
		border-radius: 0.375rem;
		transition: color 0.15s ease, background-color 0.15s ease;
	}

	.compose-photo-label:hover {
		color: var(--color-text-primary);
		background-color: var(--color-surface-raised);
	}

	.compose-photo-input {
		display: none;
	}

	.photo-thumbnail-row {
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
		margin-bottom: 0.5rem;
	}

	.photo-thumbnail-wrap {
		position: relative;
		width: 72px;
		height: 72px;
		flex-shrink: 0;
		border-radius: 0.5rem;
		overflow: hidden;
	}

	.photo-thumbnail-img {
		display: block;
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.photo-remove-btn {
		position: absolute;
		top: 3px;
		right: 3px;
		background: rgba(0, 0, 0, 0.7);
		color: #fff;
		border: none;
		border-radius: 50%;
		width: 16px;
		height: 16px;
		font-size: 0.6875rem;
		line-height: 1;
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		transition: background-color 0.15s ease;
		padding: 0;
	}

	.photo-remove-btn:hover {
		background: rgba(0, 0, 0, 0.9);
	}

	.compose-submit {
		background: var(--color-text-primary);
		color: var(--color-bg);
		border: none;
		border-radius: 9999px;
		padding: 0.4375rem 1.125rem;
		font-size: 0.875rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition: opacity 0.15s ease, transform 0.1s ease;
	}

	.compose-submit:disabled {
		opacity: 0.35;
		cursor: not-allowed;
	}

	.compose-submit:not(:disabled):hover {
		opacity: 0.85;
	}

	.compose-submit:not(:disabled):active {
		transform: scale(0.96);
	}

	/* .post-list: transition handled by Svelte fade directive */

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
			padding: 0 1rem 4rem;
		}
	}
</style>
