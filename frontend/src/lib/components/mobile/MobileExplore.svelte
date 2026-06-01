<script lang="ts">
	import { untrack } from 'svelte';
	import { fly } from 'svelte/transition';
	import { cubicOut } from 'svelte/easing';
	import { mobileExploreStore } from '$lib/stores/mobile-explore.store.svelte';
	import { UseSearch } from '$lib/features/explore/useSearch.svelte';
	import type { ExploreTab } from '$lib/features/explore/useSearch.svelte';
	import { openProfile } from '$lib/stores/profile-nav.store.svelte';
	import { openMobileComments } from '$lib/stores/mobile-comments.store';
	import ExploreSearchBar from '$lib/components/explore/ExploreSearchBar.svelte';
	import ExplorePostsTab from '$lib/components/explore/ExplorePostsTab.svelte';
	import ExplorePeopleTab from '$lib/components/explore/ExplorePeopleTab.svelte';
	import { t } from '$lib/i18n';

	const TAB_ORDER: Record<ExploreTab, number> = { posts: 0, people: 1 };

	interface Props {
		isActive: boolean;
	}

	let { isActive }: Props = $props();

	const search = new UseSearch();

	let query = $state(mobileExploreStore.query);
	let activeTab = $state(mobileExploreStore.tab);
	let inputEl = $state<HTMLInputElement | undefined>(undefined);
	// +1 = new tab is to the right (slide in from right), -1 = from left
	let slideDir = $state(1);

	function handleTabChange(tab: ExploreTab): void {
		if (tab === activeTab) return;
		slideDir = TAB_ORDER[tab] > TAB_ORDER[activeTab] ? 1 : -1;
		activeTab = tab;
	}

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

	// Scroll-driven header collapse: scrolling down past the top frosts the
	// bar and tucks the tabs away; scrolling up (or returning to the top)
	// brings them back. rAF-throttled with a dead-zone so it never jitters.
	let scrollEl = $state<HTMLElement | null>(null);
	let collapsed = $state(false);
	let lastScrollY = 0;
	let scrollTicking = false;

	const TOP_ZONE = 24; // always expanded within this many px of the top
	const DEADZONE = 6; // ignore sub-pixel / jittery deltas

	// The fixed glass header reports its height so the scroller pads its top by
	// exactly that much — content stays glued to the bar through the collapse.
	let headerHeight = $state(96);

	function onScroll(): void {
		if (scrollTicking) return;
		scrollTicking = true;
		requestAnimationFrame(() => {
			const y = scrollEl?.scrollTop ?? 0;
			const delta = y - lastScrollY;
			if (y <= TOP_ZONE) {
				collapsed = false;
			} else if (delta > DEADZONE) {
				collapsed = true;
			} else if (delta < -DEADZONE) {
				collapsed = false;
			}
			lastScrollY = y;
			scrollTicking = false;
		});
	}
</script>

<ExploreSearchBar
	{query}
	{activeTab}
	{collapsed}
	onQueryChange={(q) => { query = q; }}
	onTabChange={handleTabChange}
	onInputBind={(el) => { inputEl = el; }}
	onHeightChange={(h) => { headerHeight = h; }}
/>

<div
	class="explore-container"
	bind:this={scrollEl}
	onscroll={onScroll}
	style:padding-top="{headerHeight}px"
>
	<div
		class="content-area"
		role="tabpanel"
		aria-label={activeTab === 'posts' ? t('feed.tabPosts') : t('feed.tabPeople')}
	>
		{#key activeTab}
			<div
				class="tab-panel"
				in:fly={{ x: slideDir * 48, duration: 260, easing: cubicOut }}
			>
				{#if activeTab === 'posts'}
					<ExplorePostsTab
						{search}
						{query}
						onRetry={() => search.fetchResults(query, activeTab)}
						onAuthorClick={(username) => openProfile(username)}
						onOpenComments={(post) => openMobileComments(post)}
						onPostDeleted={(id) => search.removePost(id)}
					/>
				{:else}
					<ExplorePeopleTab
						{search}
						onRetry={() => search.fetchResults(query, activeTab)}
					/>
				{/if}
			</div>
		{/key}
	</div>
</div>

<style>
	.explore-container {
		height: 100%;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
		/* padding-top is set inline to the fixed header's measured height so the
		   first result clears the glass bar and tracks it through the collapse. */
		padding-bottom: var(--content-bottom-padding, 104px);
		scrollbar-width: none;
		-ms-overflow-style: none;
	}

	.explore-container::-webkit-scrollbar {
		display: none;
	}

	.content-area {
		padding-top: 4px;
		overflow-x: hidden;
	}
</style>
