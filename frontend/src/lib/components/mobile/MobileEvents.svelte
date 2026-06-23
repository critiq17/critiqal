<script lang="ts">
	import { onMount } from 'svelte';
	import { fly } from 'svelte/transition';
	import { cubicOut } from 'svelte/easing';
	import EventCard from '$lib/components/events/EventCard.svelte';
	import { mobileEventsStore } from '$lib/stores/mobile-events.store.svelte';
	import { navStack } from '$lib/stores/nav-stack.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import type { CommunityEvent } from '$lib/types/event';
	import { t } from '$lib/i18n';

	type EventsTab = 'upcoming' | 'mine';
	const TAB_ORDER: Record<EventsTab, number> = { upcoming: 0, mine: 1 };

	let activeTab = $state<EventsTab>('upcoming');
	let slideDir = $state(1);

	const store = mobileEventsStore;

	const loading = $derived(activeTab === 'upcoming' ? store.upcomingLoading : store.mineLoading);
	const error = $derived(activeTab === 'upcoming' ? store.upcomingError : store.mineError);
	const events = $derived(activeTab === 'upcoming' ? store.upcoming : store.mine);

	// ── Month grouping (mirrors the desktop events page) ───────────────────────

	interface MonthGroup {
		month: string;
		events: CommunityEvent[];
	}

	function groupByMonth(list: CommunityEvent[]): MonthGroup[] {
		const groups: MonthGroup[] = [];
		for (const event of list) {
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

	const visibleGroups = $derived(groupByMonth(events));

	function switchTab(tab: EventsTab): void {
		if (tab === activeTab) return;
		slideDir = TAB_ORDER[tab] > TAB_ORDER[activeTab] ? 1 : -1;
		activeTab = tab;
		getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
		if (tab === 'mine') void store.loadMine();
	}

	function openEvent(id: string): void {
		navStack.pushEventDetail(id);
	}

	function openCreate(): void {
		getTelegramWebApp()?.HapticFeedback.impactOccurred('medium');
		navStack.pushEventCreate();
	}

	onMount(() => {
		void store.loadUpcoming();
	});
</script>

<div class="events-screen">
	<header class="events-header">
		<h1 class="events-title">{t('nav.events')}</h1>

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
	</header>

	<div class="events-scroll">
		{#key activeTab}
			<div class="tab-panel" in:fly={{ x: slideDir * 48, duration: 260, easing: cubicOut }}>
				{#if loading && events.length === 0}
					<div class="skeleton-list">
						{#each Array(3) as _}
							<div class="skel-card"></div>
						{/each}
					</div>
				{:else if error}
					<p class="muted">{error}</p>
				{:else if visibleGroups.length === 0}
					<p class="muted">
						{activeTab === 'mine' ? 'You have no events yet.' : 'No upcoming events.'}
					</p>
				{:else}
					{#each visibleGroups as group (group.month)}
						<div class="month-divider">
							<span class="month-label">{group.month}</span>
						</div>
						{#each group.events as event (event.id)}
							<EventCard {event} onOpen={openEvent} />
						{/each}
					{/each}
				{/if}
			</div>
		{/key}
	</div>

	{#if authStore.isAuthenticated}
		<button class="create-fab" onclick={openCreate} aria-label="Create event">
			<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4"
				stroke-linecap="round" stroke-linejoin="round" width="24" height="24" aria-hidden="true">
				<line x1="12" y1="5" x2="12" y2="19" />
				<line x1="5" y1="12" x2="19" y2="12" />
			</svg>
		</button>
	{/if}
</div>

<style>
	.events-screen {
		position: relative;
		display: flex;
		flex-direction: column;
		height: 100%;
		overflow: hidden;
	}

	/* ── Glass header — frosted bar that holds title, create, and tabs ───────── */

	.events-header {
		position: relative;
		flex-shrink: 0;
		padding: calc(0.3rem + var(--tg-top-clearance)) 1rem 0;
		background-color: color-mix(in srgb, var(--color-bg) 80%, transparent);
		backdrop-filter: blur(var(--glass-blur-md)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(var(--glass-blur-md)) saturate(var(--glass-saturate));
		z-index: 10;
	}

	.events-header::after {
		content: '';
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		height: 1px;
		background: linear-gradient(to right, transparent, var(--glass-border), transparent);
		pointer-events: none;
	}

	.events-title {
		margin: 0 0 0.5rem;
		font-size: 1.35rem;
		font-weight: 800;
		letter-spacing: -0.02em;
		color: var(--color-text-primary);
	}

	/* Floating action button — sits clear of Telegram's native top chrome
	   (Close / ⋯ menu) and just above the bottom nav. */
	.create-fab {
		position: absolute;
		right: 18px;
		bottom: calc(var(--bottom-nav-height, 72px) + var(--safe-bottom, 0px) + 8px);
		z-index: 15;
		width: 54px;
		height: 54px;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		border: none;
		/* Calm light-on-dark CTA, same language as the Join pill — no loud fill. */
		background: var(--color-text-primary);
		color: var(--color-bg);
		box-shadow:
			0 6px 20px rgba(0, 0, 0, 0.36),
			0 2px 6px rgba(0, 0, 0, 0.24);
		cursor: pointer;
		transition: transform var(--duration-press, 0.12s) var(--ease-out-quart, ease);
		-webkit-tap-highlight-color: transparent;
	}

	.create-fab:active {
		transform: scale(0.9);
	}

	.tabs-wrap {
		display: flex;
		gap: 0;
	}

	.tab {
		background: none;
		border: none;
		border-bottom: 2px solid transparent;
		padding: 8px 16px 10px;
		font-size: 0.9rem;
		font-weight: 500;
		color: var(--color-text-muted);
		cursor: pointer;
		font-family: inherit;
		transition: color 0.15s ease, border-color 0.15s ease;
		margin-bottom: -1px;
		-webkit-tap-highlight-color: transparent;
	}

	.tab-active {
		color: var(--color-text-primary);
		border-bottom-color: var(--color-text-primary);
		font-weight: 600;
	}

	/* ── Scroller ────────────────────────────────────────────────────────────── */

	.events-scroll {
		flex: 1;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
		padding: 16px 16px var(--content-bottom-padding, 104px);
		scrollbar-width: none;
	}

	.events-scroll::-webkit-scrollbar { display: none; }

	.tab-panel {
		display: flex;
		flex-direction: column;
	}

	.month-divider {
		padding: 16px 4px 8px;
		pointer-events: none;
	}

	.month-divider:first-child {
		padding-top: 0;
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
		padding: 48px 0;
		font-size: 0.9rem;
	}

	/* ── Skeletons ───────────────────────────────────────────────────────────── */

	.skeleton-list {
		display: flex;
		flex-direction: column;
		gap: 14px;
	}

	.skel-card {
		height: 132px;
		border-radius: 16px;
		background: var(--surface-tint-subtle);
		animation: skel-pulse 1.4s ease-in-out infinite;
	}

	@keyframes skel-pulse {
		0%, 100% { opacity: 0.5; }
		50% { opacity: 0.8; }
	}

	@media (prefers-reduced-motion: reduce) {
		.skel-card { animation: none; }
	}
</style>
