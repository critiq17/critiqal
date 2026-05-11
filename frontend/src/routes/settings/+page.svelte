<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { fly, fade } from 'svelte/transition';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { stravaStore } from '$lib/stores/strava.store.svelte';
	import { twoFactorService } from '$lib/services/two-factor.service';
	import { emailVerificationService } from '$lib/services/email-verification.service';
	import { recoveryService } from '$lib/services/recovery.service';
	import { ApiError } from '$lib/types';
	import type { TotpSetupResponse, TwoFactorStatusResponse } from '$lib/types';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';

	// ── Email ────────────────────────────────────────────────────────────────

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
			emailInput = '';
		} catch (err: unknown) {
			emailError = mapError(err);
		} finally {
			emailSubmitting = false;
		}
	}

	// ── 2FA ──────────────────────────────────────────────────────────────────

	type TwoFaView = 'idle' | 'setup-qr' | 'show-codes' | 'disable-confirm' | 'regen-codes';

	let tfaStatus = $state<TwoFactorStatusResponse | null>(null);
	let tfaLoading = $state(true);
	let tfaError = $state('');
	let tfaSubmitting = $state(false);
	let tfaView = $state<TwoFaView>('idle');
	let tfaSetup = $state<TotpSetupResponse | null>(null);
	let tfaConfirmCode = $state('');
	let tfaDisableCode = $state('');
	let tfaRecoveryCodes = $state<string[]>([]);
	let tfaCodesCount = $state<number | null>(null);

	async function loadTfaStatus(): Promise<void> {
		tfaLoading = true;
		try {
			tfaStatus = await twoFactorService.status();
			if (tfaStatus.enabled) {
				const r = await recoveryService.getCodesCount();
				tfaCodesCount = r.activeCount;
			}
		} catch { /* non-fatal */ } finally {
			tfaLoading = false;
		}
	}

	async function handleSetupTfa(): Promise<void> {
		tfaSubmitting = true; tfaError = '';
		try {
			tfaSetup = await twoFactorService.setup();
			tfaConfirmCode = '';
			tfaView = 'setup-qr';
		} catch (err) { tfaError = mapError(err); } finally { tfaSubmitting = false; }
	}

	async function handleConfirmTfa(): Promise<void> {
		tfaSubmitting = true; tfaError = '';
		try {
			await twoFactorService.confirm({ code: tfaConfirmCode });
			tfaRecoveryCodes = tfaSetup?.recoveryCodes ?? [];
			tfaView = 'show-codes';
			tfaStatus = { enabled: true };
		} catch (err) { tfaError = mapError(err); } finally { tfaSubmitting = false; }
	}

	function handleDoneWithCodes(): void {
		tfaSetup = null; tfaConfirmCode = ''; tfaRecoveryCodes = [];
		tfaView = 'idle'; loadTfaStatus();
	}

	async function handleDisableTfa(): Promise<void> {
		tfaSubmitting = true; tfaError = '';
		try {
			await twoFactorService.disable({ code: tfaDisableCode });
			tfaStatus = { enabled: false };
			tfaDisableCode = ''; tfaCodesCount = null; tfaView = 'idle';
		} catch (err) { tfaError = mapError(err); } finally { tfaSubmitting = false; }
	}

	async function handleRegenCodes(): Promise<void> {
		tfaSubmitting = true; tfaError = '';
		try {
			const res = await recoveryService.regenerateCodes();
			tfaRecoveryCodes = res.codes;
			tfaView = 'regen-codes';
		} catch (err) { tfaError = mapError(err); } finally { tfaSubmitting = false; }
	}

	// ── Strava ───────────────────────────────────────────────────────────────

	let stravaComingSoon = $state(false);
	let confirmDisconnect = $state(false);

	function handleStravaConnect(): void {
		stravaComingSoon = true;
		setTimeout(() => { stravaComingSoon = false; }, 4000);
	}

	async function handleStravaDisconnect(): Promise<void> {
		await stravaStore.disconnect();
		confirmDisconnect = false;
	}

	// ── Account ──────────────────────────────────────────────────────────────

	async function handleLogout(): Promise<void> {
		await authStore.logout();
		goto('/');
	}

	function mapError(err: unknown): string {
		if (err instanceof ApiError) return err.message || 'Something went wrong.';
		if (err instanceof Error) return err.message;
		return 'Something went wrong.';
	}

	onMount(() => {
		loadTfaStatus();
		stravaStore.load();
	});
</script>

