<script lang="ts">
	import { onMount } from 'svelte';
	import { eventService } from '$lib/services/event.service';
	import { mobileEventsStore } from '$lib/stores/mobile-events.store.svelte';
	import { navStack } from '$lib/stores/nav-stack.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import DateTimePicker from '$lib/ui/DateTimePicker.svelte';
	import type { CreateEventRequest, EventLocationType } from '$lib/types/event';

	interface Props {
		onBack: () => void;
	}

	let { onBack }: Props = $props();

	// ── Permission gate (mirrors FeedComposeBox) ───────────────────────────────

	let permissionChecked = $state(false);
	let canCreate = $state<boolean | null>(null);

	// ── Form state ─────────────────────────────────────────────────────────────

	let eventTitle = $state('');
	let eventDescription = $state('');
	let locationType = $state<EventLocationType>('DISCORD');
	let locationValue = $state('');
	let startsAt = $state('');
	let endsAt = $state('');
	let publishNow = $state(true);
	let capacityInput = $state<number | null>(null);
	let submitting = $state(false);
	let formError = $state<string | null>(null);
	let capacityError = $state<string | null>(null);

	const LOCATION_TYPES: { value: EventLocationType; label: string; placeholder: string }[] = [
		{ value: 'DISCORD', label: 'Discord', placeholder: 'https://discord.gg/…' },
		{ value: 'TELEGRAM', label: 'Telegram', placeholder: 'https://t.me/…' },
		{ value: 'IRL', label: 'IRL', placeholder: 'Venue or address' },
		{ value: 'EXTERNAL', label: 'Link', placeholder: 'https://…' }
	];

	const locationPlaceholder = $derived(
		LOCATION_TYPES.find((l) => l.value === locationType)?.placeholder ?? ''
	);

	const submitLabel = $derived(publishNow ? 'Publish event' : 'Save draft');

	onMount(async () => {
		try {
			const res = await eventService.canCreate();
			canCreate = res.canCreateEvents;
		} catch {
			canCreate = false;
		} finally {
			permissionChecked = true;
		}
	});

	function selectLocationType(type: EventLocationType): void {
		locationType = type;
		getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
	}

	async function handleSubmit(): Promise<void> {
		formError = null;
		capacityError = null;
		if (!eventTitle.trim()) {
			formError = 'Title is required.';
			return;
		}
		if (!startsAt) {
			formError = 'Start date is required.';
			return;
		}
		if (endsAt && new Date(endsAt) <= new Date(startsAt)) {
			formError = 'End date must be after start date.';
			return;
		}
		if (capacityInput !== null && capacityInput < 2) {
			capacityError = 'Minimum 2 people.';
			return;
		}
		submitting = true;
		try {
			const req: CreateEventRequest = {
				title: eventTitle.trim(),
				locationType,
				startsAt: new Date(startsAt).toISOString(),
				publishNow,
				...(eventDescription.trim() ? { description: eventDescription.trim() } : {}),
				...(locationValue.trim() ? { locationValue: locationValue.trim() } : {}),
				...(endsAt ? { endsAt: new Date(endsAt).toISOString() } : {}),
				...(capacityInput !== null ? { capacity: capacityInput } : {})
			};
			const result = await eventService.create(req);
			mobileEventsStore.onCreated(result);
			getTelegramWebApp()?.HapticFeedback.notificationOccurred('success');
			// Replace this overlay with the new event's detail view.
			navStack.pop();
			navStack.pushEventDetail(result.id);
		} catch (err) {
			formError = err instanceof Error ? err.message : 'Failed to create event.';
			submitting = false;
		}
	}
</script>

