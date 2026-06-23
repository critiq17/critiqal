<script lang="ts">
	import { onMount } from 'svelte';
	import { fly } from 'svelte/transition';
	import { isTelegramMiniApp, initTelegram, getTelegramWebApp, getStartParam } from '$lib/telegram';
	import { parseStartParam, type DeepLinkTarget } from '$lib/deeplink';
	import { postService } from '$lib/services';
	import { openProfile } from '$lib/stores/profile-nav.store.svelte';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { mobileAuth } from '$lib/stores/mobile-auth.store.svelte';
	import { tabStore } from '$lib/stores/mobile-tab.store.svelte';
	import { navStack } from '$lib/stores/nav-stack.store.svelte';
	import { closeCompose, composeStore } from '$lib/stores/compose.store.svelte';
	import { sheetStore } from '$lib/stores/sheet.store.svelte';
	import { mobileFeedStore } from '$lib/stores/mobile-feed.store.svelte';
	import { mobileComments } from '$lib/stores/mobile-comments.store';
	import { mobilePostFocus } from '$lib/stores/mobile-post-focus.store.svelte';
	import type { Post } from '$lib/types';
	import BottomNav from './BottomNav.svelte';
	import MobileAuthScreen from './MobileAuthScreen.svelte';
	import MobileFeed from './MobileFeed.svelte';
	import OverlayHost from './OverlayHost.svelte';

	let colorScheme = $state<'light' | 'dark' | null>(null);

	// True while the on-screen keyboard is up. The fixed BottomNav otherwise
	// floats above the keyboard and covers form submit buttons. We detect via
	// visualViewport (web standard) and Telegram's viewportHeight as fallback.
	let keyboardOpen = $state(false);

	// Lazy tab mounting: each tab is mounted once on first activation and kept mounted after.
	let mountedTabs = $state(new Set<string>(['feed']));

	$effect(() => {
		const tab = tabStore.active;
		if (!mountedTabs.has(tab)) {
			mountedTabs = new Set([...mountedTabs, tab]);
		}
	});

	// Lazy-loaded heavy components — fetched on first need to keep the initial
	// TMA bundle lean (feed-first cold start).
	let MobileExplore = $state<typeof import('./MobileExplore.svelte').default | null>(null);
	let MobileEvents = $state<typeof import('./MobileEvents.svelte').default | null>(null);
	let MobileProfile = $state<typeof import('./MobileProfile.svelte').default | null>(null);
	let MobileCommentsSheet = $state<typeof import('./MobileCommentsSheet.svelte').default | null>(null);
	let MobilePostFocus = $state<typeof import('$lib/components/post/MobilePostFocus.svelte').default | null>(null);
	let MobilePostComposer = $state<typeof import('./MobilePostComposer.svelte').default | null>(null);

	$effect(() => {
		if (mountedTabs.has('explore') && !MobileExplore) {
			import('./MobileExplore.svelte').then((m) => { MobileExplore = m.default; });
		}
	});

	$effect(() => {
		if (mountedTabs.has('events') && !MobileEvents) {
			import('./MobileEvents.svelte').then((m) => { MobileEvents = m.default; });
		}
	});

	$effect(() => {
		if (mountedTabs.has('profile') && !MobileProfile) {
			import('./MobileProfile.svelte').then((m) => { MobileProfile = m.default; });
		}
	});

	$effect(() => {
		if ($mobileComments.open && !MobileCommentsSheet) {
			import('./MobileCommentsSheet.svelte').then((m) => { MobileCommentsSheet = m.default; });
		}
	});

	$effect(() => {
		if (mobilePostFocus.isOpen && !MobilePostFocus) {
			import('$lib/components/post/MobilePostFocus.svelte').then((m) => { MobilePostFocus = m.default; });
		}
	});

	$effect(() => {
		if (composeStore.open && !MobilePostComposer) {
			import('./MobilePostComposer.svelte').then((m) => { MobilePostComposer = m.default; });
		}
	});

	function handlePosted(post: Post): void {
		mobileFeedStore.prependPost(post);
		closeCompose();
		tabStore.active = 'feed';
	}

	// ── Deep link on launch (t.me/<bot>?startapp=<payload>) ─────────────────────
	// A shared post/profile link lands here: once auth resolves we read the
	// start_param once and open the post focus or push the profile overlay.
	let deepLinkHandled = false;

	async function openDeepLink(target: DeepLinkTarget): Promise<void> {
		if (target.kind === 'user') {
			openProfile(target.username);
			return;
		}
		try {
			const post = await postService.getById(target.id);
			mobilePostFocus.open(post);
		} catch {
			// Post removed or unavailable — fail quietly, app opens to the feed.
		}
	}

	$effect(() => {
		if (deepLinkHandled || authStore.isLoading) return;
		const target = parseStartParam(getStartParam());
		if (!target) {
			deepLinkHandled = true;
			return;
		}
		// Profile/post views require a session — wait for login, then open once.
		if (!authStore.isAuthenticated) return;
		deepLinkHandled = true;
		void openDeepLink(target);
	});

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

		// Keyboard detection. Two signals, whichever fires first wins:
		//  - visualViewport.height drops by ~150px+ when the OS keyboard is up
		//  - Telegram's viewportHeight diverges from viewportStableHeight
		const KEYBOARD_THRESHOLD = 150;
		const tg = getTelegramWebApp();

		function updateFromViewport(): void {
			const vv = window.visualViewport;
			if (!vv) return;
			keyboardOpen = window.innerHeight - vv.height > KEYBOARD_THRESHOLD;
		}

		function updateFromTelegram(): void {
			if (!tg) return;
			const stable = tg.viewportStableHeight ?? tg.viewportHeight;
			if (!stable || !tg.viewportHeight) return;
			keyboardOpen = stable - tg.viewportHeight > KEYBOARD_THRESHOLD;
		}

		const vv = window.visualViewport;
		vv?.addEventListener('resize', updateFromViewport);
		updateFromViewport();

		tg?.onEvent?.('viewportChanged', updateFromTelegram);
		updateFromTelegram();

		return () => {
			vv?.removeEventListener('resize', updateFromViewport);
			tg?.offEvent?.('viewportChanged', updateFromTelegram);
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
	{:else}
		<div class="mobile-content" bind:this={contentEl}>
			<!-- Tabs are lazily mounted on first activation, then kept in DOM. -->
			<div class="tab-panel" class:active={tabStore.active === 'feed'}>
				{#if mountedTabs.has('feed')}
					<MobileFeed />
				{/if}
			</div>
			<div class="tab-panel" class:active={tabStore.active === 'explore'}>
				{#if mountedTabs.has('explore') && MobileExplore}
					{#if authStore.isAuthenticated}
						<MobileExplore isActive={tabStore.active === 'explore'} />
					{:else}
						<div class="guest-tab">
							<MobileAuthScreen />
						</div>
					{/if}
				{/if}
			</div>
			<div class="tab-panel" class:active={tabStore.active === 'events'}>
					{#if mountedTabs.has('events') && MobileEvents}
						<MobileEvents />
					{/if}
				</div>
				<div class="tab-panel" class:active={tabStore.active === 'profile'}>
				{#if mountedTabs.has('profile') && MobileProfile}
					{#if authStore.isAuthenticated}
						<MobileProfile />
					{:else}
						<div class="guest-tab">
							<MobileAuthScreen />
						</div>
					{/if}
				{/if}
			</div>
		</div>
		{#if !sheetStore.anyOpen && !keyboardOpen}
			<BottomNav />
		{/if}
	{/if}
</div>

{#if mobileAuth.isOpen && !authStore.isAuthenticated}
	<div class="mobile-auth-overlay" transition:fly={{ y: 40, duration: 280 }}>
		<MobileAuthScreen initialMode={mobileAuth.initialMode} onClose={() => mobileAuth.close()} />
	</div>
{/if}

<OverlayHost />

{#if MobileCommentsSheet}
	<MobileCommentsSheet />
{/if}

{#if MobilePostFocus}
	<MobilePostFocus />
{/if}

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

	/* Tab panels are stacked absolutely so we can crossfade between them.
	   Inactive panels are inert (visibility:hidden disables hit-testing and
	   ARIA, opacity drives the fade). Active tab paints with a tiny lift. */
	.tab-panel {
		position: absolute;
		inset: 0;
		overflow: hidden;
		opacity: 0;
		visibility: hidden;
		transform: translateY(4px);
		transition:
			opacity 0.16s ease,
			transform 0.18s cubic-bezier(0.2, 0.7, 0.2, 1);
	}

	.tab-panel.active {
		opacity: 1;
		visibility: visible;
		transform: translateY(0);
	}

	.guest-tab {
		height: 100%;
		overflow-y: auto;
		-webkit-overflow-scrolling: touch;
	}

	.mobile-auth-overlay {
		position: fixed;
		inset: 0;
		z-index: 1100;
		background: var(--color-bg);
		display: flex;
		flex-direction: column;
	}

	@media (prefers-reduced-motion: reduce) {
		.tab-panel {
			transition: none;
			transform: none;
		}
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
