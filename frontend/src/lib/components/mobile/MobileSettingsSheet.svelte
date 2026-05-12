<script lang="ts">
	import { goto } from '$app/navigation';
	import { fly, fade } from 'svelte/transition';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { stravaStore } from '$lib/stores/strava.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import { Sheet } from '$lib/ui';
	import ProfileStravaCard from '$lib/components/profile/ProfileStravaCard.svelte';
	import { twoFactorService } from '$lib/services/two-factor.service';
	import { emailVerificationService } from '$lib/services/email-verification.service';
	import { recoveryService } from '$lib/services/recovery.service';
	import { ApiError } from '$lib/types';
	import type { TotpSetupResponse, TwoFactorStatusResponse } from '$lib/types';

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

	// ── Email ───────────────────────────────────────────────────────────────

	let emailExpanded = $state(false);
	let emailInput = $state('');
	let emailSubmitting = $state(false);
	let emailError = $state('');
	let emailSuccess = $state(false);

	async function handleSetEmail(): Promise<void> {
		emailSubmitting = true;
		emailError = '';
		emailSuccess = false;
		try {
			await emailVerificationService.setEmail({ email: emailInput });
			emailSuccess = true;
		} catch (err: unknown) {
			emailError = mapMobileError(err);
		} finally {
			emailSubmitting = false;
		}
	}

	// ── 2FA ─────────────────────────────────────────────────────────────────

	type TwoFaView = 'idle' | 'setup-qr' | 'setup-confirm' | 'show-codes' | 'disable-confirm' | 'regen-codes';

	let tfaExpanded = $state(false);
	let tfaStatus = $state<TwoFactorStatusResponse | null>(null);
	let tfaLoading = $state(false);
	let tfaView = $state<TwoFaView>('idle');
	let tfaSetup = $state<TotpSetupResponse | null>(null);
	let tfaConfirmCode = $state('');
	let tfaDisableCode = $state('');
	let tfaRecoveryCodes = $state<string[]>([]);
	let tfaSubmitting = $state(false);
	let tfaError = $state('');
	let tfaCodesCount = $state<number | null>(null);

	async function loadTfaStatus(): Promise<void> {
		if (tfaLoading) return;
		tfaLoading = true;
		try {
			tfaStatus = await twoFactorService.status();
			if (tfaStatus.enabled) {
				const res = await recoveryService.getCodesCount();
				tfaCodesCount = res.activeCount;
			}
		} catch { /* non-fatal */ } finally {
			tfaLoading = false;
		}
	}

	function toggleTfa(): void {
		tfaExpanded = !tfaExpanded;
		if (tfaExpanded && tfaStatus === null) loadTfaStatus();
	}

	async function handleSetupTfa(): Promise<void> {
		tfaSubmitting = true; tfaError = '';
		try {
			tfaSetup = await twoFactorService.setup();
			tfaConfirmCode = '';
			tfaView = 'setup-qr';
		} catch (err: unknown) { tfaError = mapMobileError(err); } finally { tfaSubmitting = false; }
	}

	async function handleConfirmTfa(): Promise<void> {
		tfaSubmitting = true; tfaError = '';
		try {
			await twoFactorService.confirm({ code: tfaConfirmCode });
			tfaRecoveryCodes = tfaSetup?.recoveryCodes ?? [];
			tfaView = 'show-codes';
			tfaStatus = { enabled: true };
		} catch (err: unknown) { tfaError = mapMobileError(err); } finally { tfaSubmitting = false; }
	}

	async function handleDisableTfa(): Promise<void> {
		tfaSubmitting = true; tfaError = '';
		try {
			await twoFactorService.disable({ code: tfaDisableCode });
			tfaStatus = { enabled: false };
			tfaDisableCode = '';
			tfaCodesCount = null;
			tfaView = 'idle';
		} catch (err: unknown) { tfaError = mapMobileError(err); } finally { tfaSubmitting = false; }
	}

	async function handleRegenCodes(): Promise<void> {
		tfaSubmitting = true; tfaError = '';
		try {
			const res = await recoveryService.regenerateCodes();
			tfaRecoveryCodes = res.codes;
			tfaView = 'regen-codes';
		} catch (err: unknown) { tfaError = mapMobileError(err); } finally { tfaSubmitting = false; }
	}

	function handleDoneWithCodes(): void {
		tfaRecoveryCodes = [];
		tfaView = 'idle';
		loadTfaStatus();
	}

	function mapMobileError(err: unknown): string {
		if (err instanceof ApiError) return err.message || 'Something went wrong';
		if (err instanceof Error) return err.message;
		return 'Something went wrong';
	}

	// ── Sheet lifecycle ──────────────────────────────────────────────────────

	$effect(() => {
		if (open && tfaStatus === null) {
			loadTfaStatus();
		}
	});

	function handleClose(): void {
		confirmDisconnect = false;
		stravaComingSoon = false;
		emailExpanded = false;
		emailError = '';
		emailSuccess = false;
		tfaExpanded = false;
		tfaView = 'idle';
		tfaError = '';
		tfaSetup = null;
		tfaConfirmCode = '';
		tfaDisableCode = '';
		tfaRecoveryCodes = [];
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

		<!-- ── Email row ─────────────────────────────────────── -->
		<div class="settings-section">
			<button class="settings-row settings-row-link" onclick={() => { emailExpanded = !emailExpanded; }}>
				<span>Email</span>
				<svg
					width="16"
					height="16"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="2"
					stroke-linecap="round"
					stroke-linejoin="round"
					class="chevron"
					class:chevron-up={emailExpanded}
					aria-hidden="true"
				>
					<polyline points="6 9 12 15 18 9" />
				</svg>
			</button>
			{#if emailExpanded}
				<div class="settings-expanded" in:fly={{ y: -8, duration: 180 }} out:fade={{ duration: 100 }}>
					{#if emailError}
						<p class="mobile-error">{emailError}</p>
					{/if}
					{#if emailSuccess}
						<p class="mobile-success">Check your inbox to verify your email.</p>
					{/if}
				
					{#if authStore.user?.emailVerified && authStore.user?.email}
						<div class="email-verified-row">
							<svg width="13" height="13" viewBox="0 0 24 24" fill="none"
								stroke="currentColor" stroke-width="2.5" aria-hidden="true">
								<polyline points="20 6 9 17 4 12" />
							</svg>
							<span>{authStore.user.email}</span>
							<span class="mobile-badge mobile-badge-on">Verified</span>
						</div>
					{:else if authStore.user?.email}
						<div class="email-pending-row">
							<span>{authStore.user.email}</span>
							<span class="mobile-badge mobile-badge-pending">Pending verification</span>
						</div>
					{/if}
					<form class="mobile-form" onsubmit={(e) => { e.preventDefault(); handleSetEmail(); }}>
						<input
							type="email"
							class="mobile-input"
							bind:value={emailInput}
							placeholder="you@example.com"
							autocomplete="email"
							required
							disabled={emailSubmitting}
						/>
						<button
							type="submit"
							class="mobile-action-btn"
							disabled={emailSubmitting || emailInput.trim().length === 0}
						>
							{emailSubmitting ? 'Saving…' : 'Save'}
						</button>
					</form>
				</div>
			{/if}
		</div>

		<!-- ── 2FA row ─────────────────────────────────────────── -->
		<div class="settings-section">
			<button class="settings-row settings-row-link" onclick={toggleTfa}>
				<span>Two-Factor Auth</span>
				<div class="tfa-row-right">
					{#if tfaLoading}
						<span class="spinner-xs" aria-label="Loading"></span>
					{:else if tfaStatus}
						{#if tfaStatus.enabled}
							<span class="mobile-badge mobile-badge-on">On</span>
						{:else}
							<span class="mobile-badge mobile-badge-off">Off</span>
						{/if}
					{/if}
					<svg
						width="16"
						height="16"
						viewBox="0 0 24 24"
						fill="none"
						stroke="currentColor"
						stroke-width="2"
						stroke-linecap="round"
						stroke-linejoin="round"
						class="chevron"
						class:chevron-up={tfaExpanded}
						aria-hidden="true"
					>
						<polyline points="6 9 12 15 18 9" />
					</svg>
				</div>
			</button>

			{#if tfaExpanded}
				<div class="settings-expanded" in:fly={{ y: -8, duration: 180 }} out:fade={{ duration: 100 }}>
					{#if tfaError}
						<p class="mobile-error">{tfaError}</p>
					{/if}

					{#if tfaView === 'setup-qr' && tfaSetup}
						<div class="tfa-mobile-setup">
							<p class="mobile-hint">Scan with your authenticator app:</p>
							<img class="tfa-qr" src={tfaSetup.qrCodeUri} alt="QR code" />
							<details class="tfa-manual">
								<summary>Enter code manually</summary>
								<code class="tfa-secret">{tfaSetup.secret}</code>
							</details>
							<input
								type="text"
								class="mobile-input"
								bind:value={tfaConfirmCode}
								inputmode="numeric"
								pattern="[0-9]{6}"
								maxlength={6}
								placeholder="6-digit code"
								autocomplete="one-time-code"
								disabled={tfaSubmitting}
							/>
							<div class="mobile-btn-row">
								<button
									class="mobile-action-btn"
									disabled={tfaSubmitting || tfaConfirmCode.trim().length !== 6}
									onclick={handleConfirmTfa}
								>
									{tfaSubmitting ? 'Confirming…' : 'Confirm'}
								</button>
								<button
									class="mobile-ghost-btn"
									disabled={tfaSubmitting}
									onclick={() => { tfaView = 'idle'; tfaSetup = null; }}
								>
									Cancel
								</button>
							</div>
						</div>

					{:else if tfaView === 'show-codes' || tfaView === 'regen-codes'}
						<div class="tfa-mobile-codes">
							<p class="mobile-hint mobile-hint-warn">Save these codes — each works once if you lose your app.</p>
							<ul class="mobile-codes-grid">
								{#each tfaRecoveryCodes as code}
									<li class="mobile-code">{code}</li>
								{/each}
							</ul>
							<button class="mobile-action-btn" onclick={handleDoneWithCodes}>I've saved these</button>
						</div>

					{:else if tfaView === 'disable-confirm'}
						<div class="tfa-mobile-disable">
							<input
								type="text"
								class="mobile-input"
								bind:value={tfaDisableCode}
								inputmode="numeric"
								pattern="[0-9]{6}"
								maxlength={6}
								placeholder="6-digit code to disable"
								autocomplete="one-time-code"
								disabled={tfaSubmitting}
							/>
							<div class="mobile-btn-row">
								<button
									class="mobile-action-btn mobile-action-btn-danger"
									disabled={tfaSubmitting || tfaDisableCode.trim().length !== 6}
									onclick={handleDisableTfa}
								>
									{tfaSubmitting ? 'Disabling…' : 'Disable 2FA'}
								</button>
								<button
									class="mobile-ghost-btn"
									disabled={tfaSubmitting}
									onclick={() => { tfaView = 'idle'; tfaDisableCode = ''; }}
								>
									Cancel
								</button>
							</div>
						</div>

					{:else if tfaStatus?.enabled}
						<div class="tfa-mobile-enabled">
							{#if tfaCodesCount !== null}
								<p class="mobile-hint">{tfaCodesCount} recovery code{tfaCodesCount !== 1 ? 's' : ''} remaining</p>
							{/if}
							<div class="mobile-btn-row">
								<button
									class="mobile-ghost-btn"
									disabled={tfaSubmitting}
									onclick={handleRegenCodes}
								>
									{tfaSubmitting ? 'Regenerating…' : 'Regenerate codes'}
								</button>
								<button
									class="mobile-ghost-btn mobile-ghost-btn-danger"
									onclick={() => { tfaView = 'disable-confirm'; tfaError = ''; }}
								>
									Disable 2FA
								</button>
							</div>
						</div>

					{:else}
						<button class="mobile-action-btn" disabled={tfaSubmitting} onclick={handleSetupTfa}>
							{tfaSubmitting ? 'Setting up…' : 'Enable 2FA'}
						</button>
					{/if}
				</div>
			{/if}
		</div>

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

	/* ── Accordion sections ──────────────────────────────────────────────── */

	.settings-section {
		border-top: 1px solid var(--color-border, rgba(255, 255, 255, 0.08));
	}

	.settings-section .settings-row {
		border-top: none;
	}

	.settings-expanded {
		padding: 0 16px 16px;
		display: flex;
		flex-direction: column;
		gap: 10px;
		border-top: 1px solid var(--color-border, rgba(255, 255, 255, 0.06));
	}

	/* ── Chevron ──────────────────────────────────────────────────────────── */

	.chevron {
		transition: transform 0.2s ease;
		flex-shrink: 0;
	}

	.chevron-up {
		transform: rotate(180deg);
	}

	/* ── 2FA row right ────────────────────────────────────────────────────── */

	.tfa-row-right {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	/* ── Badges ───────────────────────────────────────────────────────────── */

	.mobile-badge {
		display: inline-flex;
		align-items: center;
		padding: 2px 8px;
		border-radius: 999px;
		font-size: 11px;
		font-weight: 600;
		letter-spacing: 0.02em;
	}

	.mobile-badge-on {
		background: rgba(16, 185, 129, 0.12);
		color: #10b981;
	}

	.mobile-badge-off {
		background: rgba(240, 240, 240, 0.06);
		color: rgba(255, 255, 255, 0.4);
	}

	/* ── Spinner ──────────────────────────────────────────────────────────── */

	.spinner-xs {
		display: inline-block;
		width: 12px;
		height: 12px;
		border: 2px solid rgba(255, 255, 255, 0.15);
		border-top-color: rgba(255, 255, 255, 0.5);
		border-radius: 50%;
		animation: spin 0.7s linear infinite;
	}

	@keyframes spin {
		to { transform: rotate(360deg); }
	}

	/* ── Forms ────────────────────────────────────────────────────────────── */

	.mobile-form {
		display: flex;
		gap: 8px;
	}

	.mobile-form .mobile-input {
		flex: 1;
	}

	.mobile-input {
		width: 100%;
		background: var(--color-surface-raised, #1a1a1a);
		border: 1px solid var(--color-border, rgba(255, 255, 255, 0.1));
		border-radius: 8px;
		padding: 10px 12px;
		font-size: 15px;
		color: var(--color-text-primary, #f0f0f0);
		font-family: inherit;
		outline: none;
		box-sizing: border-box;
	}

	.mobile-input:focus {
		border-color: rgba(240, 240, 240, 0.3);
	}

	.mobile-input:disabled { opacity: 0.5; cursor: not-allowed; }

	/* ── Buttons ──────────────────────────────────────────────────────────── */

	.mobile-action-btn {
		padding: 10px 16px;
		border-radius: 8px;
		border: none;
		background: var(--color-text-primary, #f0f0f0);
		color: var(--color-bg, #0a0a0a);
		font-size: 14px;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		white-space: nowrap;
		-webkit-tap-highlight-color: transparent;
	}

	.mobile-action-btn:disabled { opacity: 0.5; cursor: not-allowed; }

	.mobile-action-btn-danger {
		background: rgba(224, 82, 82, 0.9);
		color: #fff;
	}

	.mobile-ghost-btn {
		padding: 10px 16px;
		border-radius: 8px;
		border: 1px solid var(--color-border, rgba(255, 255, 255, 0.12));
		background: none;
		color: var(--color-text-primary, #f0f0f0);
		font-size: 14px;
		font-family: inherit;
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
	}

	.mobile-ghost-btn-danger {
		border-color: rgba(224, 82, 82, 0.3);
		color: #e05252;
	}

	.mobile-btn-row {
		display: flex;
		gap: 8px;
		flex-wrap: wrap;
	}

	/* ── Feedback ─────────────────────────────────────────────────────────── */

	.mobile-hint {
		font-size: 13px;
		color: rgba(255, 255, 255, 0.5);
		margin: 0;
		line-height: 1.4;
	}

	.mobile-hint-warn { color: #eab308; }

	.mobile-error {
		font-size: 13px;
		color: #e05252;
		margin: 0;
	}

	.mobile-success {
		font-size: 13px;
		color: #10b981;
		margin: 0;
	}

	/* ── 2FA views ────────────────────────────────────────────────────────── */

	.tfa-mobile-setup,
	.tfa-mobile-codes,
	.tfa-mobile-disable,
	.tfa-mobile-enabled {
		display: flex;
		flex-direction: column;
		gap: 10px;
	}

	.tfa-qr {
		width: 8rem;
		height: 8rem;
		border-radius: 8px;
		align-self: center;
		background: #fff;
		padding: 6px;
	}

	.tfa-manual {
		font-size: 13px;
	}

	.tfa-manual summary {
		color: rgba(255, 255, 255, 0.5);
		cursor: pointer;
	}

	.tfa-secret {
		display: block;
		margin-top: 6px;
		font-family: 'Courier New', monospace;
		font-size: 12px;
		color: var(--color-text-primary, #f0f0f0);
		background: var(--color-surface-raised, #1a1a1a);
		border: 1px solid var(--color-border, rgba(255, 255, 255, 0.1));
		border-radius: 6px;
		padding: 8px 10px;
		word-break: break-all;
		letter-spacing: 0.04em;
	}

	.mobile-codes-grid {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 6px;
		list-style: none;
		padding: 0;
		margin: 0;
	}

	.mobile-code {
		font-family: 'Courier New', monospace;
		font-size: 12px;
		color: var(--color-text-primary, #f0f0f0);
		background: var(--color-surface-raised, #1a1a1a);
		border: 1px solid var(--color-border, rgba(255, 255, 255, 0.1));
		border-radius: 6px;
		padding: 6px 8px;
		letter-spacing: 0.04em;
	}
</style>
