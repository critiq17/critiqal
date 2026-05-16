<script lang="ts">
	import { onMount } from 'svelte';
	import { isTelegramMiniApp, initTelegram, getTelegramWebApp } from '$lib/telegram';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { tabStore } from '$lib/stores/mobile-tab.store.svelte';
	import { navStack } from '$lib/stores/nav-stack.store.svelte';
	import { closeCompose, composeStore } from '$lib/stores/compose.store.svelte';
	import { sheetStore } from '$lib/stores/sheet.store.svelte';
	import { mobileFeedStore } from '$lib/stores/mobile-feed.store.svelte';
	import type { Post } from '$lib/types';
	import BottomNav from './BottomNav.svelte';
	import MobileAuthScreen from './MobileAuthScreen.svelte';
	import MobileFeed from './MobileFeed.svelte';
	import MobileExplore from './MobileExplore.svelte';
	import MobileProfile from './MobileProfile.svelte';
	import OverlayHost from './OverlayHost.svelte';
	import MobileCommentsSheet from './MobileCommentsSheet.svelte';

	let colorScheme = $state<'light' | 'dark' | null>(null);

	// Lazy tab mounting: each tab is mounted once on first activation and kept mounted after.
	let mountedTabs = $state(new Set<string>(['feed']));

	$effect(() => {
		const tab = tabStore.active;
		if (!mountedTabs.has(tab)) {
			mountedTabs = new Set([...mountedTabs, tab]);
		}
	});

	// Composer — owned at layout level so it works from any tab
	let MobilePostComposer = $state<typeof import('./MobilePostComposer.svelte').default | null>(null);

	$effect(() => {
		const open = composeStore.open;
		if (open && !MobilePostComposer) {
			import('./MobilePostComposer.svelte').then((m) => {
				MobilePostComposer = m.default;
			});
		}
	});

	function handlePosted(post: Post): void {
		mobileFeedStore.prependPost(post);
		closeCompose();
		tabStore.active = 'feed';
	}

	// Reference to the content div that gets pushed behind the overlay stack.
	let contentEl = $state<HTMLElement | null>(null);

	// iOS-style depth cue: the app content sits slightly pushed-left while any
	// overlay is on the stack, and springs back when the stack empties.
	const PUSH_RATIO = 0.22;

	$effect(() => {
		const hasOverlay = navStack.depth > 0;
		if (!contentEl) return;
		if (hasOverlay) {
			const push = window.innerWidth * PUSH_RATIO;
			contentEl.style.transition = 'transform 0.28s cubic-bezier(0.32, 0.72, 0, 1)';
			contentEl.style.transform = `translateX(-${push}px)`;
		} else {
			contentEl.style.transition = 'transform 0.26s cubic-bezier(0.32, 0.72, 0, 1)';
			contentEl.style.transform = 'translateX(0)';
		}
	});

	onMount(() => {
		if (isTelegramMiniApp()) {
			initTelegram();
			const tg = getTelegramWebApp();
			colorScheme = tg?.colorScheme ?? null;
		}
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
		<div class="mobile-content" bind:this={contentEl}>
			<!-- Tabs are lazily mounted on first activation, then kept in DOM. -->
			<div class="tab-panel" class:active={tabStore.active === 'feed'}>
				{#if mountedTabs.has('feed')}
					<MobileFeed />
				{/if}
			</div>
			<div class="tab-panel" class:active={tabStore.active === 'explore'}>
				{#if mountedTabs.has('explore')}
					<MobileExplore isActive={tabStore.active === 'explore'} />
				{/if}
			</div>
			<div class="tab-panel" class:active={tabStore.active === 'profile'}>
				{#if mountedTabs.has('profile')}
					<MobileProfile />
				{/if}
			</div>
		</div>
		{#if !sheetStore.anyOpen}
			<BottomNav />
		{/if}
	{/if}
</div>

<OverlayHost />

<MobileCommentsSheet />

{#if MobilePostComposer && composeStore.open}
	<MobilePostComposer
		open={composeStore.open}
		onClose={closeCompose}
		onPosted={handlePosted}
	/>
{/if}

<style>
	:global(:root) {
		--bottom-nav-height: 72px;
		--safe-bottom: env(safe-area-inset-bottom, 0px);
		--safe-top: env(safe-area-inset-top, 0px);
		--content-bottom-padding: calc(var(--bottom-nav-height) + var(--safe-bottom) + 16px);
		/* Vertical space the native Telegram header chrome (Close ✕ / ⋯ menu)
		   occupies in fullscreen. Uses the values Telegram reports, but with a
		   guaranteed floor so headers never collapse under the buttons even
		   when Telegram reports 0 — same proven formula the feed uses. */
		--tg-top-clearance: max(
			var(--tg-content-top, 0px),
			var(--tg-content-safe-area-inset-top, 0px),
			calc(env(safe-area-inset-top, 20px) + 44px)
		);
	}

	.loading-screen {
		height: 100dvh;
		background: var(--tg-bg, var(--color-bg, #0a0a0a));
	}

	:global(html),
	:global(body) {
		height: var(--tg-viewport-height, 100dvh);
		overflow: hidden;
		/* Stop scroll/overscroll from chaining to the Telegram webview,
		   which is what lets a swipe drag the mini-app down or close it. */
		overscroll-behavior: none;
		touch-action: pan-x pan-y;
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
		will-change: transform;
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
