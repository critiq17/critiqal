<script lang="ts">
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { isTelegramMiniApp } from '$lib/telegram';
	import MobileLayout from '$lib/components/mobile/MobileLayout.svelte';
	import { onMount } from 'svelte';
	import { onNavigate, goto } from '$app/navigation';
	import { reducedMotion } from '$lib/ui/reducedMotion.svelte';
	import { registerUnauthorizedHandler } from '$lib/api/client';
	import { i18n } from '$lib/i18n';
	import LanguageOverlay from '$lib/i18n/LanguageOverlay.svelte';

	// Keep <html lang> in sync with the active locale — screen readers,
	// hyphenation, and CJK fallback chains all depend on it.
	$effect(() => {
		if (typeof document !== 'undefined') {
			document.documentElement.lang = i18n.locale;
		}
	});

	onNavigate((navigation) => {
		if (reducedMotion.value || !document.startViewTransition) return;
		return new Promise((resolve) => {
			const transition = document.startViewTransition(async () => {
				resolve();
				try {
					await navigation.complete;
				} catch {
					// Navigation was aborted or redirected. Swallow it so the
					// transition callback resolves cleanly instead of leaving a
					// frozen ::view-transition snapshot over the page.
				}
			});
			// An overlapping/skipped transition rejects .finished; ignore it so
			// the overlay always tears down and never blocks interaction.
			transition.finished.catch(() => {});
			// Hard safety net: never let a stuck transition wedge navigation.
			setTimeout(resolve, 400);
		});
	});

	interface Props {
		children: import('svelte').Snippet;
	}

	let { children }: Props = $props();

	let isMobile = $state(false);

	onMount(() => {
		isMobile = isTelegramMiniApp();
		authStore.init();

		registerUnauthorizedHandler(() => {
			// Don't fire during init() — that path already handles auth failure itself.
			if (authStore.isInitializing) return;

			authStore.logout();

			// In TMA the MobileLayout will show MobileAuthScreen automatically
			// once user becomes null. goto('/login') in a Telegram WebView causes
			// navigation issues, so we only do it on regular web.
			if (!isMobile) goto('/login');
		});
	});
</script>


