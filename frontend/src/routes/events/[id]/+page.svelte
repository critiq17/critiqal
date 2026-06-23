<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { page } from '$app/stores';
	import { eventService } from '$lib/services/event.service';
	import { UseEventRsvp } from '$lib/features/events/useEventRsvp.svelte';
	import Avatar from '$lib/ui/Avatar.svelte';
	import Button from '$lib/ui/Button.svelte';
	import Modal from '$lib/ui/Modal.svelte';
	import Skeleton from '$lib/ui/Skeleton.svelte';
	import type { CommunityEvent, EventAttendee } from '$lib/types/event';

	// ── Page state ───────────────────────────────────────────────────────────────

	let event = $state<CommunityEvent | null>(null);
	let loading = $state(true);
	let loadError = $state<string | null>(null);

	let rsvp = $state<UseEventRsvp | null>(null);

	// Management action state
	let confirmCancel = $state(false);
	let managing = $state(false);
	let manageError = $state<string | null>(null);

	// Description expand
	let descExpanded = $state(false);

	// Attendees modal
	let showAttendees = $state(false);
	let previewAttendees = $state<EventAttendee[]>([]);
	let allAttendees = $state<EventAttendee[]>([]);
	let attendeesLoading = $state(false);

	// ── Load ──────────────────────────────────────────────────────────────────────

	onMount(async () => {
		const id: string = $page.params.id ?? '';
		if (!id) {
			loadError = 'Event not found.';
			loading = false;
			return;
		}
		try {
			const data = await eventService.getById(id);
			event = data;
			rsvp = new UseEventRsvp(id, data);
			const preview = await eventService.getAttendees(id, 0, 4);
			previewAttendees = preview.content;
		} catch (err) {
			loadError = err instanceof Error ? err.message : 'Failed to load event.';
		} finally {
			loading = false;
		}
	});

	async function openAttendeesModal(): Promise<void> {
		if (!event) return;
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

	// ── Derived display values ───────────────────────────────────────────────────

	const dateLabel = $derived.by(() => {
		if (!event) return '';
		const now = new Date();
		const start = new Date(event.startsAt);
		const end = event.endsAt ? new Date(event.endsAt) : null;
		const fmtFull = (d: Date) => d.toLocaleString(undefined, {
			weekday: 'long', day: 'numeric', month: 'long',
			year: 'numeric', hour: '2-digit', minute: '2-digit',
		});
		const fmtTime = (d: Date) => d.toLocaleTimeString(undefined, {
			hour: '2-digit', minute: '2-digit',
		});
		const prefix =
			event.status === 'ENDED' || (end && now > end) ? 'Ended'
			: event.status === 'LIVE' || now >= start ? 'Started'
			: 'Starts';
		const base = `${prefix} ${fmtFull(start)}`;
		return end ? `${base} – ${fmtTime(end)}` : base;
	});

	const locationIsUrl = $derived(
		!!event?.locationValue && event.locationValue.startsWith('http')
	);

	const locationDomain = $derived(
		locationIsUrl && event?.locationValue
			? (() => {
				try { return new URL(event.locationValue!).hostname; }
				catch { return event.locationValue; }
			})()
			: null
	);

	const discordLink = $derived(
		event?.discordEventUrl ??
		(event?.locationType === 'DISCORD' && event.locationValue?.startsWith('https://discord')
			? event.locationValue
			: null)
	);

	const descTruncated = $derived(
		event?.description && event.description.length > 250
			? event.description.slice(0, 250) + '…'
			: (event?.description ?? null)
	);

	const needsExpand = $derived(
		!!event?.description && event.description.length > 250
	);

	// ── Management actions ───────────────────────────────────────────────────────

	async function handlePublish(): Promise<void> {
		if (!event) return;
		managing = true;
		manageError = null;
		try {
			event = await eventService.publish(event.id);
		} catch (err) {
			manageError = err instanceof Error ? err.message : 'Failed to publish.';
		} finally {
			managing = false;
		}
	}

	async function handleCancel(): Promise<void> {
		if (!event) return;
		managing = true;
		manageError = null;
		try {
			event = await eventService.cancel(event.id);
			confirmCancel = false;
		} catch (err) {
			manageError = err instanceof Error ? err.message : 'Failed to cancel event.';
		} finally {
			managing = false;
		}
	}
</script>

<svelte:head>
	{#if event}
		<title>{event.title} — Critiqal</title>
	{:else}
		<title>Event — Critiqal</title>
	{/if}
</svelte:head>

<div class="page">
	<a href="/events" class="back-link">
		<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"
			stroke-linecap="round" stroke-linejoin="round" class="back-icon" aria-hidden="true">
			<polyline points="15 18 9 12 15 6" />
		</svg>
		Events
	</a>

	{#if loading}
		<div class="skeleton-cover">
			<Skeleton width="100%" height="100%" radius="0" />
		</div>
		<div class="card">
			<Skeleton width="60%" height="1.75rem" radius="0.5rem" />
			<Skeleton width="30%" height="1rem" radius="0.5rem" />
			<Skeleton width="100%" height="1rem" radius="0.5rem" />
			<Skeleton width="85%" height="1rem" radius="0.5rem" />
		</div>

	{:else if loadError}
		<div class="card">
			<p class="err-msg">{loadError}</p>
			<Button variant="ghost" size="sm" onclick={() => goto('/events')}>Back to events</Button>
		</div>

	{:else if event && rsvp}
		{#if event.coverImageUrl}
			<div class="cover">
				<img src={event.coverImageUrl} alt={event.title} loading="eager" />
			</div>
		{/if}

		<div class="card">
			<div class="title-row">
				<span class="status-chip status-{event.status.toLowerCase()}"
					class:live-pulse={event.status === 'LIVE'}>
					{event.status === 'LIVE' ? 'LIVE' : event.status}
				</span>
			</div>

			<h1 class="title" class:title-cancelled={event.status === 'CANCELLED'}>
				{event.title}
			</h1>

			<div class="meta-row">
				<span class="meta-label">Date</span>
				<span class="meta-text">{dateLabel}</span>
			</div>

			{#if event.locationValue || event.locationType}
				<div class="meta-row">
					<span class="meta-label">Location</span>
					{#if locationIsUrl && event.locationValue && locationDomain}
						<a
							href={event.locationValue}
							target="_blank"
							rel="noopener"
							class="meta-link"
						>{locationDomain}</a>
					{:else}
						<span class="meta-text">
							{event.locationValue ?? (
								event.locationType === 'DISCORD' ? 'Discord'
								: event.locationType === 'TELEGRAM' ? 'Telegram'
								: event.locationType === 'IRL' ? 'IRL'
								: 'External'
							)}
						</span>
					{/if}
				</div>
			{/if}

			<div class="meta-row">
				<a href="/{event.host.username}" class="host-link">
					<Avatar
						src={event.host.avatarUrl}
						name={event.host.name ?? event.host.username}
						size={24}
					/>
					<span class="host-name">{event.host.name ?? event.host.username}</span>
					<span class="host-handle">@{event.host.username}</span>
				</a>
			</div>

			<button class="attendees-row" onclick={openAttendeesModal} aria-label="View attendees">
				{#if previewAttendees.length > 0}
					<div class="avatar-stack">
						{#each previewAttendees.slice(0, 4) as a (a.userId)}
							<Avatar src={a.avatarUrl} name={a.name ?? a.username} size={24} />
						{/each}
					</div>
				{/if}
				<span class="meta-text">
					{rsvp.count} going
					{#if event.capacity}
						<span class="capacity-muted">/ {event.capacity} capacity</span>
					{/if}
				</span>
			</button>

			{#if discordLink}
				<a
					href={discordLink}
					target="_blank"
					rel="noopener"
					class="discord-btn"
					aria-label="Open in Discord"
				>
					<svg viewBox="0 0 24 24" fill="currentColor" width="14" height="14" aria-hidden="true">
						<path d="M20.317 4.37a19.791 19.791 0 0 0-4.885-1.515.074.074 0 0 0-.079.037c-.21.375-.444.864-.608 1.25a18.27 18.27 0 0 0-5.487 0 12.64 12.64 0 0 0-.617-1.25.077.077 0 0 0-.079-.037A19.736 19.736 0 0 0 3.677 4.37a.07.07 0 0 0-.032.027C.533 9.046-.32 13.58.099 18.057c.002.022.015.04.032.05a19.9 19.9 0 0 0 5.993 3.03.078.078 0 0 0 .084-.028 14.09 14.09 0 0 0 1.226-1.994.076.076 0 0 0-.041-.106 13.107 13.107 0 0 1-1.872-.892.077.077 0 0 1-.008-.128 10.2 10.2 0 0 0 .372-.292.074.074 0 0 1 .077-.01c3.928 1.793 8.18 1.793 12.062 0a.074.074 0 0 1 .078.01c.12.098.246.198.373.292a.077.077 0 0 1-.006.127 12.299 12.299 0 0 1-1.873.892.077.077 0 0 0-.041.107c.36.698.772 1.362 1.225 1.993a.076.076 0 0 0 .084.028 19.839 19.839 0 0 0 6.002-3.03.077.077 0 0 0 .032-.054c.5-5.177-.838-9.674-3.549-13.66a.061.061 0 0 0-.031-.03z"/>
					</svg>
					<span>Open in Discord</span>
				</a>
			{/if}
		</div>

		{#if event.description}
			<div class="card">
				<p class="description">
					{descExpanded ? event.description : descTruncated}
				</p>
				{#if needsExpand}
					<button
						class="expand-btn"
						onclick={() => { descExpanded = !descExpanded; }}
					>
						{descExpanded ? 'Show less' : 'Show more'}
					</button>
				{/if}
			</div>
		{/if}

		{#if event.status !== 'ENDED' && event.status !== 'CANCELLED'}
			<div class="card rsvp-card">
				<p class="card-label">RSVP</p>
				<div class="rsvp-actions">
					{#if rsvp.status === 'GOING'}
						<Button
							variant="secondary"
							size="md"
							disabled={rsvp.pending}
							onclick={() => rsvp?.clear()}
						>
							Going
						</Button>
					{:else}
						<Button
							variant="primary"
							size="md"
							disabled={rsvp.pending}
							onclick={() => rsvp?.setGoing()}
						>
							Join
						</Button>
					{/if}

				</div>
			</div>
		{/if}

		{#if event.canManage}
			<div class="card manage-card">
				<p class="card-label">Manage</p>

				{#if manageError}
					<p class="err-msg">{manageError}</p>
				{/if}

				<div class="manage-actions">
					{#if event.status === 'DRAFT'}
						<Button variant="primary" size="sm" loading={managing} onclick={handlePublish}>
							Publish
						</Button>
					{/if}

					{#if event.status === 'PUBLISHED' || event.status === 'LIVE'}
						{#if confirmCancel}
							<span class="confirm-text">Cancel this event?</span>
							<Button variant="danger" size="sm" loading={managing} onclick={handleCancel}>
								Confirm cancel
							</Button>
							<Button
								variant="ghost"
								size="sm"
								disabled={managing}
								onclick={() => { confirmCancel = false; }}
							>
								Nevermind
							</Button>
						{:else}
							<Button
								variant="danger"
								size="sm"
								disabled={managing}
								onclick={() => { confirmCancel = true; manageError = null; }}
							>
								Cancel event
							</Button>
						{/if}
					{/if}
				</div>
			</div>
		{/if}
	{/if}
</div>

<Modal open={showAttendees} onclose={() => { showAttendees = false; }} title="Attendees">
	{#if attendeesLoading}
		<p class="modal-muted">Loading...</p>
	{:else if allAttendees.length === 0}
		<p class="modal-muted">No attendees yet.</p>
	{:else}
		<ul class="attendee-list">
			{#each allAttendees as a (a.userId)}
				<li class="attendee-item">
					<a href="/{a.username}" class="attendee-link" onclick={() => { showAttendees = false; }}>
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
	.page {
		max-width: 42rem;
		margin: 0 auto;
		padding: 16px 16px 80px;
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	/* ── Back link ───────────────────────────────────────────────────────────── */

	.back-link {
		display: inline-flex;
		align-items: center;
		gap: 4px;
		font-size: 0.82rem;
		color: var(--color-text-muted);
		text-decoration: none;
		transition: color 0.15s ease;
		align-self: flex-start;
	}

	.back-link:hover { color: var(--color-text-primary); }

	.back-icon {
		width: 14px;
		height: 14px;
	}

	/* ── Skeleton cover placeholder ─────────────────────────────────────────── */

	.skeleton-cover {
		width: 100%;
		aspect-ratio: 16 / 9;
		border-radius: 1rem;
		overflow: hidden;
	}

	/* ── Cover image ─────────────────────────────────────────────────────────── */

	.cover {
		width: 100%;
		border-radius: 1rem;
		overflow: hidden;
	}

	.cover img {
		width: 100%;
		aspect-ratio: 16 / 9;
		object-fit: cover;
		display: block;
	}

	/* ── Cards ───────────────────────────────────────────────────────────────── */

	.card {
		padding: 1.125rem 1.25rem;
		border-radius: 1rem;
		background: var(--glass-bg-soft);
		backdrop-filter: blur(calc(var(--glass-blur) + 4px)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(calc(var(--glass-blur) + 4px)) saturate(var(--glass-saturate));
		border: 1px solid var(--glass-border);
		box-shadow: inset 0 1px 0 var(--glass-highlight);
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}

	@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
		.card { background: var(--color-surface); }
	}

	.card-label {
		font-size: 0.6875rem;
		font-weight: 600;
		color: var(--color-text-muted);
		letter-spacing: 0.06em;
		text-transform: uppercase;
		margin: 0;
	}

	/* ── Status chips ────────────────────────────────────────────────────────── */

	.status-chip {
		display: inline-block;
		font-size: 0.6875rem;
		font-weight: 700;
		letter-spacing: 0.06em;
		text-transform: uppercase;
		padding: 3px 10px;
		border-radius: var(--radius-full, 999px);
	}

	.status-draft    { background: var(--color-surface-raised); color: var(--color-text-muted); }
	.status-published { background: var(--surface-tint-soft); color: var(--color-text-secondary); }
	.status-live     { background: var(--color-accent); color: #fff; }
	.status-ended    { background: var(--color-surface-raised); color: var(--color-text-muted); }
	.status-cancelled { background: rgba(224, 82, 82, 0.08); color: var(--color-text-muted); }

	.live-pulse {
		animation: pulse-live 1.8s ease-in-out infinite;
	}

	@keyframes pulse-live {
		0%, 100% { opacity: 1; }
		50% { opacity: 0.65; }
	}

	@media (prefers-reduced-motion: reduce) {
		.live-pulse { animation: none; }
	}

	/* ── Title ───────────────────────────────────────────────────────────────── */

	.title-row {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.title {
		font-size: 1.6rem;
		font-weight: 800;
		margin: 0;
		color: var(--color-text-primary);
		line-height: 1.2;
		letter-spacing: -0.02em;
	}

	.title-cancelled {
		text-decoration: line-through;
		color: var(--color-text-muted);
	}

	/* ── Meta rows ───────────────────────────────────────────────────────────── */

	.meta-row {
		display: flex;
		align-items: flex-start;
		gap: 10px;
	}

	.meta-label {
		font-size: 0.78rem;
		font-weight: 500;
		color: var(--color-text-muted);
		flex-shrink: 0;
		padding-top: 1px;
		min-width: 56px;
	}

	.meta-text {
		font-size: 0.9rem;
		color: var(--color-text-secondary);
		line-height: 1.4;
	}

	.meta-link {
		font-size: 0.9rem;
		color: var(--color-text-secondary);
		line-height: 1.4;
		text-decoration: none;
	}

	.meta-link:hover {
		text-decoration: underline;
	}

	.capacity-muted {
		color: var(--color-text-muted);
	}

	/* ── Host link ───────────────────────────────────────────────────────────── */

	.host-link {
		display: flex;
		align-items: center;
		gap: 8px;
		text-decoration: none;
		transition: opacity 0.15s ease;
	}

	.host-link:hover { opacity: 0.75; }

	.host-name {
		font-size: 0.9rem;
		font-weight: 600;
		color: var(--color-text-primary);
	}

	.host-handle {
		font-size: 0.82rem;
		color: var(--color-text-muted);
	}

	/* ── Attendees row ───────────────────────────────────────────────────────── */

	.attendees-row {
		display: flex;
		align-items: center;
		gap: 8px;
		background: none;
		border: none;
		padding: 0;
		cursor: pointer;
		color: inherit;
		text-align: left;
	}

	.avatar-stack {
		display: flex;
		align-items: center;
	}

	.avatar-stack > :global(:not(:first-child)) {
		margin-left: -8px;
	}

	/* ── Description ─────────────────────────────────────────────────────────── */

	.description {
		font-size: 0.9375rem;
		color: var(--color-text-secondary);
		line-height: 1.65;
		margin: 0;
		white-space: pre-line;
	}

	.expand-btn {
		background: none;
		border: none;
		padding: 0;
		font-size: 0.82rem;
		color: var(--color-text-muted);
		cursor: pointer;
		text-decoration: underline;
		font-family: inherit;
		align-self: flex-start;
	}

	.expand-btn:hover {
		color: var(--color-text-secondary);
	}

	/* ── RSVP card ───────────────────────────────────────────────────────────── */

	.rsvp-card { gap: 0.625rem; }

	.rsvp-actions {
		display: flex;
		gap: 8px;
		flex-wrap: wrap;
	}

	/* ── Management card ─────────────────────────────────────────────────────── */

	.manage-card { gap: 0.625rem; }

	.manage-actions {
		display: flex;
		align-items: center;
		gap: 8px;
		flex-wrap: wrap;
	}

	.confirm-text {
		font-size: 0.85rem;
		color: var(--color-text-secondary);
	}

	/* ── Error ───────────────────────────────────────────────────────────────── */

	.err-msg {
		font-size: 0.85rem;
		color: var(--color-accent);
		margin: 0;
	}

	/* ── Attendees modal content ──────────────────────────────────────────────── */

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
		gap: 10px;
	}

	.attendee-item {
		display: flex;
		align-items: center;
		gap: 10px;
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

	.attendee-link {
		display: flex;
		align-items: center;
		gap: 10px;
		text-decoration: none;
		transition: opacity 0.15s;
	}

	.attendee-link:hover {
		opacity: 0.75;
	}

	/* ── Discord button ───────────────────────────────────────────────────────── */

	.discord-btn {
		display: inline-flex;
		align-items: center;
		gap: 6px;
		padding: 6px 14px;
		border-radius: 999px;
		background: rgba(88, 101, 242, 0.15);
		border: 1px solid rgba(88, 101, 242, 0.25);
		color: rgb(148, 156, 248);
		font-size: 0.8rem;
		font-weight: 600;
		text-decoration: none;
		align-self: flex-start;
		transition: background-color 0.15s, border-color 0.15s;
	}

	.discord-btn:hover {
		background: rgba(88, 101, 242, 0.25);
		border-color: rgba(88, 101, 242, 0.4);
	}
</style>
