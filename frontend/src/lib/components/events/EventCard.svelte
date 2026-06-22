<script lang="ts">
	import { goto } from '$app/navigation';
	import Avatar from '$lib/ui/Avatar.svelte';
	import Button from '$lib/ui/Button.svelte';
	import { UseEventRsvp } from '$lib/features/events/useEventRsvp.svelte';
	import type { CommunityEvent } from '$lib/types/event';

	interface Props {
		event: CommunityEvent;
	}
	let { event }: Props = $props();

	const rsvp = new UseEventRsvp(event.id, event);

	const dateLabel = $derived(
		new Date(event.startsAt).toLocaleString(undefined, {
			weekday: 'short',
			day: 'numeric',
			month: 'short',
			hour: '2-digit',
			minute: '2-digit'
		})
	);

	const locationIcon = $derived(
		event.locationType === 'DISCORD'
			? '🎧'
			: event.locationType === 'TELEGRAM'
				? '✈️'
				: event.locationType === 'IRL'
					? '📍'
					: '🔗'
	);

	const isLive = $derived(event.status === 'LIVE');

	function open() {
		goto(`/events/${event.id}`);
	}
</script>

<article class="event-card glass" class:live={isLive}>
	{#if event.coverImageUrl}
		<button class="cover" onclick={open} aria-label={event.title}>
			<img src={event.coverImageUrl} alt="" loading="lazy" />
		</button>
	{/if}

	<div class="body">
		<div class="top">
			<span class="date-chip">{dateLabel}</span>
			{#if isLive}<span class="live-chip">LIVE</span>{/if}
			<span class="loc">{locationIcon} {event.locationValue ?? event.locationType}</span>
		</div>

		<button class="title" onclick={open}>{event.title}</button>

		<div class="host">
			<Avatar src={event.host.avatarUrl} name={event.host.name ?? event.host.username} size={20} />
			<span>{event.host.name ?? event.host.username}</span>
			<span class="dot">·</span>
			<span class="count">{rsvp.count} going{event.capacity ? ` / ${event.capacity}` : ''}</span>
		</div>

		<div class="actions">
			{#if rsvp.status === 'GOING'}
				<Button variant="secondary" size="sm" disabled={rsvp.pending} onclick={() => rsvp.clear()}>
					✓ Going
				</Button>
			{:else}
				<Button variant="primary" size="sm" disabled={rsvp.pending} onclick={() => rsvp.setGoing()}>
					Join
				</Button>
			{/if}
			<Button
				variant={rsvp.status === 'INTERESTED' ? 'secondary' : 'ghost'}
				size="sm"
				disabled={rsvp.pending}
				onclick={() => (rsvp.status === 'INTERESTED' ? rsvp.clear() : rsvp.setInterested())}
			>
				Interested
			</Button>
		</div>
	</div>
</article>

<style>
	.event-card {
		border-radius: 18px;
		overflow: hidden;
		margin-bottom: var(--spacing-md);
	}
	.event-card.live {
		box-shadow: 0 0 0 1px var(--color-accent), var(--glass-shadow);
	}
	.cover {
		display: block;
		width: 100%;
		border: none;
		padding: 0;
		background: none;
		cursor: pointer;
	}
	.cover img {
		width: 100%;
		aspect-ratio: 16 / 9;
		object-fit: cover;
		display: block;
	}
	.body {
		padding: 14px 16px 12px;
		display: flex;
		flex-direction: column;
		gap: 8px;
	}
	.top {
		display: flex;
		align-items: center;
		gap: 8px;
		flex-wrap: wrap;
		font-size: 0.78rem;
		color: var(--color-text-secondary);
	}
	.date-chip {
		background: var(--surface-tint-soft);
		padding: 2px 8px;
		border-radius: var(--radius-full);
	}
	.live-chip {
		background: var(--color-accent);
		color: #fff;
		padding: 2px 8px;
		border-radius: var(--radius-full);
		font-weight: 700;
		letter-spacing: 0.04em;
	}
	.loc {
		margin-left: auto;
	}
	.title {
		font-size: 1.05rem;
		font-weight: 700;
		color: var(--color-text-primary);
		text-align: left;
		background: none;
		border: none;
		padding: 0;
		cursor: pointer;
	}
	.host {
		display: flex;
		align-items: center;
		gap: 6px;
		font-size: 0.82rem;
		color: var(--color-text-secondary);
	}
	.dot {
		opacity: 0.5;
	}
	.actions {
		display: flex;
		gap: 8px;
		margin-top: 4px;
	}
</style>
