<script lang="ts">
	import { untrack } from 'svelte';
	import { goto } from '$app/navigation';
	import type { Post, User } from '$lib/types';
	import { postService, userService } from '$lib/services';
	import { Post as PostCard } from '$lib/components/post';
	import PostCardSkeleton from '$lib/components/PostCardSkeleton.svelte';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';

	type Tab = 'posts' | 'people';
	type UsersState = 'idle' | 'loading' | 'loaded' | 'error';

	const SKELETON_COUNT = 3;
	const DEBOUNCE_MS = 300;

	// --- Query / tab state ---
	let query = $state('');
	let activeTab = $state<Tab>('posts');
	let loadedQuery = $state<string | null>(null);
	let loadedTab = $state<Tab | null>(null);

	// --- Posts paginated state ---
	let posts = $state<Post[]>([]);
	let postsPage = $state(0);
	let postsHasNext = $state(false);
	let postsLoading = $state(false);
	let postsLoadingMore = $state(false);
	let postsError = $state<string | null>(null);

	// --- Users state ---
	let users = $state<User[]>([]);
	let usersState = $state<UsersState>('idle');
	let usersError = $state('');

	// --- Posts pagination ---
	async function loadPosts(q: string): Promise<void> {
		postsLoading = true;
		postsError = null;
		posts = [];
		postsPage = 0;
		postsHasNext = false;
		try {
			const res = q.trim()
				? await postService.search(q.trim(), 0)
				: await postService.getFeed(0);
			posts = res.content;
			postsHasNext = res.hasNext;
		} catch (err) {
			postsError = err instanceof Error ? err.message : 'Something went wrong.';
		} finally {
			postsLoading = false;
		}
	}

	async function loadMorePosts(): Promise<void> {
		if (!postsHasNext || postsLoadingMore) return;
		postsLoadingMore = true;
		const q = query;
		try {
			const nextPage = postsPage + 1;
			const res = q.trim()
				? await postService.search(q.trim(), nextPage)
				: await postService.getFeed(nextPage);
			posts = [...posts, ...res.content];
			postsPage = nextPage;
			postsHasNext = res.hasNext;
		} catch {
			// non-fatal
		} finally {
			postsLoadingMore = false;
		}
	}

	// --- IntersectionObserver action ---
	function infiniteScroll(el: HTMLElement): { destroy: () => void } {
		const obs = new IntersectionObserver(
			([entry]) => {
				if (entry?.isIntersecting && !postsLoadingMore) loadMorePosts();
			},
			{ threshold: 0.1 }
		);
		obs.observe(el);
		return { destroy: () => obs.disconnect() };
	}

	// --- Debounce ---
	function debounce<T extends unknown[]>(fn: (...args: T) => void, delay: number) {
		let timer: ReturnType<typeof setTimeout> | undefined;
		return (...args: T): void => {
			clearTimeout(timer);
			timer = setTimeout(() => fn(...args), delay);
		};
	}

	// --- Fetch results ---
	async function fetchResults(q: string, tab: Tab): Promise<void> {
		if (loadedQuery === q && loadedTab === tab) return;

		if (tab === 'posts') {
			await loadPosts(q);
			if (!postsError) {
				loadedQuery = q;
				loadedTab = tab;
			}
		} else {
			usersState = 'loading';
			usersError = '';
			try {
				users = await userService.search(q.trim());
				loadedQuery = q;
				loadedTab = tab;
				usersState = 'loaded';
			} catch (err: unknown) {
				usersError = err instanceof Error ? err.message : 'Something went wrong.';
				usersState = 'error';
			}
		}
	}

	const debouncedFetch = debounce(fetchResults, DEBOUNCE_MS);

	$effect(() => {
		const q = query;
		const tab = activeTab;

		const alreadyLoaded = untrack(() => loadedQuery === q && loadedTab === tab);
		if (alreadyLoaded) return;

		const queryChanged = untrack(() => q !== loadedQuery);
		if (queryChanged) {
			debouncedFetch(q, tab);
		} else {
			fetchResults(q, tab);
		}
	});

	// --- Helpers ---
	function getInitials(user: User): string {
		return (user.name ?? user.username).charAt(0).toUpperCase();
	}

	function navigateToUser(u: string): void {
		goto(`/${u}`);
	}

	function handleUserKeydown(e: KeyboardEvent, u: string): void {
		if (e.key === 'Enter' || e.key === ' ') {
			e.preventDefault();
			navigateToUser(u);
		}
	}

	// --- Derived ---
	const isDefaultState = $derived(query.trim() === '');
	const headerTitle = $derived(isDefaultState ? 'Explore' : 'Results');
	const emptyPostsMessage = $derived(
		isDefaultState ? 'Nothing to recommend yet.' : 'No posts found for that search.'
	);
	const emptyUsersMessage = $derived(
		isDefaultState ? 'No people to suggest yet.' : 'No users found for that search.'
	);
