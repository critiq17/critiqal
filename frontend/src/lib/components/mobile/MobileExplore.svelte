<script lang="ts">
	import { untrack } from 'svelte';
	import { mobileExploreStore } from '$lib/stores/mobile-explore.store.svelte';
	import { UseSearch } from '$lib/features/explore/useSearch.svelte';
	import type { Post } from '$lib/types';
	import ExploreSearchBar from '$lib/components/explore/ExploreSearchBar.svelte';
	import ExplorePostsTab from '$lib/components/explore/ExplorePostsTab.svelte';
	import ExplorePeopleTab from '$lib/components/explore/ExplorePeopleTab.svelte';

	interface Props {
		isActive: boolean;
	}

	let { isActive }: Props = $props();

	const search = new UseSearch();

	let query = $state(mobileExploreStore.query);
	let activeTab = $state(mobileExploreStore.tab);
	let inputEl = $state<HTMLInputElement | undefined>(undefined);

	$effect(() => {
		mobileExploreStore.query = query;
		mobileExploreStore.tab = activeTab;
	});

	let loadedQuery: string | null = null;
	let loadedTab: string = untrack(() => activeTab);

	$effect(() => {
		const q = query;
		const tab = activeTab;

		const alreadyLoaded = untrack(() => loadedQuery === q && loadedTab === tab);
		if (alreadyLoaded) return;

		const queryChanged = untrack(() => q !== loadedQuery);
		if (queryChanged) {
			search.scheduleSearch(q, tab);
		} else {
			search.fetchResults(q, tab);
		}
		loadedQuery = q;
		loadedTab = tab;
	});

	$effect(() => {
		if (isActive) inputEl?.focus();
	});

	function handlePostTap(_post: Post): void {
		// Placeholder for post detail navigation when route is ready
	}
</script>

<div class="explore-container">
	<ExploreSearchBar
		{query}
		{activeTab}
		onQueryChange={(q) => { query = q; }}
		onTabChange={(tab) => { activeTab = tab; }}
		onInputBind={(el) => { inputEl = el; }}
	/>

	<div
		class="content-area"
		role="tabpanel"
		aria-label={activeTab === 'posts' ? 'Posts results' : 'People results'}
	>
		{#if activeTab === 'posts'}
			<ExplorePostsTab
				{search}
				{query}
				onRetry={() => search.fetchResults(query, activeTab)}
				onPostTap={handlePostTap}
			/>
		{:else}
			<ExplorePeopleTab
				{search}
				onRetry={() => search.fetchResults(query, activeTab)}
			/>
		{/if}
	</div>
</div>

<style>
	.explore-container {
		height: 100%;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
		padding-top: var(--tg-content-top, var(--tg-content-safe-area-inset-top, env(safe-area-inset-top, 0px)));
		padding-bottom: var(--content-bottom-padding, 104px);
	}

	.content-area {
		padding-top: 4px;
	}
</style>
