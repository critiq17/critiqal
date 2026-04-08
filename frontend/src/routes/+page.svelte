<script lang="ts">
	import { onMount } from 'svelte';
	import type { Post } from '$lib/types';
	import { postService } from '$lib/services';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';
	import PostCard from '$lib/components/PostCard.svelte';
	import PostCardSkeleton from '$lib/components/PostCardSkeleton.svelte';

	type FeedState = 'loading' | 'loaded' | 'error' | 'empty';

	let posts = $state<Post[]>([]);
	let feedState = $state<FeedState>('loading');
	let errorMessage = $state('');

	let composeText = $state('');
	let isPosting = $state(false);

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

	async function submitPost(): Promise<void> {
		const content = composeText.trim();
		if (!content || isPosting) return;
		isPosting = true;
		try {
			const newPost = await postService.create({ content });
			posts = [newPost, ...posts];
			composeText = '';
			feedState = 'loaded';
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
		if (posts.length === 0) feedState = 'empty';
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
					<div class="compose-actions">
						<span class="compose-hint">Ctrl+Enter to post</span>
						<button
							class="compose-submit"
							onclick={submitPost}
							disabled={!composeText.trim() || isPosting}
						>
							{isPosting ? 'Posting…' : 'Post'}
						</button>
					</div>
				</div>
			</div>
		{/if}

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
					<PostCard {post} onDeleted={handlePostDeleted} />
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
		grid-template-columns: 16rem minmax(0, 42rem) 14rem;
		justify-content: center;
		min-height: 100vh;
	}

	.col-left {
		position: sticky;
		top: 0;
		height: 100vh;
		overflow-y: auto;
		padding: 0 1.5rem 0 1rem;
	}

	.col-center {
		padding: 0 2rem;
		min-height: 100vh;
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
		}
	}
</style>
