<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { fly, fade } from 'svelte/transition';
	import { authStore } from '$lib/stores/auth.store.svelte';

	const stravaResult = $page.url.searchParams.get('strava');
	const isSuccess = stravaResult === 'connected';
	const isDenied = stravaResult === 'denied';
	const delay = isSuccess || isDenied ? 2000 : 0;

	onMount(() => {
		setTimeout(() => {
			const username = authStore.user?.username;
			goto(username ? `/${username}` : '/', { replaceState: true });
		}, delay);
	});
</script>

<svelte:head>
	<title>Redirecting… — Critiqal</title>
</svelte:head>

<div class="page">
	<div class="card" in:fly={{ y: -14, duration: 300 }} out:fade={{ duration: 180 }}>
		{#if isSuccess}
			<div class="icon icon-success" aria-hidden="true">
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="22" height="22">
					<polyline points="20 6 9 17 4 12" />
				</svg>
			</div>
			<p class="title">Strava connected!</p>
			<p class="sub">Taking you back to your profile…</p>
		{:else if isDenied}
			<div class="icon icon-denied" aria-hidden="true">
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="20" height="20">
					<line x1="18" y1="6" x2="6" y2="18" />
					<line x1="6" y1="6" x2="18" y2="18" />
				</svg>
			</div>
			<p class="title">Connection cancelled</p>
			<p class="sub">Heading back to your profile…</p>
		{:else}
			<p class="sub">Redirecting…</p>
		{/if}
	</div>
</div>

<style>
	.page {
		height: 100vh;
		display: flex;
		align-items: center;
		justify-content: center;
		background: var(--color-bg);
	}

	.card {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
		text-align: center;
		padding: 2rem 2.5rem;
		background: var(--color-surface-raised);
		border: 1px solid var(--color-border);
		border-radius: 1rem;
	}

	.icon {
		width: 3rem;
		height: 3rem;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 0.375rem;
	}

	.icon-success {
		background: rgba(16, 185, 129, 0.12);
		color: #10b981;
	}

	.icon-denied {
		background: rgba(224, 82, 82, 0.1);
		color: var(--color-accent);
	}

	.title {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.01em;
	}

	.sub {
		font-size: 0.875rem;
		color: var(--color-text-muted);
	}
</style>
