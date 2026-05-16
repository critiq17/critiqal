<script lang="ts">
	import { untrack } from 'svelte';
	import { goto } from '$app/navigation';
	import { UseSearch } from '$lib/features/explore/useSearch.svelte';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';
	import ExplorePostsTab from '$lib/components/explore/ExplorePostsTab.svelte';
	import ExplorePeopleTab from '$lib/components/explore/ExplorePeopleTab.svelte';

	const DEBOUNCE_MS = 300;

	const search = new UseSearch();

	let query = $state('');
	let activeTab = $state<'posts' | 'people'>('posts');
	let loadedQuery: string | null = null;
	let loadedTab: 'posts' | 'people' | null = null;

	const headerTitle = $derived(query.trim() === '' ? 'Explore' : 'Results');

	let debounceTimer: ReturnType<typeof setTimeout> | undefined;

	function scheduleOrFetch(q: string, tab: 'posts' | 'people'): void {
		const alreadyLoaded = untrack(() => loadedQuery === q && loadedTab === tab);
		if (alreadyLoaded) return;
		const queryChanged = untrack(() => q !== loadedQuery);
		if (queryChanged) {
			clearTimeout(debounceTimer);
			debounceTimer = setTimeout(() => {
				search.fetchResults(q, tab);
				loadedQuery = q;
				loadedTab = tab;
			}, DEBOUNCE_MS);
		} else {
			search.fetchResults(q, tab);
			loadedQuery = q;
			loadedTab = tab;
		}
	}

	$effect(() => {
		scheduleOrFetch(query, activeTab);
	});

	function handleAuthorClick(username: string): void {
		goto(`/${username}`);
	}
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
					<button class="search-clear" onclick={() => { query = ''; }} aria-label="Clear search">
						<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
							<line x1="18" y1="6" x2="6" y2="18" />
							<line x1="6" y1="6" x2="18" y2="18" />
						</svg>
					</button>
				{/if}
			</div>

			<div class="tab-bar" role="tablist" aria-label="Content type">
				<button
					class="tab-btn"
					class:tab-active={activeTab === 'posts'}
					role="tab"
					aria-selected={activeTab === 'posts'}
					onclick={() => { activeTab = 'posts'; }}
				>
					Posts
				</button>
				<button
					class="tab-btn"
					class:tab-active={activeTab === 'people'}
					role="tab"
					aria-selected={activeTab === 'people'}
					onclick={() => { activeTab = 'people'; }}
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

		<div class="content-area" role="tabpanel" aria-label={activeTab === 'posts' ? 'Posts results' : 'People results'}>
			{#if activeTab === 'posts'}
				<ExplorePostsTab
					{search}
					{query}
					onRetry={() => search.fetchResults(query, activeTab)}
					onAuthorClick={handleAuthorClick}
					onPostDeleted={(id) => search.removePost(id)}
				/>
			{:else}
				<ExplorePeopleTab
					{search}
					onRetry={() => search.fetchResults(query, activeTab)}
				/>
			{/if}
		</div>
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
		padding: 0 2rem;
		scrollbar-width: none;
		-ms-overflow-style: none;
	}

	.col-center::-webkit-scrollbar {
		display: none;
	}

	.col-right {
		display: none;
	}

	.explore-header {
		position: sticky;
		top: 0;
		background-color: rgba(12, 12, 12, 0.85);
		backdrop-filter: blur(12px);
		-webkit-backdrop-filter: blur(12px);
		z-index: 10;
		padding-bottom: 0;
	}

	.explore-title {
		font-size: 1rem;
		font-weight: 600;
		color: var(--color-text-primary);
		letter-spacing: -0.015em;
		padding: 1.25rem 0 0.875rem;
	}

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
		background: var(--color-surface);
		border: 1px solid var(--color-border);
		border-radius: 9999px;
		padding: 0.5625rem 2.5rem 0.5625rem 2.375rem;
		font-size: 0.9375rem;
		color: var(--color-text-primary);
		font-family: inherit;
		outline: none;
		transition: border-color 0.18s ease, box-shadow 0.18s ease;
		-webkit-appearance: none;
	}

	.search-input::placeholder {
		color: var(--color-text-muted);
		opacity: 0.7;
	}

	.search-input:focus {
		border-color: rgba(255, 255, 255, 0.15);
		background: var(--color-surface-raised);
	}

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

	.tab-bar {
		position: relative;
		display: grid;
		grid-template-columns: 1fr 1fr;
		border-bottom: 1px solid rgba(255, 255, 255, 0.055);
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

	.content-area {
		padding-top: 0.25rem;
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
			padding: 0 1rem;
		}
	}
</style>