<svelte:head>
	<title>Settings — Critiqal</title>
	<meta name="description" content="Manage your account settings" />
</svelte:head>

<div class="page-layout">
	<div class="col-left">
		<LeftSidebar />
	</div>

	<main class="col-center">
		<header class="page-header">
			<h1 class="page-title">Settings</h1>
		</header>

		<!-- Profile -->
		{#if authStore.isAuthenticated && authStore.user}
			<section class="section">
				<p class="label">Profile</p>
				<a href="/{authStore.user.username}" class="profile-row">
					<div class="avatar" aria-hidden="true">
						{#if authStore.user.avatarUrl}
							<img src={authStore.user.avatarUrl} alt={authStore.user.username} class="avatar-img" />
						{:else}
							<span class="avatar-initial">
								{(authStore.user.name ?? authStore.user.username).charAt(0).toUpperCase()}
							</span>
						{/if}
					</div>
					<div class="profile-text">
						<span class="profile-name">{authStore.user.name ?? authStore.user.username}</span>
						<span class="profile-handle">@{authStore.user.username}</span>
					</div>
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" class="chevron" aria-hidden="true">
						<polyline points="9 18 15 12 9 6" />
					</svg>
				</a>
			</section>
		{/if}

		<!-- Email -->
		<section class="section">
			<p class="label">Email</p>
			<p class="sublabel">Used for password recovery and notifications.</p>

			{#if emailSuccess}
				<p class="msg msg-ok" in:fly={{ y: -4, duration: 150 }}>Check your inbox to verify.</p>
			{/if}
			{#if emailError}
				<p class="msg msg-err" in:fly={{ y: -4, duration: 150 }}>{emailError}</p>
			{/if}

			<form class="row-form" onsubmit={(e) => { e.preventDefault(); handleSetEmail(); }}>
				<input
					type="email"
					class="input"
					bind:value={emailInput}
					autocomplete="email"
					required
					disabled={emailSubmitting}
					placeholder="you@example.com"
				/>
				<button type="submit" class="btn-primary" disabled={emailSubmitting || emailInput.trim().length === 0}>
					{emailSubmitting ? 'Saving…' : 'Save'}
				</button>
			</form>
		</section>

		<!-- 2FA -->
		<section class="section">
			<div class="label-row">
				<p class="label">Two-factor authentication</p>
				{#if !tfaLoading && tfaStatus?.enabled}
					<span class="badge-on">Enabled</span>
				{/if}
			</div>
			<p class="sublabel">Protect your account with an authenticator app.</p>

			{#if tfaError}
				<p class="msg msg-err" in:fly={{ y: -4, duration: 150 }}>{tfaError}</p>
			{/if}

			{#if tfaLoading}
				<div class="skeleton"></div>

			{:else if tfaView === 'setup-qr' && tfaSetup}
				<div class="tfa-panel" in:fly={{ y: 6, duration: 200 }}>
					<p class="hint">Scan with your authenticator app (Google Authenticator, Authy, etc.)</p>
					<img class="qr" src={tfaSetup.qrCodeUri} alt="QR code" />
					<details class="manual">
						<summary>Can't scan? Enter manually</summary>
						<code class="secret">{tfaSetup.secret}</code>
					</details>
					<div class="field-group">
						<input
							type="text"
							class="input"
							bind:value={tfaConfirmCode}
							inputmode="numeric"
							pattern="[0-9]{6}"
							maxlength={6}
							placeholder="6-digit code"
							autocomplete="one-time-code"
							disabled={tfaSubmitting}
						/>
						<div class="btn-row">
							<button class="btn-primary" disabled={tfaSubmitting || tfaConfirmCode.trim().length !== 6} onclick={handleConfirmTfa}>
								{tfaSubmitting ? 'Confirming…' : 'Confirm'}
							</button>
							<button class="btn-ghost" disabled={tfaSubmitting} onclick={() => { tfaView = 'idle'; tfaSetup = null; }}>
								Cancel
							</button>
						</div>
					</div>
				</div>

			{:else if tfaView === 'show-codes' || tfaView === 'regen-codes'}
				<div class="tfa-panel" in:fly={{ y: 6, duration: 200 }}>
					<p class="hint hint-warn">Save these codes somewhere safe. Each works once if you lose your authenticator.</p>
					<ul class="codes-grid">
						{#each tfaRecoveryCodes as code}
							<li class="code">{code}</li>
						{/each}
					</ul>
					<button class="btn-primary" onclick={tfaView === 'regen-codes' ? () => { tfaRecoveryCodes = []; tfaView = 'idle'; loadTfaStatus(); } : handleDoneWithCodes}>
						Done, I've saved them
					</button>
				</div>

			{:else if tfaView === 'disable-confirm'}
				<div class="tfa-panel" in:fly={{ y: 6, duration: 200 }}>
					<input
						type="text"
						class="input"
						bind:value={tfaDisableCode}
						inputmode="numeric"
						pattern="[0-9]{6}"
						maxlength={6}
						placeholder="Enter your 6-digit code to confirm"
						autocomplete="one-time-code"
						disabled={tfaSubmitting}
					/>
					<div class="btn-row">
						<button class="btn-danger" disabled={tfaSubmitting || tfaDisableCode.trim().length !== 6} onclick={handleDisableTfa}>
							{tfaSubmitting ? 'Disabling…' : 'Disable 2FA'}
						</button>
						<button class="btn-ghost" disabled={tfaSubmitting} onclick={() => { tfaView = 'idle'; tfaDisableCode = ''; }}>
							Cancel
						</button>
					</div>
				</div>

			{:else if tfaStatus?.enabled}
				<div class="tfa-actions" in:fade={{ duration: 150 }}>
					{#if tfaCodesCount !== null}
						<p class="hint">{tfaCodesCount} recovery code{tfaCodesCount !== 1 ? 's' : ''} remaining</p>
					{/if}
					<div class="btn-row">
						<button class="btn-ghost" disabled={tfaSubmitting} onclick={handleRegenCodes}>
							Regenerate codes
						</button>
						<button class="btn-ghost btn-ghost-danger" onclick={() => { tfaView = 'disable-confirm'; tfaError = ''; }}>
							Disable
						</button>
					</div>
				</div>

			{:else}
				<button class="btn-primary" style="align-self: flex-start" disabled={tfaSubmitting} onclick={handleSetupTfa} in:fade={{ duration: 150 }}>
					{tfaSubmitting ? 'Setting up…' : 'Enable 2FA'}
				</button>
			{/if}
		</section>

		<!-- Integrations -->
		<section class="section">
			<p class="label">Integrations</p>

			<div class="integration-row">
				<div class="integration-left">
					<span class="integration-name">Strava</span>
					{#if stravaStore.connection}
						<span class="integration-sub">{stravaStore.connection.firstname} {stravaStore.connection.lastname}</span>
					{/if}
				</div>

				{#if stravaStore.connection}
					{#if confirmDisconnect}
						<div class="btn-row">
							<button class="btn-danger-sm" disabled={stravaStore.loading} onclick={handleStravaDisconnect}>
								{stravaStore.loading ? '…' : 'Disconnect'}
							</button>
							<button class="btn-ghost-sm" disabled={stravaStore.loading} onclick={() => { confirmDisconnect = false; }}>
								Cancel
							</button>
						</div>
					{:else}
						<button class="btn-ghost-sm" onclick={() => { confirmDisconnect = true; }}>
							Disconnect
						</button>
					{/if}
				{:else if stravaComingSoon}
					<span class="coming-soon" in:fade={{ duration: 150 }}>Coming soon</span>
				{:else}
					<button class="btn-connect" onclick={handleStravaConnect}>
						Connect
					</button>
				{/if}
			</div>
		</section>

		<!-- Account -->
		<section class="section">
			<p class="label">Account</p>
			<button class="btn-ghost btn-ghost-danger" style="align-self: flex-start" onclick={handleLogout}>
				Sign out
			</button>
		</section>
	</main>

	<aside class="col-right" aria-hidden="true"></aside>
</div>

<style>
	/* ── Layout ───────────────────────────────────────────────────────────── */

	.page-layout {
		display: grid;
		grid-template-columns: 16rem 42rem 14rem;
		justify-content: center;
		height: 100vh;
		overflow: hidden;
	}

	.col-left {
		overflow-y: auto;
		padding: 0 1.5rem 0 1rem;
	}

	.col-center {
		overflow-y: auto;
		padding: 0 2rem 6rem;
		scrollbar-width: none;
	}

	.col-center::-webkit-scrollbar { display: none; }

	.col-right {
		overflow-y: auto;
		padding: 1.5rem 1rem 1.5rem 1.5rem;
	}

	@media (max-width: 1024px) {
		.page-layout { grid-template-columns: 4.5rem 1fr; }
		.col-right { display: none; }
	}

	@media (max-width: 640px) {
		.page-layout { grid-template-columns: 1fr; }
		.col-left { display: none; }
		.col-center { padding: 0 1rem 4rem; }
	}

	/* ── Header ───────────────────────────────────────────────────────────── */

	.page-header {
		padding: 1.25rem 0;
		border-bottom: 1px solid var(--color-border);
		position: sticky;
		top: 0;
		background: var(--color-bg);
		z-index: 10;
	}

	.page-title {
		font-size: 1.0625rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.01em;
		margin: 0;
	}

	/* ── Sections ─────────────────────────────────────────────────────────── */

	.section {
		padding: 1.5rem 0;
		border-top: 1px solid var(--color-border);
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}

	.label {
		font-size: 0.8125rem;
		font-weight: 600;
		color: var(--color-text-muted);
		margin: 0;
		text-transform: uppercase;
		letter-spacing: 0.06em;
	}

	.label-row {
		display: flex;
		align-items: center;
		gap: 0.625rem;
	}

	.sublabel {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		margin: -0.25rem 0 0;
		line-height: 1.5;
	}

	/* ── Profile row ──────────────────────────────────────────────────────── */

	.profile-row {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		text-decoration: none;
		padding: 0.5rem 0;
		border-radius: 0.5rem;
		transition: opacity 0.15s ease;
	}

	.profile-row:hover { opacity: 0.75; }

	.avatar {
		width: 2.25rem;
		height: 2.25rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.avatar-img { width: 100%; height: 100%; object-fit: cover; }

	.avatar-initial {
		font-size: 0.8125rem;
		font-weight: 600;
		color: var(--color-text-muted);
		user-select: none;
	}

	.profile-text {
		display: flex;
		flex-direction: column;
		gap: 0.1rem;
		flex: 1;
		min-width: 0;
	}

	.profile-name {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text-primary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.profile-handle {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
	}

	.chevron {
		width: 1rem;
		height: 1rem;
		color: var(--color-text-muted);
		flex-shrink: 0;
	}

	/* ── Badge ────────────────────────────────────────────────────────────── */

	.badge-on {
		display: inline-flex;
		align-items: center;
		padding: 0.125rem 0.5rem;
		border-radius: 999px;
		font-size: 0.6875rem;
		font-weight: 600;
		letter-spacing: 0.02em;
		background: rgba(16, 185, 129, 0.12);
		color: #10b981;
	}

	/* ── Inputs ───────────────────────────────────────────────────────────── */

	.input {
		width: 100%;
		background: var(--color-surface-raised);
		border: 1px solid var(--color-border);
		border-radius: 0.5rem;
		padding: 0.625rem 0.75rem;
		font-size: 0.9375rem;
		color: var(--color-text-primary);
		font-family: inherit;
		transition: border-color 0.15s ease, box-shadow 0.15s ease;
		outline: none;
		box-sizing: border-box;
	}

	.input::placeholder { color: var(--color-text-muted); opacity: 0.5; }
	.input:focus { border-color: rgba(240, 240, 240, 0.25); box-shadow: 0 0 0 3px rgba(240, 240, 240, 0.04); }
	.input:disabled { opacity: 0.5; cursor: not-allowed; }

	/* ── Forms ────────────────────────────────────────────────────────────── */

	.row-form { display: flex; gap: 0.5rem; }
	.row-form .input { flex: 1; }

	.field-group { display: flex; flex-direction: column; gap: 0.625rem; }

	.btn-row { display: flex; gap: 0.5rem; flex-wrap: wrap; }

	/* ── Buttons ──────────────────────────────────────────────────────────── */

	.btn-primary {
		padding: 0.5rem 1rem;
		border-radius: 0.5rem;
		border: none;
		background: var(--color-text-primary);
		color: var(--color-bg);
		font-size: 0.875rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		white-space: nowrap;
		transition: opacity 0.15s ease, transform 0.1s ease;
	}

	.btn-primary:hover:not(:disabled) { opacity: 0.85; }
	.btn-primary:active:not(:disabled) { transform: scale(0.97); }
	.btn-primary:disabled { opacity: 0.4; cursor: not-allowed; }

	.btn-ghost {
		padding: 0.5rem 1rem;
		border-radius: 0.5rem;
		border: 1px solid var(--color-border);
		background: transparent;
		color: var(--color-text-primary);
		font-size: 0.875rem;
		font-weight: 500;
		font-family: inherit;
		cursor: pointer;
		transition: background-color 0.15s ease;
	}

	.btn-ghost:hover:not(:disabled) { background: var(--color-surface-raised); }
	.btn-ghost:disabled { opacity: 0.4; cursor: not-allowed; }

	.btn-ghost-danger { border-color: transparent; color: var(--color-accent); }
	.btn-ghost-danger:hover:not(:disabled) { background: rgba(224, 82, 82, 0.06); }

	.btn-danger {
		padding: 0.5rem 1rem;
		border-radius: 0.5rem;
		border: none;
		background: rgba(224, 82, 82, 0.12);
		color: var(--color-accent);
		font-size: 0.875rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition: background-color 0.15s ease;
	}

	.btn-danger:hover:not(:disabled) { background: rgba(224, 82, 82, 0.2); }
	.btn-danger:disabled { opacity: 0.4; cursor: not-allowed; }

	.btn-ghost-sm {
		padding: 0.375rem 0.75rem;
		border-radius: 0.375rem;
		border: 1px solid var(--color-border);
		background: transparent;
		color: var(--color-text-muted);
		font-size: 0.8125rem;
		font-family: inherit;
		cursor: pointer;
		transition: color 0.15s ease;
	}

	.btn-ghost-sm:hover:not(:disabled) { color: var(--color-text-primary); }
	.btn-ghost-sm:disabled { opacity: 0.4; cursor: not-allowed; }

	.btn-danger-sm {
		padding: 0.375rem 0.75rem;
		border-radius: 0.375rem;
		border: none;
		background: transparent;
		color: var(--color-accent);
		font-size: 0.8125rem;
		font-family: inherit;
		cursor: pointer;
	}

	.btn-danger-sm:disabled { opacity: 0.4; cursor: not-allowed; }

	.btn-connect {
		padding: 0.375rem 0.875rem;
		border-radius: 0.375rem;
		border: 1px solid var(--color-border);
		background: transparent;
		color: var(--color-text-primary);
		font-size: 0.8125rem;
		font-weight: 500;
		font-family: inherit;
		cursor: pointer;
		transition: background-color 0.15s ease;
	}

	.btn-connect:hover { background: var(--color-surface-raised); }

	/* ── Feedback ─────────────────────────────────────────────────────────── */

	.msg { font-size: 0.8125rem; margin: 0; line-height: 1.4; }
	.msg-ok { color: #10b981; }
	.msg-err { color: var(--color-accent); }

	/* ── 2FA panels ───────────────────────────────────────────────────────── */

	.tfa-panel, .tfa-actions {
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}

	.qr {
		width: 9rem;
		height: 9rem;
		border-radius: 0.5rem;
		align-self: flex-start;
		background: #fff;
		padding: 0.375rem;
	}

	.manual { font-size: 0.8125rem; color: var(--color-text-muted); }
	.manual summary { cursor: pointer; }

	.secret {
		display: block;
		margin-top: 0.375rem;
		font-family: 'Courier New', monospace;
		font-size: 0.8125rem;
		color: var(--color-text-primary);
		background: var(--color-surface-raised);
		border-radius: 0.375rem;
		padding: 0.5rem 0.75rem;
		word-break: break-all;
		letter-spacing: 0.05em;
	}

	.hint {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		margin: 0;
		line-height: 1.5;
	}

	.hint-warn { color: #ca8a04; }

	.codes-grid {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 0.375rem;
		list-style: none;
		padding: 0;
		margin: 0;
	}

	.code {
		font-family: 'Courier New', monospace;
		font-size: 0.8125rem;
		color: var(--color-text-primary);
		background: var(--color-surface-raised);
		border-radius: 0.375rem;
		padding: 0.375rem 0.625rem;
		letter-spacing: 0.04em;
	}

	/* ── Integrations ─────────────────────────────────────────────────────── */

	.integration-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
	}

	.integration-left {
		display: flex;
		flex-direction: column;
		gap: 0.125rem;
	}

	.integration-name {
		font-size: 0.9375rem;
		font-weight: 500;
		color: var(--color-text-primary);
	}

	.integration-sub {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
	}

	.coming-soon {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
	}

	/* ── Skeleton ─────────────────────────────────────────────────────────── */

	.skeleton {
		height: 1.75rem;
		width: 10rem;
		border-radius: 0.375rem;
		background: var(--color-surface-raised);
		animation: pulse 1.4s ease-in-out infinite;
	}

	@keyframes pulse {
		0%, 100% { opacity: 0.4; }
		50% { opacity: 0.8; }
	}
</style>
