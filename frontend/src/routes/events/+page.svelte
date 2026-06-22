<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import EventCard from '$lib/components/events/EventCard.svelte';
	import Button from '$lib/ui/Button.svelte';
	import Skeleton from '$lib/ui/Skeleton.svelte';
	import { eventService } from '$lib/services/event.service';
	import type { CommunityEvent } from '$lib/types/event';

	let events = $state<CommunityEvent[]>([]);
	let loading = $state(true);
	let canCreate = $state(false);
	let error = $state<string | null>(null);

	onMount(async () => {
		try {
			const [page, perm] = await Promise.all([
				eventService.getUpcoming(),
				eventService.canCreate().catch(() => ({ canCreateEvents: false }))
			]);
			events = page.content;
			canCreate = perm.canCreateEvents;
		} catch (e) {
			error = e instanceof Error ? e.message : 'Failed to load events';
		} finally {
			loading = false;
		}
	});
</script>

<div class="events-page">
	<header class="head">
		<h1>Events</h1>
		{#if canCreate}
			<Button variant="primary" size="sm" onclick={() => goto('/events/new')}>+ Create</Button>
		{/if}
	</header>

	{#if loading}
		{#each Array(3) as _unused}
			<Skeleton height="220px" radius="18px" />
		{/each}
	{:else if error}
		<p class="muted">{error}</p>
	{:else if events.length === 0}
		<p class="muted">No upcoming events yet.{canCreate ? ' Be the first to host one.' : ''}</p>
	{:else}
		{#each events as event (event.id)}
			<EventCard {event} />
		{/each}
	{/if}
</div>

<style>
	.events-page {
		max-width: 42rem;
		margin: 0 auto;
		padding: 16px;
		display: flex;
		flex-direction: column;
		gap: 12px;
	}
	.head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 4px;
	}
	h1 {
		font-size: 1.4rem;
		font-weight: 700;
	}
	.muted {
		color: var(--color-text-secondary);
		text-align: center;
		padding: 40px 0;
	}
</style>
