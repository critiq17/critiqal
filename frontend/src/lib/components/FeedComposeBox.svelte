<script lang="ts">
	import { goto } from '$app/navigation';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import type { UseComposer } from '$lib/features/posts/useComposer.svelte';
	import { eventService } from '$lib/services/event.service';
	import type { CreateEventRequest, EventLocationType } from '$lib/types/event';
	import { t } from '$lib/i18n';
	import DateTimePicker from '$lib/ui/DateTimePicker.svelte';

	interface Props {
		composer: UseComposer;
		onSubmit: () => void;
	}

	let { composer, onSubmit }: Props = $props();

	// ── Mode toggle ───────────────────────────────────────────────────────────

	type Mode = 'post' | 'event';
	let mode = $state<Mode>('post');

	// ── Permission state (cached after first check) ───────────────────────────

	let permissionChecked = $state(false);
	let canCreate = $state<boolean | null>(null);

	// ── Event form state ──────────────────────────────────────────────────────

	let eventTitle = $state('');
	let eventDescription = $state('');
	let locationType = $state<EventLocationType>('DISCORD');
	let locationValue = $state('');
	let startsAt = $state('');
	let endsAt = $state('');
	let publishNow = $state(true);
	let eventSubmitting = $state(false);
	let eventError = $state<string | null>(null);
	let capacityInput = $state<number | null>(null);
	let capacityError = $state<string | null>(null);

	// ── Derived labels ────────────────────────────────────────────────────────

	const submitLabel = $derived(publishNow ? 'Publish event' : 'Save draft');

	// ── Mode switching ────────────────────────────────────────────────────────

	async function switchMode(next: Mode): Promise<void> {
		if (next === mode) return;
		if (next === 'post') {
			eventTitle = '';
			eventDescription = '';
			locationType = 'DISCORD';
			locationValue = '';
			startsAt = '';
			endsAt = '';
			publishNow = true;
			eventError = null;
			capacityInput = null;
			capacityError = null;
		}
		mode = next;
		if (next === 'event' && !permissionChecked) {
			canCreate = null;
			try {
				const res = await eventService.canCreate();
				canCreate = res.canCreateEvents;
			} catch {
				canCreate = false;
			} finally {
				permissionChecked = true;
			}
		}
	}

	// ── Post handlers (unchanged) ─────────────────────────────────────────────

	function handleKeydown(e: KeyboardEvent): void {
		if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) onSubmit();
	}

	function handlePhotoSelect(e: Event): void {
		const input = e.target as HTMLInputElement;
		const files = input.files;
		if (!files || files.length === 0) return;
		composer.addFiles(Array.from(files));
		input.value = '';
	}

	// ── Event submit ──────────────────────────────────────────────────────────

	async function handleEventSubmit(): Promise<void> {
		eventError = null;
		if (!eventTitle.trim()) {
			eventError = 'Title is required.';
			return;
		}
		if (!startsAt) {
			eventError = 'Start date is required.';
			return;
		}
		if (endsAt && new Date(endsAt) <= new Date(startsAt)) {
			eventError = 'End date must be after start date.';
			return;
		}
		if (capacityInput !== null && capacityInput < 2) {
			capacityError = 'Minimum 2 people.';
			return;
		}
		capacityError = null;
		eventSubmitting = true;
		try {
			const req: CreateEventRequest = {
				title: eventTitle.trim(),
				locationType,
				startsAt: new Date(startsAt).toISOString(),
				publishNow,
				...(eventDescription.trim() ? { description: eventDescription.trim() } : {}),
				...(locationValue.trim() ? { locationValue: locationValue.trim() } : {}),
				...(endsAt ? { endsAt: new Date(endsAt).toISOString() } : {}),
				...(capacityInput !== null ? { capacity: capacityInput } : {}),
			};
			const result = await eventService.create(req);
			goto(`/events/${result.id}`);
		} catch (err) {
			eventError = err instanceof Error ? err.message : 'Failed to create event.';
			eventSubmitting = false;
		}
	}
