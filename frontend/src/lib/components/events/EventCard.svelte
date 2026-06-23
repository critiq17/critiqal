<script lang="ts">
	import { goto } from '$app/navigation';
	import Avatar from '$lib/ui/Avatar.svelte';
	import Modal from '$lib/ui/Modal.svelte';
	import { UseEventRsvp } from '$lib/features/events/useEventRsvp.svelte';
	import { eventService } from '$lib/services/event.service';
	import type { CommunityEvent, EventAttendee } from '$lib/types/event';

	interface Props {
		event: CommunityEvent;
		// Optional override for opening the event. Desktop routes to a page;
		// the mini-app pushes an in-app overlay instead.
		onOpen?: (id: string) => void;
	}
	let { event, onOpen }: Props = $props();

	const rsvp = new UseEventRsvp(event.id, event);

	// ── Attendees ─────────────────────────────────────────────────────────────
	// The compact list card shows the count from the event payload; the full
	// attendee list is fetched lazily only when the modal opens. This keeps the
	// list view from firing one request per card on mount.

	let showAttendees = $state(false);
	let allAttendees = $state<EventAttendee[]>([]);
	let attendeesLoading = $state(false);

	async function openAttendeesModal(): Promise<void> {
		showAttendees = true;
		if (allAttendees.length === 0) {
			attendeesLoading = true;
			try {
				const res = await eventService.getAttendees(event.id, 0, 50);
				allAttendees = res.content;
			} finally {
				attendeesLoading = false;
			}
		}
	}

	// ── Derived values ────────────────────────────────────────────────────────

	const isLive = $derived(event.status === 'LIVE');

	const dateDay = $derived((() => {
		const start = new Date(event.startsAt);
		return start.toLocaleString(undefined, { weekday: 'short', day: 'numeric', month: 'short' });
	})());

	const dateTime = $derived((() => {
		const start = new Date(event.startsAt);
		return start.toLocaleTimeString(undefined, { hour: '2-digit', minute: '2-digit' });
	})());

	const isUrl = $derived(
		!!event.locationValue && event.locationValue.startsWith('http')
	);

	const locationDomain = $derived(
		isUrl && event.locationValue
			? (() => {
				try { return new URL(event.locationValue).hostname; }
				catch { return event.locationValue; }
			})()
			: null
	);

	const locationText = $derived(
		!isUrl
			? (event.locationValue ?? (
				event.locationType === 'DISCORD' ? 'Discord'
				: event.locationType === 'TELEGRAM' ? 'Telegram'
				: event.locationType === 'IRL' ? 'IRL'
				: 'External'
			))
			: null
	);

	// Discord button target: prefer the Discord scheduled event URL,
	// fall back to the locationValue if it's a discord.gg invite.
	const discordLink = $derived(
		event.discordEventUrl ??
		(event.locationType === 'DISCORD' && event.locationValue?.startsWith('https://discord')
			? event.locationValue
			: null)
	);

	function open(): void {
		if (onOpen) {
			onOpen(event.id);
			return;
		}
		goto(`/events/${event.id}`);
	}
</script>

