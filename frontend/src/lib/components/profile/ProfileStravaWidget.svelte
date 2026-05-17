<script lang="ts">
	import { fade, fly } from 'svelte/transition';
	import { stravaStore } from '$lib/stores/strava.store.svelte';

	interface Props {
		isOwnProfile: boolean;
	}

	let { isOwnProfile }: Props = $props();

	let confirmDisconnect = $state(false);
	let comingSoon = $state(false);

	function showComingSoon(): void {
		comingSoon = true;
		setTimeout(() => { comingSoon = false; }, 4000);
	}

	async function handleDisconnect(): Promise<void> {
		await stravaStore.disconnect();
		confirmDisconnect = false;
	}
</script>

<div class="strava-widget" in:fly={{ y: 12, duration: 280, delay: 120 }}>
	<div class="strava-widget-header">
		<svg class="strava-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
			<path d="M13 2L3 14h9l-1 8 10-12h-9l1-8z" />
		</svg>
		<span class="strava-widget-title">Strava</span>
		{#if stravaStore.connection}
			<span class="strava-badge" in:fade={{ duration: 200 }}>Connected</span>
		{/if}
	</div>

	{#if stravaStore.loading}
		<div class="strava-skeleton" aria-hidden="true">
			<div class="sk-line" style="width: 60%; height: 0.75rem;"></div>
			<div class="sk-line" style="width: 80%; height: 0.625rem; margin-top: 0.375rem;"></div>
			<div class="sk-btn" style="margin-top: 0.875rem;"></div>
		</div>

	{:else if stravaStore.connection}
		{@const c = stravaStore.connection}
		<div class="strava-connected" in:fly={{ y: 6, duration: 200 }}>
			<div class="strava-athlete">
				{#if c.avatarUrl}
					<img src={c.avatarUrl} alt="{c.firstname} {c.lastname}" class="strava-avatar" />
				{:else}
					<div class="strava-avatar-fallback">{c.firstname.charAt(0)}</div>
				{/if}
				<div class="strava-athlete-info">
					<span class="strava-athlete-name">{c.firstname} {c.lastname}</span>
					{#if c.city}
						<span class="strava-athlete-city">{c.city}</span>
					{/if}
				</div>
			</div>

			{#if isOwnProfile}
				{#if confirmDisconnect}
					<div class="strava-confirm" in:fly={{ y: 4, duration: 180 }}>
						<p class="strava-confirm-text">Remove Strava?</p>
						<div class="strava-confirm-actions">
							<button
								class="strava-btn strava-btn-danger"
								onclick={handleDisconnect}
								disabled={stravaStore.loading}
							>
								{stravaStore.loading ? '…' : 'Remove'}
							</button>
							<button
								class="strava-btn strava-btn-ghost"
								onclick={() => (confirmDisconnect = false)}
								disabled={stravaStore.loading}
							>
								Cancel
							</button>
						</div>
					</div>
				{:else}
					<button
						class="strava-btn strava-btn-ghost strava-disconnect-btn"
						onclick={() => (confirmDisconnect = true)}
					>
						Disconnect
					</button>
				{/if}
			{/if}
		</div>

	{:else if isOwnProfile}
		<div class="strava-disconnected" in:fly={{ y: 6, duration: 200 }}>
			<p class="strava-desc">Sync your runs, rides and workouts.</p>
			<button class="strava-btn strava-btn-connect" onclick={showComingSoon}>
				Connect Strava
			</button>
			{#if comingSoon}
				<p class="strava-soon" in:fade={{ duration: 180 }}>
					Coming soon · pending Strava's approval
				</p>
			{/if}
		</div>

	{:else}
		<p class="strava-desc strava-desc-muted">Not connected.</p>
	{/if}

	{#if stravaStore.error}
		<p class="strava-error" role="alert" in:fade={{ duration: 150 }}>{stravaStore.error}</p>
	{/if}
</div>

<style>
	.strava-widget {
		margin-top: 1.5rem;
		padding: 1rem;
		background: var(--color-surface-raised);
		border: 1px solid var(--color-border);
		border-left: 2px solid #fc4c02;
		border-radius: 0.75rem;
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}

	.strava-widget-header {
		display: flex;
		align-items: center;
		gap: 0.4rem;
	}

	.strava-icon {
		width: 1rem;
		height: 1rem;
		color: #fc4c02;
		flex-shrink: 0;
	}

	.strava-widget-title {
		font-size: 0.8125rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: 0.01em;
	}

	.strava-badge {
		margin-left: auto;
		font-size: 0.6875rem;
		font-weight: 600;
		color: #10b981;
		background: rgba(16, 185, 129, 0.1);
		padding: 0.125rem 0.4rem;
		border-radius: 9999px;
	}

	.strava-athlete {
		display: flex;
		align-items: center;
		gap: 0.5rem;
	}

	.strava-avatar {
		width: 2rem;
		height: 2rem;
		border-radius: 50%;
		object-fit: cover;
		flex-shrink: 0;
	}

	.strava-avatar-fallback {
		width: 2rem;
		height: 2rem;
		border-radius: 50%;
		background: rgba(252, 76, 2, 0.15);
		color: #fc4c02;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 0.875rem;
		font-weight: 600;
		flex-shrink: 0;
	}

	.strava-athlete-info {
		display: flex;
		flex-direction: column;
		min-width: 0;
	}

	.strava-athlete-name {
		font-size: 0.8125rem;
		font-weight: 600;
		color: var(--color-text-primary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.strava-athlete-city {
		font-size: 0.75rem;
		color: var(--color-text-muted);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.strava-desc {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		line-height: 1.45;
	}

	.strava-desc-muted {
		font-style: italic;
	}

	.strava-soon {
		font-size: 0.75rem;
		color: var(--color-text-muted);
		text-align: center;
		line-height: 1.4;
		margin-top: 0.25rem;
	}

	.strava-btn {
		width: 100%;
		padding: 0.4375rem 0.75rem;
		border-radius: 0.5rem;
		font-size: 0.8125rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 0.375rem;
		transition:
			background-color 0.15s ease,
			color 0.15s ease,
			transform 0.1s ease;
	}

	.strava-btn:active:not(:disabled) {
		transform: scale(0.97);
	}

	.strava-btn:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}

	.strava-btn-connect {
		background: #fc4c02;
		color: #fff;
		border: none;
	}

	.strava-btn-connect:hover:not(:disabled) {
		background: #e04400;
	}

	.strava-btn-ghost {
		background: none;
		color: var(--color-text-muted);
		border: 1px solid var(--color-border);
	}

	.strava-btn-ghost:hover:not(:disabled) {
		background: var(--color-surface-raised);
		color: var(--color-text-primary);
	}

	.strava-btn-danger {
		background: none;
		color: var(--color-accent);
		border: 1px solid var(--color-accent);
	}

	.strava-btn-danger:hover:not(:disabled) {
		background: rgba(224, 82, 82, 0.08);
	}

	.strava-disconnect-btn {
		font-size: 0.75rem;
		padding: 0.3125rem 0.5rem;
		width: auto;
		align-self: flex-start;
	}

	.strava-confirm {
		display: flex;
		flex-direction: column;
		gap: 0.4rem;
	}

	.strava-confirm-text {
		font-size: 0.75rem;
		color: var(--color-text-muted);
	}

	.strava-confirm-actions {
		display: flex;
		gap: 0.375rem;
	}

	.strava-confirm-actions .strava-btn {
		flex: 1;
		font-size: 0.75rem;
		padding: 0.3125rem 0.5rem;
	}

	.strava-error {
		font-size: 0.75rem;
		color: var(--color-accent);
		line-height: 1.4;
	}

	.strava-skeleton {
		display: flex;
		flex-direction: column;
	}

	.sk-line {
		border-radius: 4px;
		background: var(--color-skeleton);
		animation: shimmer 1.6s ease-in-out infinite;
	}

	.sk-btn {
		height: 2rem;
		border-radius: 0.5rem;
		background: var(--color-skeleton);
		animation: shimmer 1.6s ease-in-out infinite;
	}

	@keyframes shimmer {
		0%, 100% { opacity: 0.5; }
		50% { opacity: 1; }
	}
</style>
