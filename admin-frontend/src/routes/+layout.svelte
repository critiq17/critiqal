<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { goto } from '$app/navigation';
	import { adminAuthStore } from '$lib/stores/admin-auth.store.svelte';
	import type { Snippet } from 'svelte';

	interface Props {
		children: Snippet;
	}

	let { children }: Props = $props();

	const isLoginRoute = $derived($page.url.pathname === '/login');
	let checking = $state(true);

	onMount(async () => {
		const status = await adminAuthStore.load();
		checking = false;
		if (status === 'anon' && !isLoginRoute) {
			void goto('/login');
		}
	});

	async function handleLogout(): Promise<void> {
		await adminAuthStore.logout();
		void goto('/login');
	}
</script>

<svelte:head>
	<title>critiqal admin</title>
</svelte:head>

<div class="admin-root">
	{#if isLoginRoute}
		{@render children()}
	{:else if checking || adminAuthStore.status === 'unknown'}
		<div class="center"><span class="state-text">Loading…</span></div>
	{:else if adminAuthStore.status === 'authed'}
		<header class="admin-header">
			<span class="brand">critiqal <span class="brand-dim">admin</span></span>
			<button class="logout" onclick={handleLogout}>Log out</button>
		</header>
		<main class="admin-main">
			{@render children()}
		</main>
	{:else}
		<div class="center"><span class="state-text">Redirecting…</span></div>
	{/if}
</div>

<style>
	:global(*),
	:global(*::before),
	:global(*::after) {
		box-sizing: border-box;
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
		--radius-sm: 4px;
		--radius-md: 8px;
		--radius-lg: 16px;
		--radius-full: 9999px;
		--glass-bg: rgba(20, 20, 20, 0.72);
		--glass-border: rgba(255, 255, 255, 0.08);
		--glass-blur: 16px;
	}

	:global(html),
	:global(body) {
		margin: 0;
		padding: 0;
		background: var(--color-bg);
		color: var(--color-text-primary);
		font-family:
			-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, sans-serif;
		font-size: 16px;
		line-height: 1.5;
		-webkit-font-smoothing: antialiased;
	}

	.admin-root {
		position: fixed;
		inset: 0;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.admin-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0.875rem 1.5rem;
		border-bottom: 1px solid var(--color-border);
		background: var(--color-surface);
		flex-shrink: 0;
	}

	.brand {
		font-weight: 700;
		font-size: 1rem;
		letter-spacing: 0.01em;
	}

	.brand-dim {
		color: var(--color-text-secondary);
		font-weight: 500;
	}

	.logout {
		background: transparent;
		border: 1px solid var(--color-border);
		color: var(--color-text-secondary);
		padding: 0.375rem 0.875rem;
		border-radius: var(--radius-md);
		cursor: pointer;
		font-size: 0.8125rem;
		font-family: inherit;
		transition: color 0.15s ease, border-color 0.15s ease;
	}

	.logout:hover {
		color: var(--color-text-primary);
		border-color: var(--color-accent);
	}

	.admin-main {
		flex: 1;
		overflow-y: auto;
	}

	.center {
		flex: 1;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.state-text {
		color: var(--color-text-secondary);
		font-size: 0.875rem;
	}
</style>