<div class="compose-overlay">
	<header class="compose-header glass">
		<button class="back-btn" onclick={onBack} aria-label="Back">
			<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"
				stroke-linecap="round" stroke-linejoin="round" width="22" height="22" aria-hidden="true">
				<polyline points="15 18 9 12 15 6" />
			</svg>
		</button>
		<span class="header-title">New event</span>
		<span class="header-spacer" aria-hidden="true"></span>
	</header>

	<div class="compose-scroll">
		{#if canCreate === null && !permissionChecked}
			<div class="perm-checking"><span class="spinner" aria-hidden="true"></span></div>
		{:else if canCreate === false}
			<div class="card perm-locked">
				<p class="perm-locked-text">You need an Aedile, Praetor, or Consul badge to host events.</p>
				<p class="perm-locked-text">Badges are earned through community reputation.</p>
			</div>
		{:else}
			<div class="card">
				<input
					type="text"
					class="evt-bare evt-title"
					maxlength={120}
					placeholder="Event title"
					bind:value={eventTitle}
					disabled={submitting}
				/>
				<textarea
					class="evt-bare evt-desc"
					placeholder="Short description (optional)"
					maxlength={250}
					rows={3}
					bind:value={eventDescription}
					disabled={submitting}
				></textarea>
			</div>

			<div class="card">
				<span class="field-label">Location</span>
				<div class="seg" role="group" aria-label="Location type">
					{#each LOCATION_TYPES as lt (lt.value)}
						<button
							type="button"
							class="seg-btn"
							class:seg-active={locationType === lt.value}
							onclick={() => selectLocationType(lt.value)}
							disabled={submitting}
						>{lt.label}</button>
					{/each}
				</div>
				<input
					type="text"
					class="evt-bare evt-loc"
					placeholder={locationPlaceholder}
					aria-label="Location detail"
					bind:value={locationValue}
					disabled={submitting}
				/>
			</div>

			<div class="card">
				<div class="row">
					<span class="field-label">Starts</span>
					<DateTimePicker bind:value={startsAt} present="modal" placeholder="Add start date" disabled={submitting} />
				</div>
				<hr class="sep" />
				<div class="row">
					<span class="field-label">Ends</span>
					<DateTimePicker bind:value={endsAt} present="modal" placeholder="Add end date (optional)" disabled={submitting} />
				</div>
				<hr class="sep" />
				<div class="row">
					<span class="field-label">Limit</span>
					<input
						type="number"
						class="evt-bare evt-capacity"
						min="2"
						placeholder="Unlimited"
						bind:value={capacityInput}
						disabled={submitting}
					/>
				</div>
				{#if capacityError}<span class="cap-error">{capacityError}</span>{/if}
			</div>

			<div class="card">
				<label class="toggle-row">
					<button
						type="button"
						role="switch"
						aria-checked={publishNow}
						aria-label={publishNow ? 'Publish immediately' : 'Save as draft'}
						class="toggle"
						class:toggle-on={publishNow}
						onclick={() => { publishNow = !publishNow; }}
						disabled={submitting}
					>
						<span class="toggle-thumb"></span>
					</button>
					<span class="toggle-lbl">{publishNow ? 'Publish now' : 'Save as draft'}</span>
				</label>
			</div>

			{#if formError}<p class="form-error" role="alert">{formError}</p>{/if}
		{/if}
	</div>

	{#if canCreate}
		<div class="compose-footer">
			<button class="submit-btn" onclick={handleSubmit} disabled={submitting}>
				{#if submitting}
					<span class="spinner spinner-sm" aria-hidden="true"></span>
				{:else}
					{submitLabel}
				{/if}
			</button>
		</div>
	{/if}
</div>

<style>
	.compose-overlay {
		display: flex;
		flex-direction: column;
		height: 100%;
		overflow: hidden;
	}

	/* ── Header ──────────────────────────────────────────────────────────────── */

	.compose-header {
		position: relative;
		flex-shrink: 0;
		display: flex;
		align-items: center;
		gap: 8px;
		padding: calc(0.25rem + var(--tg-top-clearance)) 0.75rem 0.55rem;
		background-color: color-mix(in srgb, var(--color-bg) 82%, transparent);
		backdrop-filter: blur(var(--glass-blur-md)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(var(--glass-blur-md)) saturate(var(--glass-saturate));
	}

	.compose-header::after {
		content: '';
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		height: 1px;
		background: linear-gradient(to right, transparent, var(--glass-border), transparent);
		pointer-events: none;
	}

	.back-btn {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 36px;
		height: 36px;
		border-radius: 50%;
		background: none;
		border: none;
		color: var(--color-text-primary);
		cursor: pointer;
		flex-shrink: 0;
		-webkit-tap-highlight-color: transparent;
	}

	.header-title {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text-primary);
	}

	.header-spacer { width: 36px; margin-left: auto; }

	/* ── Scroller ────────────────────────────────────────────────────────────── */

	.compose-scroll {
		flex: 1;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
		display: flex;
		flex-direction: column;
		gap: 12px;
		padding: 14px;
		scrollbar-width: none;
	}

	.compose-scroll::-webkit-scrollbar { display: none; }

	/* ── Cards ───────────────────────────────────────────────────────────────── */

	.card {
		padding: 1rem 1.125rem;
		border-radius: 16px;
		/* Settings-style minimal surface: faint tint on near-black, no border,
		   no shadow. No backdrop-filter — cards scroll over an opaque bg. */
		background: var(--surface-tint-subtle);
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}

	.field-label {
		font-size: 0.72rem;
		font-weight: 600;
		color: var(--color-text-muted);
		letter-spacing: 0.04em;
		text-transform: uppercase;
		min-width: 48px;
	}

	/* ── Bare inputs ─────────────────────────────────────────────────────────── */

	.evt-bare {
		background: none;
		border: none;
		outline: none;
		width: 100%;
		font-family: inherit;
		color: var(--color-text-primary);
		padding: 0;
		resize: none;
		line-height: 1.5;
		box-sizing: border-box;
	}

	.evt-bare::placeholder {
		color: var(--color-text-muted);
		opacity: 0.45;
	}

	.evt-bare:disabled { opacity: 0.45; }

	.evt-title { font-size: 1.15rem; font-weight: 600; }

	.evt-desc {
		font-size: 0.9rem;
		color: var(--color-text-secondary);
	}

	.evt-loc {
		font-size: 0.9rem;
		color: var(--color-text-secondary);
	}

	.evt-capacity {
		font-size: 0.9rem;
		width: 100px;
		color: var(--color-text-secondary);
		text-align: right;
	}

	/* ── Segmented control ───────────────────────────────────────────────────── */

	.seg {
		display: flex;
		gap: 4px;
		padding: 3px;
		border-radius: 12px;
		background: var(--surface-tint-soft, rgba(255, 255, 255, 0.05));
	}

	.seg-btn {
		flex: 1;
		padding: 8px 4px;
		border: none;
		border-radius: 9px;
		background: none;
		color: var(--color-text-muted);
		font-size: 0.8rem;
		font-weight: 500;
		font-family: inherit;
		cursor: pointer;
		transition: background-color 0.15s ease, color 0.15s ease;
		-webkit-tap-highlight-color: transparent;
	}

	.seg-active {
		background: var(--color-surface-raised, rgba(255, 255, 255, 0.1));
		color: var(--color-text-primary);
		font-weight: 600;
		box-shadow: inset 0 1px 0 var(--glass-highlight);
	}

	/* ── Rows ────────────────────────────────────────────────────────────────── */

	.row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
	}

	.sep {
		border: none;
		border-top: 1px solid var(--glass-border);
		margin: 0;
	}

	.cap-error {
		font-size: 0.75rem;
		color: var(--color-accent);
	}

	/* ── Toggle ──────────────────────────────────────────────────────────────── */

	.toggle-row {
		display: flex;
		align-items: center;
		gap: 10px;
		cursor: pointer;
	}

	.toggle {
		flex-shrink: 0;
		width: 42px;
		height: 25px;
		border-radius: 999px;
		border: none;
		background: var(--color-border);
		cursor: pointer;
		position: relative;
		transition: background-color 0.15s ease;
		padding: 0;
	}

	.toggle:disabled { opacity: 0.45; }

	.toggle-on { background: var(--color-accent); }

	.toggle-thumb {
		display: block;
		width: 19px;
		height: 19px;
		border-radius: 50%;
		background: #fff;
		position: absolute;
		top: 3px;
		left: 3px;
		transition: transform 0.15s ease;
		pointer-events: none;
	}

	.toggle-on .toggle-thumb { transform: translateX(17px); }

	.toggle-lbl {
		font-size: 0.9rem;
		color: var(--color-text-secondary);
		user-select: none;
	}

	/* ── Permission states ───────────────────────────────────────────────────── */

	.perm-checking {
		display: flex;
		justify-content: center;
		padding: 40px 0;
	}

	.perm-locked { gap: 6px; }

	.perm-locked-text {
		margin: 0;
		font-size: 0.85rem;
		color: var(--color-text-muted);
		line-height: 1.5;
	}

	.form-error {
		margin: 0;
		font-size: 0.85rem;
		color: var(--color-accent);
		padding: 0 4px;
	}

	/* ── Footer submit ───────────────────────────────────────────────────────── */

	.compose-footer {
		flex-shrink: 0;
		padding: 12px 14px calc(14px + env(safe-area-inset-bottom, 0px));
		border-top: 1px solid var(--glass-border);
		background-color: color-mix(in srgb, var(--color-bg) 82%, transparent);
		backdrop-filter: blur(var(--glass-blur-md)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(var(--glass-blur-md)) saturate(var(--glass-saturate));
	}

	.submit-btn {
		width: 100%;
		padding: 14px;
		border-radius: 14px;
		border: none;
		background: var(--color-text-primary);
		color: var(--color-bg);
		font-size: 0.95rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 8px;
		transition: opacity 0.15s ease, transform 0.1s ease;
		-webkit-tap-highlight-color: transparent;
	}

	.submit-btn:disabled { opacity: 0.5; cursor: not-allowed; }
	.submit-btn:not(:disabled):active { transform: scale(0.98); }

	/* ── Spinner ─────────────────────────────────────────────────────────────── */

	.spinner {
		display: inline-block;
		width: 18px;
		height: 18px;
		border: 2px solid var(--color-border);
		border-top-color: var(--color-text-secondary);
		border-radius: 50%;
		animation: spin 0.7s linear infinite;
	}

	.spinner-sm {
		width: 16px;
		height: 16px;
		border-color: rgba(255, 255, 255, 0.3);
		border-top-color: var(--color-bg);
	}

	@keyframes spin {
		to { transform: rotate(360deg); }
	}
</style>
