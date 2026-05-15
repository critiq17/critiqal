<script lang="ts">
	import { onMount } from 'svelte';
	import { isTelegramMiniApp, initTelegram, getTelegramWebApp } from '$lib/telegram';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { tabStore } from '$lib/stores/mobile-tab.store.svelte';
	import { profileNavStore } from '$lib/stores/profile-nav.store.svelte';
	import { registerOverlaySwipeListener } from '$lib/overlay-swipe';
	import type { SwipePhase } from '$lib/overlay-swipe';
	import { closeCompose, composeStore } from '$lib/stores/compose.store.svelte';
	import { sheetStore } from '$lib/stores/sheet.store.svelte';
	import { mobileFeedStore } from '$lib/stores/mobile-feed.store.svelte';
	import type { Post } from '$lib/types';
	import { settingsNavStore } from '$lib/stores/settings-nav.store.svelte';
	import BottomNav from './BottomNav.svelte';
	import MobileAuthScreen from './MobileAuthScreen.svelte';
	import MobileFeed from './MobileFeed.svelte';
	import MobileExplore from './MobileExplore.svelte';
	import MobileProfile from './MobileProfile.svelte';
	import UserProfileOverlay from './UserProfileOverlay.svelte';
	import MobileSettingsOverlay from './MobileSettingsOverlay.svelte';
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

	// Reference to the content div that gets pushed during overlay navigation
	let contentEl = $state<HTMLElement | null>(null);

	// iOS-style push ratio: background shifts left by this fraction of screen
	// while the overlay is on top, then tracks back to 0 as overlay is swiped away.
	const PUSH_RATIO = 0.28;

	function applyContentTransform(x: number, sw: number, transition: string): void {
		if (!contentEl) return;
		// Interpolate: when overlay is at 0 (fully shown) → content at -PUSH.
		//              when overlay is at sw (dismissed)  → content at 0.
		const bgX = PUSH_RATIO * (x / sw - 1) * sw;
		contentEl.style.transition = transition;
		contentEl.style.transform = `translateX(${bgX}px)`;
	}

	function onOverlaySwipe(x: number, sw: number, phase: SwipePhase): void {
		if (phase === 'drag') {
			applyContentTransform(x, sw, 'none');
		} else if (phase === 'dismiss') {
			// Match overlay's dismiss curve
			contentEl!.style.transition = 'transform 0.24s cubic-bezier(0.4, 0, 1, 1)';
			contentEl!.style.transform = 'translateX(0)';
		} else {
			// Cancel / snap-back: spring back to pushed position, match overlay curve
			if (!contentEl) return;
			const push = sw * PUSH_RATIO;
			contentEl.style.transition = 'transform 0.38s cubic-bezier(0.34, 1.56, 0.64, 1)';
			contentEl.style.transform = `translateX(-${push}px)`;
		}
	}

	// Watch overlay open/close to push/restore background.
	$effect(() => {
		const username = profileNavStore.username;
		const settingsOpen = settingsNavStore.open;
		if (!contentEl) return;
		const sw = window.innerWidth;
		const push = sw * PUSH_RATIO;

		if (username || settingsOpen) {
			contentEl.style.transition = 'transform 0.28s cubic-bezier(0.4, 0, 0.2, 1)';
			contentEl.style.transform = `translateX(-${push}px)`;
		} else {
			contentEl.style.transition = 'transform 0.24s cubic-bezier(0.4, 0, 1, 1)';
			contentEl.style.transform = 'translateX(0)';
		}
	});

	onMount(() => {
		if (isTelegramMiniApp()) {
			initTelegram();
			const tg = getTelegramWebApp();
			colorScheme = tg?.colorScheme ?? null;
		}

		const unregisterSwipe = registerOverlaySwipeListener(onOverlaySwipe);

		return () => {
			unregisterSwipe();
		};
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

{#if profileNavStore.username}
	<UserProfileOverlay username={profileNavStore.username} />
{/if}

{#if settingsNavStore.open}
	<MobileSettingsOverlay />
{/if}

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