<article class="event-card" class:has-cover={!!event.coverImageUrl}>
	<!-- ambient glow from cover image -->
	{#if event.coverImageUrl}
		<div
			class="card-glow"
			style:background-image="url('{event.coverImageUrl}')"
			aria-hidden="true"
		></div>
	{/if}

	<div class="card-inner">
		<!-- meta row: date block · live badge · location right-aligned -->
		<div class="meta">
			<div class="date-block">
				<span class="date-main">{dateDay}</span>
				<span class="date-time">{dateTime}</span>
			</div>
			{#if isLive}<span class="live-chip">Live</span>{/if}
			{#if isUrl && event.locationValue && locationDomain}
				<a
					href={event.locationValue}
					target="_blank"
					rel="noopener"
					class="loc-link"
					onclick={(e) => e.stopPropagation()}
				>{locationDomain}</a>
			{:else if locationText}
				<span class="loc-text">{locationText}</span>
			{/if}
		</div>

		<!-- cover image between meta and title -->
		{#if event.coverImageUrl}
			<button class="cover" onclick={open} aria-label={event.title} tabindex="-1">
				<img src={event.coverImageUrl} alt="" loading="lazy" />
			</button>
		{/if}

		<!-- title as clickable text -->
		<button class="title" onclick={open}>{event.title}</button>

		<!-- description: 2-line clamp -->
		{#if event.description}
			<p class="desc">{event.description}</p>
		{/if}

		<!-- footer: attendees button + RSVP actions + Discord link -->
		<div class="footer">
			<button class="attendees-btn" onclick={openAttendeesModal} aria-label="View attendees">
				<span class="going-count">{rsvp.count} going</span>
				{#if event.capacity}<span class="cap">/ {event.capacity}</span>{/if}
			</button>

			<div class="footer-actions">
				{#if discordLink}
					<a
						href={discordLink}
						target="_blank"
						rel="noopener"
						class="discord-btn"
						onclick={(e) => e.stopPropagation()}
						aria-label="Open in Discord"
					>
						<svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14" aria-hidden="true">
							<path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057c.002.022.015.04.032.05a19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03z"/>
						</svg>
					</a>
				{/if}

				{#if rsvp.status === 'GOING'}
					<button class="rsvp-btn going" disabled={rsvp.pending} onclick={() => rsvp.clear()}>Going</button>
				{:else}
					<button class="rsvp-btn join" disabled={rsvp.pending} onclick={() => rsvp.setGoing()}>Join</button>
				{/if}
			</div>
		</div>
	</div>
</article>

<!-- Attendees modal -->
<Modal open={showAttendees} onclose={() => { showAttendees = false; }} title="Attendees">
	{#if attendeesLoading}
		<p class="modal-muted">Loading...</p>
	{:else if allAttendees.length === 0}
		<p class="modal-muted">No attendees yet.</p>
	{:else}
		<ul class="attendee-list">
			{#each allAttendees as a (a.userId)}
				<li class="attendee-row">
					<a href="/{a.username}" onclick={() => { showAttendees = false; }} class="attendee-link">
						<Avatar src={a.avatarUrl} name={a.name ?? a.username} size={32} />
						<div class="attendee-info">
							<span class="attendee-name">{a.name ?? a.username}</span>
							<span class="attendee-handle">@{a.username}</span>
						</div>
					</a>
				</li>
			{/each}
		</ul>
	{/if}
</Modal>

<style>
	.event-card {
		--spring: cubic-bezier(0.34, 1.56, 0.64, 1);
		position: relative;
		border-radius: 16px;
		padding: 18px 18px 15px;
		margin-bottom: 14px;
		/* Settings-style minimal surface: a faint tint on the near-black page,
		   no border, no drop shadow. Reads as a soft inset, not a boxed card. */
		background: var(--surface-tint-subtle, rgba(255, 255, 255, 0.04));
		border: none;
		transition: transform 380ms var(--spring), background-color 200ms ease;
	}

	.event-card:hover {
		background: var(--surface-tint-soft, rgba(255, 255, 255, 0.06));
	}

	/* ambient glow from cover image */
	.card-glow {
		position: absolute;
		inset: 6px;
		z-index: 0;
		border-radius: inherit;
		background-size: cover;
		background-position: center;
		filter: blur(34px) saturate(160%) brightness(0.55);
		opacity: 0.15;
		pointer-events: none;
		transform: translateZ(0);
		transition: opacity 220ms ease;
	}

	.event-card:hover .card-glow {
		opacity: 0.22;
	}

	.card-inner {
		position: relative;
		z-index: 1;
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	/* ── Meta row ────────────────────────────────────────────────────────────── */

	.meta {
		display: flex;
		align-items: center;
		gap: 7px;
		flex-wrap: wrap;
	}

	.date-block {
		display: flex;
		align-items: baseline;
		gap: 6px;
		flex-wrap: wrap;
	}

	.date-main {
		font-size: 0.82rem;
		font-weight: 600;
		color: var(--color-text-primary);
	}

	.date-time {
		font-size: 0.76rem;
		color: var(--color-text-muted);
	}

	.live-chip {
		display: inline-flex;
		align-items: center;
		gap: 5px;
		font-size: 0.7rem;
		font-weight: 500;
		color: var(--color-text-secondary);
	}

	.live-chip::before {
		content: '';
		width: 5px;
		height: 5px;
		border-radius: 50%;
		background: var(--color-accent);
	}

	.loc-text,
	.loc-link {
		font-size: 0.75rem;
		color: var(--color-text-muted);
		margin-left: auto;
		text-decoration: none;
	}

	.loc-link:hover {
		text-decoration: underline;
	}

	/* ── Cover image ─────────────────────────────────────────────────────────── */

	.cover {
		display: block;
		width: 100%;
		border: none;
		padding: 0;
		background: none;
		cursor: pointer;
		border-radius: 14px;
		overflow: hidden;
		margin: 2px 0;
	}

	.cover img {
		width: 100%;
		aspect-ratio: 16 / 7;
		object-fit: cover;
		display: block;
	}

	/* ── Title ───────────────────────────────────────────────────────────────── */

	.title {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text-primary);
		text-align: left;
		background: none;
		border: none;
		padding: 0;
		cursor: pointer;
		line-height: 1.3;
		letter-spacing: -0.01em;
	}

	/* ── Description ─────────────────────────────────────────────────────────── */

	.desc {
		font-size: 0.85rem;
		color: var(--color-text-secondary);
		margin: 0;
		line-height: 1.45;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow: hidden;
	}

	/* ── Footer ──────────────────────────────────────────────────────────────── */

	.footer {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 8px;
		flex-wrap: wrap;
		margin-top: 2px;
	}

	.footer-actions {
		display: flex;
		align-items: center;
		gap: 6px;
		flex-wrap: wrap;
	}

	.attendees-btn {
		display: flex;
		align-items: center;
		gap: 5px;
		background: none;
		border: none;
		padding: 0;
		cursor: pointer;
		color: inherit;
	}

	.going-count {
		font-size: 0.78rem;
		color: var(--color-text-secondary);
	}

	.cap {
		font-size: 0.75rem;
		color: var(--color-text-muted);
	}

	/* ── RSVP pill ───────────────────────────────────────────────────────────── */

	.rsvp-btn {
		padding: 7px 18px;
		border-radius: 999px;
		border: none;
		font-family: inherit;
		font-size: 0.8rem;
		font-weight: 600;
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
		transition: opacity 0.15s ease, transform 0.1s ease, background-color 0.15s ease;
	}

	.rsvp-btn:active:not(:disabled) { transform: scale(0.95); }
	.rsvp-btn:disabled { opacity: 0.5; cursor: not-allowed; }

	/* Single calm CTA — a light pill on the dark surface, no loud accent fill. */
	.rsvp-btn.join {
		background: var(--color-text-primary);
		color: var(--color-bg);
	}

	/* Confirmed state recedes into the surface. */
	.rsvp-btn.going {
		background: var(--surface-tint-medium, rgba(255, 255, 255, 0.1));
		color: var(--color-text-secondary);
	}

	/* ── Discord glyph ───────────────────────────────────────────────────────── */
	/* A whisper-quiet icon link, not a coloured pill. */
	.discord-btn {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 32px;
		height: 32px;
		border-radius: 50%;
		background: none;
		border: none;
		color: var(--color-text-muted);
		text-decoration: none;
		flex-shrink: 0;
		transition: color 0.15s ease, background-color 0.15s ease;
	}

	.discord-btn:hover {
		color: var(--color-text-secondary);
		background: var(--surface-tint-soft, rgba(255, 255, 255, 0.06));
	}

	/* ── Attendees modal content ─────────────────────────────────────────────── */

	.modal-muted {
		color: var(--color-text-muted);
		font-size: 0.875rem;
		margin: 0;
		text-align: center;
		padding: 8px 0;
	}

	.attendee-list {
		list-style: none;
		margin: 0;
		padding: 0;
		display: flex;
		flex-direction: column;
		gap: 4px;
	}

	.attendee-row {
		border-radius: 10px;
		overflow: hidden;
	}

	.attendee-link {
		display: flex;
		align-items: center;
		gap: 10px;
		padding: 6px 8px;
		border-radius: 10px;
		text-decoration: none;
		transition: background-color 0.15s;
	}

	.attendee-link:hover {
		background: var(--color-surface-hover, rgba(255, 255, 255, 0.06));
	}

	.attendee-info {
		display: flex;
		flex-direction: column;
		gap: 1px;
	}

	.attendee-name {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--color-text-primary);
	}

	.attendee-handle {
		font-size: 0.78rem;
		color: var(--color-text-muted);
	}
</style>
