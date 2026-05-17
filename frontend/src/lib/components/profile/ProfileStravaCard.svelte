<script lang="ts">
	import { fade } from 'svelte/transition';
	import { stravaStore } from '$lib/stores/strava.store.svelte';

	interface Props {
		confirmDisconnect: boolean;
		stravaComingSoon: boolean;
		onConnect: () => void;
		onDisconnect: () => void;
		onConfirmDisconnectChange: (v: boolean) => void;
	}

	let {
		confirmDisconnect,
		stravaComingSoon,
		onConnect,
		onDisconnect,
		onConfirmDisconnectChange
	}: Props = $props();
</script>

{#if stravaStore.loading && !stravaStore.connection}
	<div class="settings-row settings-row-info">
		<span class="settings-row-label">Strava</span>
		<span class="spinner-xs" aria-label="Loading"></span>
	</div>
{:else if stravaStore.connection}
	{#if confirmDisconnect}
		<div class="settings-row settings-strava-confirm">
			<span class="settings-strava-confirm-text">Disconnect Strava?</span>
			<div class="settings-strava-confirm-actions">
				<button
					class="strava-yes-btn"
					onclick={onDisconnect}
					disabled={stravaStore.loading}
				>
					{stravaStore.loading ? '…' : 'Yes'}
				</button>
				<button
					class="strava-no-btn"
					onclick={() => onConfirmDisconnectChange(false)}
					disabled={stravaStore.loading}
				>
					No
				</button>
			</div>
		</div>
	{:else}
		<div class="settings-row settings-strava-connected">
			<div class="strava-connected-info">
				<span class="strava-dot" aria-hidden="true"></span>
				<div class="strava-text">
					<span class="strava-athlete-name">{stravaStore.connection.firstname} {stravaStore.connection.lastname}</span>
					<span class="strava-connected-label">Strava connected</span>
				</div>
			</div>
			<button class="strava-disconnect-btn" onclick={() => onConfirmDisconnectChange(true)}>
				Disconnect
			</button>
		</div>
	{/if}
{:else}
	<button
		class="settings-row settings-row-strava-connect"
		onclick={onConnect}
		disabled={stravaStore.loading}
	>
		<div class="strava-connect-left">
			<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
				<polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
			</svg>
			<span>Connect Strava</span>
		</div>
		<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
			<polyline points="9 18 15 12 9 6"/>
		</svg>
	</button>
{/if}

{#if stravaComingSoon}
	<p class="strava-soon-note" in:fade={{ duration: 180 }}>Coming soon · pending Strava's approval</p>
{:else if stravaStore.error}
	<p class="strava-error-row" role="alert">{stravaStore.error}</p>
{/if}

<style>
	.settings-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 16px 16px;
		font-size: 16px;
		font-family: inherit;
		cursor: pointer;
		border: none;
		background: none;
		width: 100%;
		text-align: left;
		border-top: 1px solid var(--color-border, rgba(255, 255, 255, 0.08));
	}

	.settings-row-info {
		cursor: default;
	}

	.settings-row-label {
		font-size: 14px;
		color: rgba(255, 255, 255, 0.5);
	}

	.settings-row-strava-connect {
		color: #fc4c02;
	}

	.settings-row-strava-connect:active {
		background: rgba(252, 76, 2, 0.06);
	}

	.settings-row-strava-connect:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	.strava-connect-left {
		display: flex;
		align-items: center;
		gap: 10px;
	}

	.settings-strava-connected {
		cursor: default;
		flex-direction: row;
		justify-content: space-between;
	}

	.strava-connected-info {
		display: flex;
		align-items: center;
		gap: 10px;
	}

	.strava-dot {
		width: 8px;
		height: 8px;
		border-radius: 50%;
		background: #fc4c02;
		flex-shrink: 0;
	}

	.strava-text {
		display: flex;
		flex-direction: column;
		gap: 1px;
	}

	.strava-athlete-name {
		font-size: 15px;
		color: var(--color-text-primary, #f0f0f0);
		font-weight: 500;
	}

	.strava-connected-label {
		font-size: 12px;
		color: rgba(255, 255, 255, 0.38);
	}

	.strava-disconnect-btn {
		background: none;
		border: none;
		cursor: pointer;
		font-size: 14px;
		font-weight: 500;
		color: #e05252;
		padding: 6px 0;
		font-family: inherit;
		-webkit-tap-highlight-color: transparent;
	}

	.strava-disconnect-btn:active {
		opacity: 0.7;
	}

	.settings-strava-confirm {
		flex-direction: column;
		align-items: flex-start !important;
		gap: 10px;
		cursor: default;
	}

	.settings-strava-confirm-text {
		font-size: 15px;
		color: var(--color-text-primary, #f0f0f0);
	}

	.settings-strava-confirm-actions {
		display: flex;
		gap: 8px;
	}

	.strava-yes-btn {
		padding: 6px 18px;
		border-radius: 8px;
		background: rgba(224, 82, 82, 0.12);
		border: 1px solid rgba(224, 82, 82, 0.25);
		color: #e05252;
		font-size: 14px;
		font-weight: 500;
		cursor: pointer;
		font-family: inherit;
		min-height: 36px;
	}

	.strava-yes-btn:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	.strava-no-btn {
		padding: 6px 18px;
		border-radius: 8px;
		background: none;
		border: 1px solid var(--color-border, rgba(255, 255, 255, 0.15));
		color: var(--color-text-primary, #f0f0f0);
		font-size: 14px;
		cursor: pointer;
		font-family: inherit;
		min-height: 36px;
	}

	.strava-no-btn:disabled {
		opacity: 0.5;
	}

	.strava-soon-note {
		padding: 0 16px 12px;
		font-size: 12px;
		color: rgba(255, 255, 255, 0.38);
		line-height: 1.4;
	}

	.strava-error-row {
		padding: 0 16px 10px;
		font-size: 12px;
		color: #e05252;
	}

	.spinner-xs {
		display: inline-block;
		width: 16px;
		height: 16px;
		border: 2px solid rgba(255, 255, 255, 0.15);
		border-top-color: rgba(255, 255, 255, 0.55);
		border-radius: 50%;
		animation: spin 0.8s linear infinite;
		flex-shrink: 0;
	}

	@keyframes spin {
		from { transform: rotate(0deg); }
		to { transform: rotate(360deg); }
	}
</style>
