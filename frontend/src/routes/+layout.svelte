<script lang="ts">
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { isTelegramMiniApp } from '$lib/telegram';
	import MobileLayout from '$lib/components/mobile/MobileLayout.svelte';
	import { onMount } from 'svelte';
	import { onNavigate, goto } from '$app/navigation';
	import { reducedMotion } from '$lib/ui/reducedMotion.svelte';
	import { registerUnauthorizedHandler } from '$lib/api/client';

	onNavigate((navigation) => {
		if (reducedMotion.value || !document.startViewTransition) return;
		return new Promise((resolve) => {
			document.startViewTransition(async () => {
				resolve();
				await navigation.complete;
			});
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

<svelte:head>
	<link rel="preload" as="image" href="/assets/reactions/GIGACHAD.png" />
	<link rel="preload" as="image" href="/assets/reactions/THEROCK.png" />
</svelte:head>

{#if isMobile}
	<MobileLayout />
{:else}
	{@render children()}
{/if}

<style>
	:global(*),
	:global(*::before),
	:global(*::after) {
		box-sizing: border-box;
		margin: 0;
		padding: 0;
	}

	:global(:root) {
		--color-bg: #0a0a0a;
		--color-surface: #111111;
		--color-surface-raised: #1a1a1a;
		--color-border: #232323;
		--color-text-primary: #f0f0f0;
		--color-text-muted: #666666;
		--color-accent: #e05252;
		--color-skeleton: #1e1e1e;

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

		font-family:
			-apple-system,
			BlinkMacSystemFont,
			'Segoe UI',
			system-ui,
			sans-serif;
		font-size: 16px;
		line-height: 1.5;
		color-scheme: dark;
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

	:global(img) {
		display: block;
		max-width: 100%;
	}
</style>
