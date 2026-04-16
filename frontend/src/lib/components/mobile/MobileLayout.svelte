<script lang="ts">
	import { onMount } from 'svelte';
	import { isTelegramMiniApp, initTelegram, getTelegramWebApp } from '$lib/telegram';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { activeTab } from '$lib/stores/mobile-tab.store';
	import { profileNav } from '$lib/stores/profile-nav.store';
	import BottomNav from './BottomNav.svelte';
	import MobileAuthScreen from './MobileAuthScreen.svelte';
	import MobileFeed from './MobileFeed.svelte';
	import MobileExplore from './MobileExplore.svelte';
	import MobileProfile from './MobileProfile.svelte';
	import UserProfileOverlay from './UserProfileOverlay.svelte';

	let colorScheme = $state<'light' | 'dark' | null>(null);
	let currentTab = $state('feed');
	let viewedUsername = $state<string | null>(null);

	profileNav.subscribe((u) => { viewedUsername = u; });

	const unsubscribe = activeTab.subscribe((tab) => {
		currentTab = tab;
	});

	onMount(() => {
		if (isTelegramMiniApp()) {
			initTelegram();
			const tg = getTelegramWebApp();
			colorScheme = tg?.colorScheme ?? null;
		}
		return unsubscribe;
	});
</script>

<svelte:head>
	<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no, viewport-fit=cover" />
</svelte:head>

<div
	class="mobile-layout"
	class:dark={colorScheme === 'dark'}
	class:light={colorScheme === 'light'}
>
	{#if authStore.isLoading}
		<div class="loading-screen"></div>
	{:else if !authStore.isAuthenticated}
		<MobileAuthScreen />
	{:else}
		<div class="mobile-content">
			<!-- All three tabs stay mounted — only visibility toggles, no DOM destroy -->
			<div class="tab-panel" class:active={currentTab === 'feed'}>
				<MobileFeed />
			</div>
			<div class="tab-panel" class:active={currentTab === 'explore'}>
				<MobileExplore isActive={currentTab === 'explore'} />
			</div>
			<div class="tab-panel" class:active={currentTab === 'profile'}>
				<MobileProfile />
			</div>
		</div>
		<BottomNav />
	{/if}
</div>

{#if viewedUsername}
	<UserProfileOverlay username={viewedUsername} />
{/if}

<style>
	:global(:root) {
		--bottom-nav-height: 72px;
		--safe-bottom: env(safe-area-inset-bottom, 0px);
		--safe-top: env(safe-area-inset-top, 0px);
		--content-bottom-padding: calc(var(--bottom-nav-height) + var(--safe-bottom) + 16px);
	}

	.loading-screen {
		height: 100dvh;
		background: var(--tg-bg, var(--color-bg, #0a0a0a));
	}

	:global(body) {
		height: var(--tg-viewport-height, 100dvh);
		overflow: hidden;
	}

	.mobile-layout {
		height: var(--tg-viewport-height, 100dvh);
		min-height: var(--tg-viewport-height, 100dvh);
		overflow: hidden;
		position: relative;
		background: var(--tg-bg, var(--color-bg, #0f0f0f));
		color: var(--tg-text, var(--color-text-primary, #f0f0f0));
	}

	.mobile-content {
		height: 100%;
		overflow: hidden;
		position: relative;
	}

	.tab-panel {
		display: none;
		height: 100%;
		overflow: hidden;
	}

	.tab-panel.active {
		display: block;
	}

	:global(.mobile-scroll-container) {
		height: 100dvh;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
		/* In fullscreen mode --tg-content-top = transparent Telegram header height.
		   Falls back to Telegram SDK CSS var, then device safe area, then 0. */
		padding-top: var(--tg-content-top, var(--tg-content-safe-area-inset-top, env(safe-area-inset-top, 0px)));
		padding-bottom: var(--content-bottom-padding);
	}
</style>