{#if isMobile}
	<MobileLayout />
{:else}
	{@render children()}
{/if}

<LanguageOverlay />

<style>
	:global(*),
	:global(*::before),
	:global(*::after) {
		box-sizing: border-box;
		margin: 0;
		padding: 0;
	}

	:global(:root) {
		--color-bg: #0c0c0c;
		--color-surface: #141414;
		--color-surface-raised: #1e1e1e;
		--color-border: #242424;
		--color-text-primary: #eaeaea;
		--color-text-secondary: #8c8c8c;
		--color-text-muted: #575757;
		--color-accent: #e05252;
		--color-skeleton: #1e1e1e;

		/* Theme-aware semantic tokens. These resolve per-theme via
		   [data-theme="light"] overrides below — every mobile component should
		   prefer these over hardcoded rgba(255,255,255,X) so the UI tracks the
		   Telegram colorScheme without per-file work. */
		--surface-tint-strong: rgba(255, 255, 255, 0.18);
		--surface-tint-medium: rgba(255, 255, 255, 0.10);
		--surface-tint-soft: rgba(255, 255, 255, 0.06);
		--surface-tint-subtle: rgba(255, 255, 255, 0.04);
		--surface-tint-faint: rgba(255, 255, 255, 0.025);

		--divider-strong: rgba(255, 255, 255, 0.10);
		--divider-soft: rgba(255, 255, 255, 0.06);
		--divider-faint: rgba(255, 255, 255, 0.04);

		--text-strong: rgba(255, 255, 255, 0.82);
		--text-secondary-2: rgba(255, 255, 255, 0.7);
		--text-tertiary: rgba(255, 255, 255, 0.5);
		--text-quaternary: rgba(255, 255, 255, 0.4);
		--text-faint: rgba(255, 255, 255, 0.3);
		--text-ghost: rgba(255, 255, 255, 0.18);

		--scrim-soft: rgba(0, 0, 0, 0.4);
		--scrim-medium: rgba(0, 0, 0, 0.55);
		--scrim-strong: rgba(0, 0, 0, 0.7);

		--radius-sm: 8px;
		--radius-md: 12px;
		--radius-lg: 16px;
		--radius-xl: 20px;
		--radius-full: 9999px;
		--shadow-sm: 0 1px 3px rgba(0,0,0,0.3);
		--shadow-md: 0 4px 16px rgba(0,0,0,0.4);
		--shadow-lg: 0 8px 32px rgba(0,0,0,0.5);
		--spacing-xs: 4px;
		--spacing-sm: 8px;
		--spacing-md: 16px;
		--spacing-lg: 24px;
		--spacing-xl: 32px;
		--transition-fast: 150ms ease;
		--transition-base: 250ms ease;
		--transition-slow: 400ms ease;

		/* Glass surface system — medium intensity, Telegram-iOS style.
		   One source of truth: surfaces opt in with class="glass". */
		--glass-blur: 24px;
		--glass-saturate: 180%;
		--glass-bg: rgba(20, 20, 20, 0.78);
		--glass-bg-strong: rgba(20, 20, 20, 0.88);
		--glass-bg-soft: rgba(20, 20, 20, 0.5);
		--glass-border: rgba(255, 255, 255, 0.08);
		--glass-highlight: rgba(255, 255, 255, 0.1);
		--glass-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);

		font-family:
			'Inter',
			-apple-system,
			BlinkMacSystemFont,
			'Segoe UI',
			system-ui,
			sans-serif;
		font-feature-settings: 'rlig' 1, 'calt' 1;
		font-size: 16px;
		line-height: 1.5;
		color-scheme: dark;
	}

	/* Light theme — applied only inside the Telegram Mini App when the user's
	   Telegram colorScheme is 'light'. The data-theme attribute is toggled in
	   telegram.ts; the desktop site is unaffected because it never sets it. */
	:global([data-theme='light']) {
		--color-bg: #ffffff;
		--color-surface: #f4f4f5;
		--color-surface-raised: #ffffff;
		--color-border: #e5e7eb;
		--color-text-primary: #0f1115;
		--color-text-secondary: #5a6273;
		--color-text-muted: #8a90a0;
		--color-accent: #d23a3a;
		--color-skeleton: #ececef;

		--surface-tint-strong: rgba(0, 0, 0, 0.12);
		--surface-tint-medium: rgba(0, 0, 0, 0.07);
		--surface-tint-soft: rgba(0, 0, 0, 0.045);
		--surface-tint-subtle: rgba(0, 0, 0, 0.03);
		--surface-tint-faint: rgba(0, 0, 0, 0.02);

		--divider-strong: rgba(0, 0, 0, 0.10);
		--divider-soft: rgba(0, 0, 0, 0.06);
		--divider-faint: rgba(0, 0, 0, 0.04);

		--text-strong: rgba(15, 17, 21, 0.92);
		--text-secondary-2: rgba(15, 17, 21, 0.72);
		--text-tertiary: rgba(15, 17, 21, 0.56);
		--text-quaternary: rgba(15, 17, 21, 0.44);
		--text-faint: rgba(15, 17, 21, 0.32);
		--text-ghost: rgba(15, 17, 21, 0.18);

		--scrim-soft: rgba(0, 0, 0, 0.28);
		--scrim-medium: rgba(0, 0, 0, 0.38);
		--scrim-strong: rgba(0, 0, 0, 0.5);

		--shadow-sm: 0 1px 3px rgba(15, 17, 21, 0.08);
		--shadow-md: 0 4px 16px rgba(15, 17, 21, 0.10);
		--shadow-lg: 0 8px 32px rgba(15, 17, 21, 0.14);

		--glass-bg: rgba(255, 255, 255, 0.82);
		--glass-bg-strong: rgba(255, 255, 255, 0.92);
		--glass-bg-soft: rgba(255, 255, 255, 0.6);
		--glass-border: rgba(15, 17, 21, 0.08);
		--glass-highlight: rgba(255, 255, 255, 0.7);
		--glass-shadow: 0 8px 32px rgba(15, 17, 21, 0.10);

		color-scheme: light;
	}

	:global(body) {
		background-color: var(--color-bg);
		color: var(--color-text-primary);
		min-height: 100vh;
		-webkit-font-smoothing: antialiased;
		-moz-osx-font-smoothing: grayscale;
	}

	:global(a) {
		color: inherit;
	}

	:global(button) {
		font-family: inherit;
	}

	/* Reusable glass surface. The inset highlight is the cheap "liquid"
	   top-edge light cue; blur/saturate is GPU work so it lives only on
	   small, non-scrolling surfaces (menus, sheets, buttons). */
	:global(.glass) {
		background: var(--glass-bg);
		backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
		border: 1px solid var(--glass-border);
		box-shadow: var(--glass-shadow), inset 0 1px 0 var(--glass-highlight);
	}

	:global(.glass-strong) {
		background: var(--glass-bg-strong);
	}

	/* More transparent / liquid — for floating surfaces (the menu) where you
	   want the content behind to glow through. Slightly more blur keeps it
	   readable despite the lower opacity. */
	:global(.glass-soft) {
		background: var(--glass-bg-soft);
		backdrop-filter: blur(calc(var(--glass-blur) + 8px)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(calc(var(--glass-blur) + 8px)) saturate(var(--glass-saturate));
	}

	/* No backdrop-filter support (or disabled for perf) → solid fallback
	   so text contrast never breaks. */
	@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
		:global(.glass),
		:global(.glass-strong),
		:global(.glass-soft) {
			background: var(--color-surface);
		}
	}

	:global(img) {
		display: block;
		max-width: 100%;
	}
</style>