</script>

<div class="compose-box">
	<div class="compose-avatar" aria-hidden="true">
		{#if authStore.user?.avatarUrl}
			<img src={authStore.user.avatarUrl} alt={authStore.user.username} class="compose-avatar-img" loading="lazy" decoding="async" />
		{:else}
			<span class="compose-avatar-initial">
				{(authStore.user?.name ?? authStore.user?.username ?? '?').charAt(0).toUpperCase()}
			</span>
		{/if}
	</div>
	<div class="compose-right">
		<!-- Mode tab strip -->
		<div class="mode-strip" role="tablist" aria-label="Compose mode">
			<button
				type="button"
				role="tab"
				aria-selected={mode === 'post'}
				class="mode-tab"
				class:mode-tab-active={mode === 'post'}
				onclick={() => switchMode('post')}
			>Post</button>
			<button
				type="button"
				role="tab"
				aria-selected={mode === 'event'}
				class="mode-tab"
				class:mode-tab-active={mode === 'event'}
				onclick={() => switchMode('event')}
			>Event</button>
		</div>

		{#if mode === 'post'}
			<textarea
				class="compose-input"
				value={composer.text}
				oninput={(e) => { composer.text = (e.target as HTMLTextAreaElement).value; }}
				onkeydown={handleKeydown}
				placeholder={t('feed.composePlaceholder')}
				rows={3}
				disabled={composer.loading}
				aria-label={t('post.composeTitle')}
			></textarea>

			{#if composer.previewUrls.length > 0}
				<div class="photo-thumbnail-row">
					{#each composer.previewUrls as url, i (url)}
						<div class="photo-thumbnail-wrap">
							<img src={url} alt="Selected photo {i + 1}" class="photo-thumbnail-img" />
							<button
								class="photo-remove-btn"
								onclick={() => composer.removePhoto(i)}
								aria-label="Remove photo {i + 1}"
								type="button"
							>×</button>
						</div>
					{/each}
				</div>
			{/if}

			<div class="compose-actions">
				<div class="compose-actions-left">
					{#if composer.selectedFiles.length < composer.maxPhotos}
						<label class="compose-photo-label" aria-label={t('common.add')} title={`${t('common.add')} (${composer.maxPhotos})`}>
							<input
								type="file"
								accept="image/jpeg,image/png,image/webp"
								class="compose-photo-input"
								onchange={handlePhotoSelect}
								disabled={composer.loading}
								multiple
							/>
							<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18" aria-hidden="true">
								<path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
								<circle cx="12" cy="13" r="4" />
							</svg>
						</label>
					{/if}
					<span class="compose-hint">
						{#if composer.selectedFiles.length > 0}
							{composer.selectedFiles.length}/{composer.maxPhotos} · Ctrl+Enter
						{:else}
							Ctrl+Enter
						{/if}
					</span>
				</div>
				<button
					class="compose-submit"
					onclick={onSubmit}
					disabled={!composer.canPost}
				>
					{#if composer.loading}
						{t('post.composePosting')}
					{:else}
						{t('post.composePost')}
					{/if}
				</button>
			</div>

			{#if composer.errorMessage}
				<p class="compose-error" role="alert">{composer.errorMessage}</p>
			{/if}

		{:else}
			<!-- Event mode -->
			{#if canCreate === null && !permissionChecked}
				<div class="perm-checking" aria-live="polite">
					<span class="spinner" aria-hidden="true"></span>
					<span class="sr-only">Checking permissions...</span>
				</div>
			{:else if canCreate === null && permissionChecked}
				<!-- Still null means the async hasn't resolved yet; spinner -->
				<div class="perm-checking" aria-live="polite">
					<span class="spinner" aria-hidden="true"></span>
				</div>
			{:else if canCreate === false}
				<div class="perm-locked">
					<p class="perm-locked-text">You need an Aedile, Praetor, or Consul badge to host events.</p>
					<p class="perm-locked-text">Badges are earned through community reputation.</p>
				</div>
			{:else}
				<div class="event-form">
					<!-- Title -->
					<input
						type="text"
						class="evt-bare evt-title"
						maxlength={120}
						placeholder="Event title"
						bind:value={eventTitle}
						disabled={eventSubmitting}
					/>

					<!-- Description -->
					<textarea
						class="evt-bare evt-desc"
						placeholder="Short description (optional)"
						maxlength={250}
						rows={2}
						bind:value={eventDescription}
						disabled={eventSubmitting}
					></textarea>

					<hr class="evt-sep" />

					<!-- Location value -->
					<div class="loc-section" role="group" aria-label="Location">
						<input
							type="text"
							class="evt-bare evt-loc-val"
							placeholder="https://discord.gg/..."
							aria-label="Discord channel or invite link"
							bind:value={locationValue}
							disabled={eventSubmitting}
						/>
					</div>

					<hr class="evt-sep" />

					<!-- Dates -->
					<div class="evt-dates">
						<div class="evt-date-row">
							<span class="evt-date-lbl">Starts</span>
							<DateTimePicker
								bind:value={startsAt}
								placeholder="Add start date"
								disabled={eventSubmitting}
							/>
						</div>
						<div class="evt-date-row">
							<span class="evt-date-lbl">Ends</span>
							<DateTimePicker
								bind:value={endsAt}
								placeholder="Add end date (optional)"
								disabled={eventSubmitting}
							/>
						</div>
					</div>

					<hr class="evt-sep" />

					<!-- Capacity -->
					<div class="evt-date-row">
						<span class="evt-date-lbl">Limit</span>
						<input
							type="number"
							class="evt-bare evt-capacity"
							min="2"
							placeholder="Unlimited"
							bind:value={capacityInput}
							disabled={eventSubmitting}
						/>
						{#if capacityError}
							<span class="cap-error">{capacityError}</span>
						{/if}
					</div>

					<hr class="evt-sep" />

					<!-- Bottom: toggle + submit -->
					<div class="evt-bottom">
						<label class="evt-toggle-row">
							<button
								type="button"
								role="switch"
								aria-checked={publishNow}
								aria-label={publishNow ? 'Publish immediately' : 'Save as draft'}
								class="toggle"
								class:toggle-on={publishNow}
								onclick={() => { publishNow = !publishNow; }}
								disabled={eventSubmitting}
							>
								<span class="toggle-thumb"></span>
							</button>
							<span class="evt-toggle-lbl">{publishNow ? 'Publish now' : 'Save draft'}</span>
						</label>
						<button
							class="compose-submit"
							onclick={handleEventSubmit}
							disabled={eventSubmitting}
						>
							{#if eventSubmitting}
								<span class="spinner spinner-sm" aria-hidden="true"></span>
							{:else}
								{submitLabel}
							{/if}
						</button>
					</div>

					{#if eventError}
						<p class="compose-error" role="alert">{eventError}</p>
					{/if}
				</div>
			{/if}
		{/if}
	</div>
</div>

<style>
	.compose-box {
		display: flex;
		gap: 0.75rem;
		padding: 1.125rem 1.25rem;
		margin: 0.5rem 0 1rem;
		border-radius: 1rem;
		background: var(--glass-bg-soft);
		backdrop-filter: blur(calc(var(--glass-blur) + 4px)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(calc(var(--glass-blur) + 4px)) saturate(var(--glass-saturate));
		border: 1px solid var(--glass-border);
		box-shadow: inset 0 1px 0 var(--glass-highlight);
		transition: box-shadow 0.2s ease;
	}

	.compose-box:focus-within {
		box-shadow: inset 0 1px 0 var(--glass-highlight),
			0 0 0 1px var(--glass-highlight);
	}

	@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
		.compose-box {
			background: var(--color-surface);
		}
	}

	.compose-avatar {
		width: 2.5rem;
		height: 2.5rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.compose-avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.compose-avatar-initial {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--color-text-muted);
		user-select: none;
	}

	.compose-right {
		flex: 1;
		display: flex;
		flex-direction: column;
		gap: 0.625rem;
		min-width: 0;
	}

	/* ── Mode strip ──────────────────────────────────────────────────────────── */

	.mode-strip {
		display: flex;
		gap: 4px;
	}

	.mode-tab {
		padding: 4px 14px;
		border-radius: 999px;
		border: none;
		background: transparent;
		color: var(--color-text-muted);
		font-size: 0.82rem;
		font-weight: 500;
		font-family: inherit;
		cursor: pointer;
		opacity: 0.55;
		transition: background-color 0.12s ease, opacity 0.12s ease, color 0.12s ease;
	}

	.mode-tab-active {
		background: var(--color-surface-raised);
		color: var(--color-text-primary);
		opacity: 1;
	}

	.mode-tab:not(.mode-tab-active):hover {
		opacity: 0.8;
	}

	/* ── Post compose (unchanged) ────────────────────────────────────────────── */

	.compose-input {
		width: 100%;
		background: none;
		border: none;
		outline: none;
		font-size: 1rem;
		color: var(--color-text-primary);
		font-family: inherit;
		resize: none;
		line-height: 1.5;
		padding: 0;
	}

	.compose-input::placeholder {
		color: var(--color-text-muted);
		opacity: 0.5;
	}

	.compose-input:disabled {
		opacity: 0.6;
	}

	.compose-actions {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.compose-hint {
		font-size: 0.6875rem;
		color: var(--color-text-muted);
		opacity: 0.45;
	}

	.compose-actions-left {
		display: flex;
		align-items: center;
		gap: 0.625rem;
	}

	.compose-photo-label {
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		color: var(--color-text-muted);
		padding: 0.25rem;
		border-radius: 0.375rem;
		transition: color 0.15s ease, background-color 0.15s ease;
	}

	.compose-photo-label:hover {
		color: var(--color-text-primary);
		background-color: var(--color-surface-raised);
	}

	.compose-photo-input {
		display: none;
	}

	.photo-thumbnail-row {
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
		margin-bottom: 0.5rem;
	}

	.photo-thumbnail-wrap {
		position: relative;
		width: 72px;
		height: 72px;
		flex-shrink: 0;
		border-radius: 0.5rem;
		overflow: hidden;
	}

	.photo-thumbnail-img {
		display: block;
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.photo-remove-btn {
		position: absolute;
		top: 3px;
		right: 3px;
		background: rgba(0, 0, 0, 0.7);
		color: #fff;
		border: none;
		border-radius: 50%;
		width: 16px;
		height: 16px;
		font-size: 0.6875rem;
		line-height: 1;
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		transition: background-color 0.15s ease;
		padding: 0;
	}

	.photo-remove-btn:hover {
		background: rgba(0, 0, 0, 0.9);
	}

	.compose-submit {
		background: var(--color-text-primary);
		color: var(--color-bg);
		border: none;
		border-radius: 9999px;
		padding: 0.4375rem 1.125rem;
		font-size: 0.875rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition: opacity 0.15s ease, transform 0.1s ease;
		display: inline-flex;
		align-items: center;
		gap: 6px;
	}

	.compose-submit:disabled {
		opacity: 0.35;
		cursor: not-allowed;
	}

	.compose-submit:not(:disabled):hover {
		opacity: 0.85;
	}

	.compose-submit:not(:disabled):active {
		transform: scale(0.96);
	}

	.compose-error {
		margin: 0;
		font-size: 0.75rem;
		color: #e05252;
	}

	/* ── Permission states ───────────────────────────────────────────────────── */

	.perm-checking {
		display: flex;
		align-items: center;
		gap: 8px;
		padding: 12px 0;
	}

	.perm-locked {
		padding: 8px 0;
		display: flex;
		flex-direction: column;
		gap: 4px;
	}

	.perm-locked-text {
		margin: 0;
		font-size: 0.8rem;
		color: var(--color-text-muted);
		line-height: 1.5;
	}

	/* ── Event form ──────────────────────────────────────────────────────────── */

	.event-form {
		display: flex;
		flex-direction: column;
		gap: 0;
	}

	/* Bare inputs — same visual language as post compose */
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

	.evt-bare:disabled {
		opacity: 0.45;
		cursor: not-allowed;
	}

	.evt-title {
		font-size: 1rem;
		margin-bottom: 5px;
	}

	.evt-desc {
		font-size: 0.85rem;
		color: var(--color-text-secondary);
		margin-bottom: 2px;
	}

	.evt-sep {
		border: none;
		border-top: 1px solid rgba(255,255,255,0.055);
		margin: 8px 0;
	}

	/* Location */
	.loc-section {
		display: flex;
		flex-direction: column;
		gap: 0;
	}

	.evt-loc-val {
		font-size: 0.83rem;
		color: var(--color-text-secondary);
		padding-left: 1px;
	}

	.evt-capacity {
		font-size: 0.83rem;
		width: 80px;
		color: var(--color-text-secondary);
	}

	.cap-error {
		font-size: 0.72rem;
		color: #e05252;
		margin-left: 4px;
	}

	/* Dates */
	.evt-dates {
		display: flex;
		flex-direction: column;
		gap: 7px;
	}

	.evt-date-row {
		display: flex;
		align-items: center;
		gap: 10px;
	}

	.evt-date-lbl {
		font-size: 0.72rem;
		color: var(--color-text-muted);
		opacity: 0.55;
		min-width: 34px;
		letter-spacing: 0.01em;
	}

	/* Bottom row */
	.evt-bottom {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
	}

	.evt-toggle-row {
		display: flex;
		align-items: center;
		gap: 7px;
		cursor: pointer;
	}

	.evt-toggle-lbl {
		font-size: 0.78rem;
		color: var(--color-text-muted);
		opacity: 0.65;
		user-select: none;
	}

	/* Toggle */
	.toggle {
		flex-shrink: 0;
		width: 34px;
		height: 20px;
		border-radius: 999px;
		border: none;
		background: var(--color-border);
		cursor: pointer;
		position: relative;
		transition: background-color 0.15s ease;
		padding: 0;
	}

	.toggle:disabled {
		opacity: 0.45;
		cursor: not-allowed;
	}

	.toggle-on {
		background: var(--color-accent);
	}

	.toggle-thumb {
		display: block;
		width: 14px;
		height: 14px;
		border-radius: 50%;
		background: #fff;
		position: absolute;
		top: 3px;
		left: 3px;
		transition: transform 0.15s ease;
		pointer-events: none;
	}

	.toggle-on .toggle-thumb {
		transform: translateX(14px);
	}

	/* ── Spinner ─────────────────────────────────────────────────────────────── */

	.spinner {
		display: inline-block;
		width: 14px;
		height: 14px;
		border: 2px solid var(--color-border);
		border-top-color: var(--color-text-secondary);
		border-radius: 50%;
		animation: spin 0.7s linear infinite;
	}

	.spinner-sm {
		width: 12px;
		height: 12px;
		border-width: 2px;
		border-color: rgba(255, 255, 255, 0.3);
		border-top-color: #fff;
	}

	@keyframes spin {
		to { transform: rotate(360deg); }
	}

	.sr-only {
		position: absolute;
		width: 1px;
		height: 1px;
		padding: 0;
		margin: -1px;
		overflow: hidden;
		clip: rect(0, 0, 0, 0);
		white-space: nowrap;
		border-width: 0;
	}
</style>
