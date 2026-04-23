<script lang="ts">
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { isTelegramMiniApp } from '$lib/telegram';
	import MobileLayout from '$lib/components/mobile/MobileLayout.svelte';
	import { onMount } from 'svelte';

	interface Props {
		children: import('svelte').Snippet;
	}

	let { children }: Props = $props();

	let isMobile = $state(false);

	onMount(() => {
		isMobile = isTelegramMiniApp();
		authStore.init();
	});
</script>

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
