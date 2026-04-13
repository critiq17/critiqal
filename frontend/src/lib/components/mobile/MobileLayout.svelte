<script lang="ts">
	import { onMount } from 'svelte';
	import type { Snippet } from 'svelte';
	import { isTelegramMiniApp, initTelegram, getTelegramWebApp } from '$lib/telegram';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import BottomNav from './BottomNav.svelte';
	import MobileAuthScreen from './MobileAuthScreen.svelte';

	interface Props {
		children: Snippet;
	}

	let { children }: Props = $props();

	let colorScheme = $state<'light' | 'dark' | null>(null);

	onMount(() => {
		if (isTelegramMiniApp()) {
			initTelegram();
			colorScheme = getTelegramWebApp()?.colorScheme ?? null;
		}
	});
</script>

<svelte:head>
	<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
</svelte:head>

<div
	class="mobile-layout"
	class:dark={colorScheme === 'dark'}
	class:light={colorScheme === 'light'}
>
	{#if !authStore.isAuthenticated && !authStore.isLoading}
		<MobileAuthScreen />
	{:else}
		<div class="mobile-content">
			{@render children()}
		</div>
		<BottomNav />
	{/if}
</div>

<style>
	:global(:root) {
		--bottom-nav-height: 72px;
		--safe-bottom: env(safe-area-inset-bottom, 0px);
		--safe-top: env(safe-area-inset-top, 0px);
		--content-bottom-padding: calc(var(--bottom-nav-height) + var(--safe-bottom) + 16px);
	}

	.mobile-scroll-container {
		height: 100dvh;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
		padding-bottom: var(--content-bottom-padding);
	}

	.mobile-layout {
		height: 100dvh;
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
</style>
