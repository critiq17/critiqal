<script lang="ts">
	import { goto } from '$app/navigation';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { stravaStore } from '$lib/stores/strava.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import { Sheet } from '$lib/ui';
	import ProfileStravaCard from '$lib/components/profile/ProfileStravaCard.svelte';

	interface Props {
		open: boolean;
		onClose: () => void;
	}

	let { open, onClose }: Props = $props();

	let confirmDisconnect = $state(false);
	let stravaComingSoon = $state(false);

	function handleStravaConnect(): void {
		stravaComingSoon = true;
		setTimeout(() => { stravaComingSoon = false; }, 4000);
	}

	async function handleStravaDisconnect(): Promise<void> {
		await stravaStore.disconnect();
		confirmDisconnect = false;
	}

	function openSupport(): void {
		const tg = getTelegramWebApp();
		if (tg) {
			tg.openTelegramLink('https://t.me/critiq1');
		} else {
			window.open('https://t.me/critiq1', '_blank');
		}
	}

	function openDesktopVersion(): void {
		const tg = getTelegramWebApp();
		if (tg) {
			tg.openLink('https://dev.critiqal.xyz');
		} else {
			window.open('https://dev.critiqal.xyz', '_blank');
		}
	}

	async function handleLogout(): Promise<void> {
		await authStore.logout();
		onClose();
		goto('/');
	}

	function handleClose(): void {
		confirmDisconnect = false;
		stravaComingSoon = false;
		onClose();
	}
</script>

<Sheet open={open} onclose={handleClose} title="Settings" maxHeight="auto">
	<div class="settings-body">
		<button class="settings-row settings-row-link" onclick={openSupport}>
			<span>Support</span>
			<svg
				width="16"
				height="16"
				viewBox="0 0 24 24"
				fill="none"
				stroke="currentColor"
				stroke-width="2"
				stroke-linecap="round"
				stroke-linejoin="round"
				aria-hidden="true"
			>
				<path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
				<polyline points="15 3 21 3 21 9" />
				<line x1="10" y1="14" x2="21" y2="3" />
			</svg>
		</button>
		<button class="settings-row settings-row-link" onclick={openDesktopVersion}>
			<span>Desktop Version</span>
			<svg
				width="16"
				height="16"
				viewBox="0 0 24 24"
				fill="none"
				stroke="currentColor"
				stroke-width="2"
				stroke-linecap="round"
				stroke-linejoin="round"
				aria-hidden="true"
			>
				<path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
				<polyline points="15 3 21 3 21 9" />
				<line x1="10" y1="14" x2="21" y2="3" />
			</svg>
		</button>
		<ProfileStravaCard
			{confirmDisconnect}
			{stravaComingSoon}
			onConnect={handleStravaConnect}
			onDisconnect={handleStravaDisconnect}
			onConfirmDisconnectChange={(v) => { confirmDisconnect = v; }}
		/>
		<button class="settings-row settings-row-danger" onclick={handleLogout}>Log out</button>
		<div class="settings-row settings-row-info" aria-label="App version">
			<span class="settings-row-label">Version</span>
			<span class="settings-row-value">Critiqal v0.1</span>
		</div>
	</div>
</Sheet>

<style>
	.settings-body {
		display: flex;
		flex-direction: column;
		overflow-y: auto;
		-webkit-overflow-scrolling: touch;
		padding-bottom: calc(16px + env(safe-area-inset-bottom, 0px));
	}

	.settings-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 16px;
		font-size: 16px;
		font-family: inherit;
		cursor: pointer;
		border: none;
		background: none;
		width: 100%;
		text-align: left;
		border-top: 1px solid var(--color-border, rgba(255, 255, 255, 0.08));
	}

	.settings-row:first-child { border-top: none; }

	.settings-row-link {
		color: var(--color-text-primary, #f0f0f0);
		font-weight: 400;
	}

	.settings-row-link:active { background: rgba(255, 255, 255, 0.05); }

	.settings-row-danger {
		color: #e05252;
		font-weight: 500;
	}

	.settings-row-danger:active { background: rgba(224, 82, 82, 0.08); }

	.settings-row-info { cursor: default; }

	.settings-row-label {
		font-size: 14px;
		color: rgba(255, 255, 255, 0.5);
	}

	.settings-row-value {
		font-size: 14px;
		color: rgba(255, 255, 255, 0.35);
	}
</style>