</script>

<svelte:head>
	<title>Critiqal — Explore</title>
	<meta name="description" content="Search posts and people on Critiqal" />
</svelte:head>

<div class="page-layout">
	<div class="col-left">
		<LeftSidebar />
	</div>

	<main class="col-center" aria-label="Explore">
		<!-- Sticky header -->
		<header class="explore-header">
			<h1 class="explore-title">{headerTitle}</h1>

			<div class="search-wrapper">
				<svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
					<circle cx="11" cy="11" r="8" />
					<line x1="21" y1="21" x2="16.65" y2="16.65" />
				</svg>
				<input
					class="search-input"
					type="search"
					bind:value={query}
					placeholder="Search posts or people…"
					aria-label="Search"
					autocomplete="off"
					spellcheck="false"
				/>
				{#if query}
					<button
						class="search-clear"
						onclick={() => (query = '')}
						aria-label="Clear search"
					>
						<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
							<line x1="18" y1="6" x2="6" y2="18" />
							<line x1="6" y1="6" x2="18" y2="18" />
						</svg>
					</button>
				{/if}
			</div>

			<!-- Tab bar -->
			<div class="tab-bar" role="tablist" aria-label="Content type">
				<button
					class="tab-btn"
					class:tab-active={activeTab === 'posts'}
					role="tab"
					aria-selected={activeTab === 'posts'}
					onclick={() => (activeTab = 'posts')}
				>
					Posts
				</button>
				<button
					class="tab-btn"
					class:tab-active={activeTab === 'people'}
					role="tab"
					aria-selected={activeTab === 'people'}
					onclick={() => (activeTab = 'people')}
				>
					People
				</button>
				<span
					class="tab-indicator"
					style:transform={activeTab === 'posts' ? 'translateX(0%)' : 'translateX(100%)'}
					aria-hidden="true"
				></span>
			</div>
		</header>

		<!-- Content area -->
		<div class="content-area" role="tabpanel" aria-label={activeTab === 'posts' ? 'Posts results' : 'People results'}>

			{#if activeTab === 'posts'}
				{#if postsLoading}
					<div aria-busy="true" aria-label="Loading posts">
						{#each { length: SKELETON_COUNT } as _, i (i)}
							<PostCardSkeleton />
						{/each}
					</div>
				{:else if postsError}
					<div class="empty-state" role="alert">
						<svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
							<circle cx="12" cy="12" r="10" />
							<line x1="12" y1="8" x2="12" y2="12" />
							<line x1="12" y1="16" x2="12.01" y2="16" />
						</svg>
						<p class="empty-title">Something went wrong</p>
						<p class="empty-body">{postsError}</p>
						<button class="retry-btn" onclick={() => fetchResults(query, activeTab)}>Try again</button>
					</div>
				{:else if posts.length === 0}
					<div class="empty-state">
						<svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
							<path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
							<polyline points="14 2 14 8 20 8" />
							<line x1="9" y1="13" x2="15" y2="13" />
						</svg>
						<p class="empty-title">No posts yet</p>
						<p class="empty-body">{emptyPostsMessage}</p>
					</div>
				{:else}
					<div class="results-list">
						{#each posts as post, i (post.id)}
							<div class="result-item" style:animation-delay="{i * 40}ms">
								<PostCard {post} />
							</div>
						{/each}
					</div>

					{#if postsHasNext}
						<div class="explore-sentinel" use:infiniteScroll></div>
					{/if}
					{#if postsLoadingMore}
						<div class="loading-more">Loading more…</div>
					{/if}
				{/if}

			{:else}
				<!-- People tab -->
				{#if usersState === 'loading'}
					<div class="user-skeleton-list" aria-busy="true" aria-label="Loading people">
						{#each { length: SKELETON_COUNT } as _, i (i)}
							<div class="user-skeleton">
								<div class="user-skeleton-avatar"></div>
								<div class="user-skeleton-meta">
									<div class="user-skeleton-line user-skeleton-name"></div>
									<div class="user-skeleton-line user-skeleton-username"></div>
								</div>
							</div>
						{/each}
					</div>
				{:else if usersState === 'error'}
					<div class="empty-state" role="alert">
						<svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
							<circle cx="12" cy="12" r="10" />
							<line x1="12" y1="8" x2="12" y2="12" />
							<line x1="12" y1="16" x2="12.01" y2="16" />
						</svg>
						<p class="empty-title">Something went wrong</p>
						<p class="empty-body">{usersError}</p>
						<button class="retry-btn" onclick={() => fetchResults(query, activeTab)}>Try again</button>
					</div>
				{:else if users.length === 0}
					<div class="empty-state">
						<svg class="empty-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
							<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
							<circle cx="12" cy="7" r="4" />
						</svg>
						<p class="empty-title">No people yet</p>
						<p class="empty-body">{emptyUsersMessage}</p>
					</div>
				{:else}
					<ul class="user-list" aria-label="People">
						{#each users as user, i (user.id)}
							<li
								class="user-card"
								style:animation-delay="{i * 40}ms"
								onclick={() => navigateToUser(user.username)}
								onkeydown={(e) => handleUserKeydown(e, user.username)}
								role="link"
								tabindex="0"
								aria-label="View {user.name ?? user.username}'s profile"
							>
								<div class="user-avatar" aria-hidden="true">
									{#if user.avatarUrl}
										<img src={user.avatarUrl} alt={user.username} class="user-avatar-img" />
									{:else}
										<span class="user-avatar-initial">{getInitials(user)}</span>
									{/if}
								</div>
								<div class="user-info">
									<span class="user-name">{user.name ?? user.username}</span>
									<span class="user-handle">@{user.username}</span>
									{#if user.bio}
										<p class="user-bio">{user.bio}</p>
									{/if}
								</div>
								<svg class="user-chevron" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
									<polyline points="9 18 15 12 9 6" />
								</svg>
							</li>
						{/each}
					</ul>
				{/if}
			{/if}
		</div>
	</main>

	<aside class="col-right" aria-label="Search tips">
		<div class="tip-panel">
			<p class="tip-label">Search tips</p>
			<ul class="tip-list">
				<li>Search by name or @username to find people</li>
				<li>Use keywords to discover posts</li>
				<li>Leave it empty to see recommendations</li>
			</ul>
		</div>
	</aside>
</div>

<style>
	/* ── Layout ─────────────────────────────────────────── */
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
		padding: 0 2rem;
	}

	.col-right {
		overflow-y: auto;
		padding: 1.5rem 1rem 1.5rem 1.5rem;
	}

	.col-right {
		padding: 1.5rem 1rem 1.5rem 1.5rem;
	}

	/* ── Sticky header ───────────────────────────────────── */
	.explore-header {
		position: sticky;
		top: 0;
		background-color: var(--color-bg);
		z-index: 10;
		padding-bottom: 0;
	}

	.explore-title {
		font-size: 1.0625rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.01em;
		padding: 1.25rem 0 0.875rem;
	}

	/* ── Search input ────────────────────────────────────── */
	.search-wrapper {
		position: relative;
		display: flex;
		align-items: center;
		margin-bottom: 0.75rem;
	}

	.search-icon {
		position: absolute;
		left: 0.75rem;
		width: 1rem;
		height: 1rem;
		color: var(--color-text-muted);
		pointer-events: none;
		flex-shrink: 0;
	}

	.search-input {
		width: 100%;
		background: var(--color-surface-raised);
		border: 1px solid transparent;
		border-radius: 9999px;
		padding: 0.5625rem 2.5rem 0.5625rem 2.375rem;
		font-size: 0.9375rem;
		color: var(--color-text-primary);
		font-family: inherit;
		outline: none;
		transition:
			border-color 0.18s ease,
			box-shadow 0.18s ease;
		/* Remove default search cancel button in WebKit */
		-webkit-appearance: none;
	}

	.search-input::placeholder {
		color: var(--color-text-muted);
		opacity: 0.55;
	}

	.search-input:focus {
		border-color: var(--color-accent);
		box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-accent) 15%, transparent);
	}

	/* Hide native clear button in Chrome/Safari */
	.search-input::-webkit-search-cancel-button {
		display: none;
	}

	.search-clear {
		position: absolute;
		right: 0.625rem;
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-muted);
		padding: 0.25rem;
		display: flex;
		align-items: center;
		border-radius: 50%;
		transition: color 0.15s ease, background-color 0.15s ease;
	}

	.search-clear svg {
		width: 0.875rem;
		height: 0.875rem;
	}

	.search-clear:hover {
		color: var(--color-text-primary);
		background-color: var(--color-border);
	}

	/* ── Tab bar ─────────────────────────────────────────── */
	.tab-bar {
		position: relative;
		display: grid;
		grid-template-columns: 1fr 1fr;
		border-bottom: 1px solid var(--color-border);
	}

	.tab-btn {
		background: none;
		border: none;
		cursor: pointer;
		font-family: inherit;
		font-size: 0.875rem;
		font-weight: 500;
		color: var(--color-text-muted);
		padding: 0.75rem 0;
		text-align: center;
		transition: color 0.18s ease;
		position: relative;
		z-index: 1;
	}

	.tab-btn:hover {
		color: var(--color-text-primary);
	}

	.tab-btn.tab-active {
		color: var(--color-text-primary);
		font-weight: 600;
	}

	.tab-indicator {
		position: absolute;
		bottom: -1px;
		left: 0;
		width: 50%;
		height: 2px;
		background: var(--color-accent);
		border-radius: 1px 1px 0 0;
		transition: transform 0.22s cubic-bezier(0.4, 0, 0.2, 1);
	}

	/* ── Content area ────────────────────────────────────── */
	.content-area {
		padding-top: 0.25rem;
	}

	/* ── Infinite scroll ────────────────────────────────── */
	.explore-sentinel {
		height: 1px;
	}

	.loading-more {
		padding: 1.5rem;
		text-align: center;
		font-size: 0.8125rem;
		color: var(--color-text-muted);
	}

	/* ── Post results ────────────────────────────────────── */
	.results-list {
		display: flex;
		flex-direction: column;
	}

	.result-item {
		animation: fadeSlideUp 0.22s ease-out both;
	}

	/* ── User list ───────────────────────────────────────── */
	.user-list {
		list-style: none;
		padding: 0;
		margin: 0;
		display: flex;
		flex-direction: column;
	}

	.user-card {
		display: flex;
		align-items: center;
		gap: 0.875rem;
		padding: 0.875rem 0.625rem;
		border-radius: 0.625rem;
		cursor: pointer;
		transition: background-color 0.15s ease;
		animation: fadeSlideUp 0.22s ease-out both;
		outline: none;
	}

	.user-card:hover,
	.user-card:focus-visible {
		background-color: var(--color-surface-raised);
	}

	.user-card:focus-visible {
		box-shadow: 0 0 0 2px var(--color-accent);
	}

	.user-avatar {
		width: 2.75rem;
		height: 2.75rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.user-avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.user-avatar-initial {
		font-size: 1rem;
		font-weight: 600;
		color: var(--color-text-muted);
		user-select: none;
	}

	.user-info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 0.125rem;
	}

	.user-name {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text-primary);
		line-height: 1.25;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.user-handle {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		line-height: 1.25;
	}

	.user-bio {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		line-height: 1.4;
		margin: 0.125rem 0 0;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.user-chevron {
		width: 1rem;
		height: 1rem;
		color: var(--color-text-muted);
		flex-shrink: 0;
		opacity: 0;
		transition: opacity 0.15s ease;
	}

	.user-card:hover .user-chevron,
	.user-card:focus-visible .user-chevron {
		opacity: 1;
	}

	/* ── User skeletons ──────────────────────────────────── */
	.user-skeleton-list {
		display: flex;
		flex-direction: column;
		gap: 0;
		padding-top: 0.25rem;
	}

	.user-skeleton {
		display: flex;
		align-items: center;
		gap: 0.875rem;
		padding: 0.875rem 0.625rem;
	}

	.user-skeleton-avatar {
		width: 2.75rem;
		height: 2.75rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		flex-shrink: 0;
		animation: shimmer 1.4s ease-in-out infinite;
	}

	.user-skeleton-meta {
		flex: 1;
		display: flex;
		flex-direction: column;
		gap: 0.375rem;
	}

	.user-skeleton-line {
		border-radius: 0.25rem;
		background: var(--color-surface-raised);
		animation: shimmer 1.4s ease-in-out infinite;
	}

	.user-skeleton-name {
		height: 0.875rem;
		width: 45%;
	}

	.user-skeleton-username {
		height: 0.75rem;
		width: 30%;
		animation-delay: 0.1s;
	}

	/* ── Empty state ─────────────────────────────────────── */
	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
		padding: 4rem 1rem;
		text-align: center;
	}

	.empty-icon {
		width: 2.5rem;
		height: 2.5rem;
		color: var(--color-text-muted);
		opacity: 0.4;
		margin-bottom: 0.25rem;
	}

	.empty-title {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text-primary);
		margin: 0;
	}

	.empty-body {
		font-size: 0.875rem;
		color: var(--color-text-muted);
		margin: 0;
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
		font-family: inherit;
		cursor: pointer;
		transition: background-color 0.15s ease, transform 0.1s ease;
	}

	.retry-btn:hover {
		background-color: var(--color-surface-raised);
	}

	.retry-btn:active {
		transform: scale(0.97);
	}

	/* ── Right sidebar ───────────────────────────────────── */
	.tip-panel {
		padding: 1rem;
		border-radius: 0.75rem;
		background: var(--color-surface-raised);
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
		margin-top: 1.5rem;
	}

	.tip-label {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--color-text-primary);
		margin: 0;
	}

	.tip-list {
		list-style: none;
		padding: 0;
		margin: 0;
		display: flex;
		flex-direction: column;
		gap: 0.375rem;
	}

	.tip-list li {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		line-height: 1.5;
		padding-left: 0.75rem;
		position: relative;
	}

	.tip-list li::before {
		content: '–';
		position: absolute;
		left: 0;
		color: var(--color-text-muted);
		opacity: 0.5;
	}

	/* ── Animations ──────────────────────────────────────── */
	@keyframes fadeSlideUp {
		from {
			opacity: 0;
			transform: translateY(8px);
		}
		to {
			opacity: 1;
			transform: translateY(0);
		}
	}

	@keyframes shimmer {
		0%, 100% {
			opacity: 0.5;
		}
		50% {
			opacity: 1;
		}
	}

	/* ── Responsive ──────────────────────────────────────── */
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
