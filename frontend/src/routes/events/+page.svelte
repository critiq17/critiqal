<script lang="ts">
	import { onMount } from 'svelte';
	import EventCard from '$lib/components/events/EventCard.svelte';
	import Skeleton from '$lib/ui/Skeleton.svelte';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';
	import { eventService } from '$lib/services/event.service';
	import type { CommunityEvent } from '$lib/types/event';

	// ── Tab state ─────────────────────────────────────────────────────────────

	let activeTab = $state<'upcoming' | 'mine'>('upcoming');

	// ── Upcoming data ─────────────────────────────────────────────────────────

	let upcomingEvents = $state<CommunityEvent[]>([]);
	let upcomingLoading = $state(true);
	let upcomingError = $state<string | null>(null);

	// ── Mine data ─────────────────────────────────────────────────────────────

	let mineEvents = $state<CommunityEvent[]>([]);
	let mineLoading = $state(false);
	let mineError = $state<string | null>(null);
	let mineLoaded = $state(false);

	// ── Derived: active loading/error ─────────────────────────────────────────

	const loading = $derived(activeTab === 'upcoming' ? upcomingLoading : mineLoading);
	const error = $derived(activeTab === 'upcoming' ? upcomingError : mineError);

	// ── Month grouping ────────────────────────────────────────────────────────

	interface MonthGroup {
		month: string;
		events: CommunityEvent[];
	}

	function groupByMonth(events: CommunityEvent[]): MonthGroup[] {
		const groups: MonthGroup[] = [];
		for (const event of events) {
			const month = new Date(event.startsAt).toLocaleString(undefined, {
				month: 'long',
				year: 'numeric'
			});
			const last = groups[groups.length - 1];
			if (!last || last.month !== month) {
				groups.push({ month, events: [event] });
			} else {
				last.events.push(event);
			}
		}
		return groups;
	}

	const visibleGroups = $derived(
		groupByMonth(activeTab === 'upcoming' ? upcomingEvents : mineEvents)
	);

	// ── Load on mount ─────────────────────────────────────────────────────────

	onMount(async () => {
		try {
			const page = await eventService.getUpcoming(0, 40);
			upcomingEvents = page.content;
		} catch (e) {
			upcomingError = e instanceof Error ? e.message : 'Failed to load events';
		} finally {
			upcomingLoading = false;
		}
	});

	// ── Tab switching ─────────────────────────────────────────────────────────

	async function switchTab(tab: 'upcoming' | 'mine'): Promise<void> {
		activeTab = tab;
		if (tab === 'mine' && !mineLoaded) {
			mineLoading = true;
			mineError = null;
			try {
				const page = await eventService.getMine(0, 40);
				mineEvents = page.content;
				mineLoaded = true;
			} catch (e) {
				mineError = e instanceof Error ? e.message : 'Failed to load your events';
			} finally {
				mineLoading = false;
			}
		}
	}
</script>

<svelte:head>
	<title>Events — Critiqal</title>
</svelte:head>

<div class="page-layout">
	<div class="col-left">
		<LeftSidebar />
	</div>

	<main class="col-center">
		<header class="feed-header">
			<h1>Events</h1>
		</header>

		<div class="tabs-wrap" role="tablist" aria-label="Events view">
			<button
				type="button"
				role="tab"
				class="tab"
				class:tab-active={activeTab === 'upcoming'}
				aria-selected={activeTab === 'upcoming'}
				onclick={() => switchTab('upcoming')}
			>Upcoming</button>
			<button
				type="button"
				role="tab"
				class="tab"
				class:tab-active={activeTab === 'mine'}
				aria-selected={activeTab === 'mine'}
				onclick={() => switchTab('mine')}
			>Mine</button>
		</div>

		{#if loading}
			{#each Array(3) as _}
				<Skeleton height="160px" radius="22px" />
			{/each}
		{:else if error}
			<p class="muted">{error}</p>
		{:else if visibleGroups.length === 0}
			<p class="muted">{activeTab === 'mine' ? 'You have no events yet.' : 'No upcoming events.'}</p>
		{:else}
			{#each visibleGroups as group (group.month)}
				<div class="month-divider">
					<span class="month-label">{group.month}</span>
				</div>
				{#each group.events as event (event.id)}
					<EventCard {event} />
				{/each}
			{/each}
		{/if}
	</main>

	<aside class="col-right" aria-hidden="true"></aside>
</div>

<style>
	.page-layout {
		height: 100vh;
		overflow: hidden;
	}

	.col-left {
		position: fixed;
		right: calc(50% + 21rem);
		top: 0;
		bottom: 0;
		width: 16rem;
		overflow-y: auto;
		padding: 0 1.5rem 0 1rem;
		z-index: 20;
	}

	.col-center {
		height: 100vh;
		max-width: 42rem;
		margin: 0 auto;
		overflow-y: auto;
		padding: 0 2rem 6rem;
		scrollbar-width: none;
	}

	.col-center::-webkit-scrollbar { display: none; }

	.col-right { display: none; }

	.feed-header {
		padding: 1.5rem 0 0.75rem;
		position: sticky;
		top: 0;
		z-index: 10;
		background: linear-gradient(
			to bottom,
			var(--color-bg) 0%,
			rgba(12, 12, 12, 0.9) 50%,
			rgba(12, 12, 12, 0) 100%
		);
		backdrop-filter: blur(16px) saturate(140%);
		-webkit-backdrop-filter: blur(16px) saturate(140%);
		-webkit-mask-image: linear-gradient(to bottom, #000 0%, #000 65%, transparent 100%);
		mask-image: linear-gradient(to bottom, #000 0%, #000 65%, transparent 100%);
	}

	h1 {
		font-size: 1rem;
		font-weight: 600;
		color: var(--color-text-primary);
		letter-spacing: -0.015em;
		margin: 0;
	}

	.tabs-wrap {
		display: flex;
		gap: 0;
		margin-bottom: 16px;
		border-bottom: 1px solid var(--glass-border, rgba(255,255,255,0.07));
		padding-bottom: 0;
	}

	.tab {
		background: none;
		border: none;
		border-bottom: 2px solid transparent;
		padding: 6px 16px 8px;
		font-size: 0.875rem;
		font-weight: 500;
		color: var(--color-text-muted);
		cursor: pointer;
		font-family: inherit;
		transition: color 0.15s ease, border-color 0.15s ease;
		margin-bottom: -1px;
	}

	.tab:hover:not(.tab-active) {
		color: var(--color-text-secondary);
	}

	.tab-active {
		color: var(--color-text-primary);
		border-bottom-color: var(--color-text-primary);
		font-weight: 600;
	}

	.month-divider {
		padding: 20px 4px 8px;
		pointer-events: none;
	}

	.month-label {
		font-size: 0.72rem;
		font-weight: 600;
		color: var(--color-text-muted);
		letter-spacing: 0.06em;
		text-transform: uppercase;
	}

	.muted {
		color: var(--color-text-secondary);
		text-align: center;
		padding: 40px 0;
		font-size: 0.9rem;
	}

	@media (max-width: 900px) {
		.col-left { width: 4.5rem; padding: 0 0.5rem; }
	}

	@media (max-width: 640px) {
		.col-left { display: none; }
		.col-center { padding: 0 1rem 4rem; }
	}
</style>
