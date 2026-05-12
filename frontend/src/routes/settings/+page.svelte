<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { fly, slide } from 'svelte/transition';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { stravaStore } from '$lib/stores/strava.store.svelte';
	import { apiClient } from '$lib/api/client';
	import type { User } from '$lib/types';
	import { twoFactorService } from '$lib/services/two-factor.service';
	import { emailVerificationService } from '$lib/services/email-verification.service';
	import { recoveryService } from '$lib/services/recovery.service';
	import { ApiError } from '$lib/types';
	import type { TotpSetupResponse, TwoFactorStatusResponse } from '$lib/types';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';

	// ── Email ────────────────────────────────────────────────────────────────

	let emailInput = $state('');
	let emailEditing = $state(false);
	let emailSubmitting = $state(false);
	let emailError = $state('');
	let emailSuccess = $state(false);

	function startEmailEdit(): void {
		emailInput = '';
		emailEditing = true;
		emailError = '';
		emailSuccess = false;
	}

	function cancelEmailEdit(): void {
		emailEditing = false;
		emailInput = '';
		emailError = '';
	}

	async function handleSetEmail(): Promise<void> {
		emailSubmitting = true;
		emailError = '';
		emailSuccess = false;
		try {
			await emailVerificationService.setEmail({ email: emailInput });
			emailSuccess = true;
			emailInput = '';
			emailEditing = false;
			const fresh = await apiClient.get<User>('/api/auth/me');
			authStore.updateUser(fresh);
		} catch (err: unknown) {
			emailError = mapError(err);
		} finally {
			emailSubmitting = false;
		}
	}

	async function handleResendVerification(): Promise<void> {
		const pending = authStore.user?.pendingEmail;
		if (!pending) return;
		emailSubmitting = true;
		emailError = '';
		emailSuccess = false;
		try {
			await emailVerificationService.setEmail({ email: pending });
			emailSuccess = true;
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
				<p class="section-label">Profile</p>
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
			<p class="section-label">Email</p>

			<!-- Verified email row -->
			{#if authStore.user?.email && authStore.user.emailVerified}
				<div class="setting-row">
					<div class="setting-info">
						<span class="setting-value">{authStore.user.email}</span>
						<span class="status-dot verified" title="Verified"></span>
					</div>
					{#if !emailEditing}
						<button class="link-btn" onclick={startEmailEdit}>Change</button>
					{/if}
				</div>
			{/if}

			<!-- Pending email row -->
			{#if authStore.user?.pendingEmail}
				<div class="setting-row" in:fly={{ y: -4, duration: 180 }}>
					<div class="setting-info col">
						<div class="setting-info">
							<span class="setting-value">{authStore.user.pendingEmail}</span>
							<span class="badge-pending">Pending</span>
						</div>
						<span class="setting-sub">Check your inbox for the verification link</span>
					</div>
					{#if !emailEditing}
						<button class="link-btn" disabled={emailSubmitting} onclick={handleResendVerification}>
							{emailSubmitting ? '…' : 'Resend'}
						</button>
					{/if}
				</div>
			{/if}

			<!-- No email at all -->
			{#if !authStore.user?.email && !authStore.user?.pendingEmail && !emailEditing}
				<div class="setting-row">
					<span class="setting-placeholder">No email set</span>
					<button class="link-btn" onclick={startEmailEdit}>Add</button>
				</div>
			{/if}

			{#if emailSuccess}
				<p class="hint hint-ok" in:fly={{ y: -4, duration: 150 }}>Verification email sent — check your inbox.</p>
			{/if}

			{#if emailError && !emailEditing}
				<p class="hint hint-err" in:fly={{ y: -4, duration: 150 }}>{emailError}</p>
			{/if}

			{#if emailEditing}
				<div class="inline-form" transition:slide={{ duration: 180 }}>
					{#if emailError}
						<p class="hint hint-err">{emailError}</p>
					{/if}
					<div class="input-row">
						<input
							type="email"
							class="input"
							bind:value={emailInput}
							autocomplete="email"
							required
							disabled={emailSubmitting}
							placeholder="new@example.com"
						/>
						<button
							class="btn-primary"
							disabled={emailSubmitting || emailInput.trim().length === 0}
							onclick={handleSetEmail}
						>
							{emailSubmitting ? 'Saving…' : 'Save'}
						</button>
						<button class="btn-ghost" disabled={emailSubmitting} onclick={cancelEmailEdit}>
							Cancel
						</button>
					</div>
				</div>
			{/if}
		</section>

		<!-- 2FA -->
		<section class="section">
			<div class="setting-row">
				<div class="setting-info col">
					<p class="section-label">Two-factor authentication</p>
					{#if !tfaLoading}
						<span class="setting-sub">
							{#if tfaStatus?.enabled}
								Enabled · {tfaCodesCount ?? '…'} recovery codes
							{:else}
								Add an extra layer of security
							{/if}
						</span>
					{/if}
				</div>
				{#if !tfaLoading && tfaView === 'idle'}
					{#if tfaStatus?.enabled}
						<span class="status-badge enabled">On</span>
					{:else}
						<button class="link-btn" disabled={tfaSubmitting} onclick={handleSetupTfa}>
							{tfaSubmitting ? 'Setting up…' : 'Enable'}
						</button>
					{/if}
				{/if}
			</div>

			{#if tfaError}
				<p class="hint hint-err" in:fly={{ y: -4, duration: 150 }}>{tfaError}</p>
			{/if}

			{#if tfaLoading}
				<div class="skeleton"></div>

			{:else if tfaView === 'setup-qr' && tfaSetup}
				<div class="tfa-panel" in:fly={{ y: 6, duration: 200 }}>
					<p class="hint">Scan with Google Authenticator, Authy, or any TOTP app.</p>
					<img class="qr" src={tfaSetup.qrCodeUri} alt="QR code" />
					<details class="manual-entry">
						<summary>Can't scan?</summary>
						<code class="secret">{tfaSetup.secret}</code>
					</details>
					<div class="input-row">
						<input
							type="text"
							class="input"
							bind:value={tfaConfirmCode}
							inputmode="numeric"
							maxlength={6}
							placeholder="6-digit code"
							autocomplete="one-time-code"
							disabled={tfaSubmitting}
						/>
						<button class="btn-primary" disabled={tfaSubmitting || tfaConfirmCode.trim().length !== 6} onclick={handleConfirmTfa}>
							{tfaSubmitting ? 'Confirming…' : 'Confirm'}
						</button>
						<button class="btn-ghost" disabled={tfaSubmitting} onclick={() => { tfaView = 'idle'; tfaSetup = null; }}>
							Cancel
						</button>
					</div>
				</div>

			{:else if tfaView === 'show-codes' || tfaView === 'regen-codes'}
				<div class="tfa-panel" in:fly={{ y: 6, duration: 200 }}>
					<p class="hint hint-warn">Save these codes — each works once if you lose your authenticator.</p>
					<ul class="codes-grid">
						{#each tfaRecoveryCodes as code}
							<li class="code">{code}</li>
						{/each}
					</ul>
					<button class="btn-primary" style="align-self: flex-start" onclick={tfaView === 'regen-codes' ? () => { tfaRecoveryCodes = []; tfaView = 'idle'; loadTfaStatus(); } : handleDoneWithCodes}>
						Done, I've saved them
					</button>
				</div>

			{:else if tfaView === 'disable-confirm'}
				<div class="tfa-panel" in:fly={{ y: 6, duration: 200 }}>
					<p class="hint">Enter your 6-digit code to confirm disabling 2FA.</p>
					<div class="input-row">
						<input
							type="text"
							class="input"
							bind:value={tfaDisableCode}
							inputmode="numeric"
							maxlength={6}
							placeholder="6-digit code"
							autocomplete="one-time-code"
							disabled={tfaSubmitting}
						/>
						<button class="btn-danger" disabled={tfaSubmitting || tfaDisableCode.trim().length !== 6} onclick={handleDisableTfa}>
							{tfaSubmitting ? 'Disabling…' : 'Disable'}
						</button>
						<button class="btn-ghost" disabled={tfaSubmitting} onclick={() => { tfaView = 'idle'; tfaDisableCode = ''; }}>
							Cancel
						</button>
					</div>
				</div>

			{:else if tfaStatus?.enabled && tfaView === 'idle'}
				<div class="tfa-actions">
					<button class="link-btn" disabled={tfaSubmitting} onclick={handleRegenCodes}>
						Regenerate codes
					</button>
					<span class="dot-sep" aria-hidden="true">·</span>
					<button class="link-btn danger" onclick={() => { tfaView = 'disable-confirm'; tfaError = ''; }}>
						Disable
					</button>
				</div>
			{/if}
		</section>

		<!-- Integrations -->
		<section class="section">
			<p class="section-label">Integrations</p>

			<div class="setting-row">
				<div class="setting-info col">
					<span class="setting-value">Strava</span>
					{#if stravaStore.connection}
						<span class="setting-sub">{stravaStore.connection.firstname} {stravaStore.connection.lastname}</span>
					{:else}
						<span class="setting-sub">Connect your activities</span>
					{/if}
				</div>

				{#if stravaStore.connection}
					{#if confirmDisconnect}
						<div class="inline-actions">
							<button class="link-btn danger" disabled={stravaStore.loading} onclick={handleStravaDisconnect}>
								{stravaStore.loading ? '…' : 'Disconnect'}
							</button>
							<button class="link-btn" disabled={stravaStore.loading} onclick={() => { confirmDisconnect = false; }}>
								Cancel
							</button>
						</div>
					{:else}
						<button class="link-btn" onclick={() => { confirmDisconnect = true; }}>Disconnect</button>
					{/if}
				{:else if stravaComingSoon}
					<span class="setting-sub" in:fly={{ x: 4, duration: 150 }}>Coming soon</span>
				{:else}
					<button class="link-btn" onclick={handleStravaConnect}>Connect</button>
				{/if}
			</div>
		</section>

		<!-- Account -->
		<section class="section section-last">
			<button class="link-btn danger" onclick={handleLogout}>Sign out</button>
		</section>
	</main>

	<aside class="col-right" aria-hidden="true"></aside>
</div>

<style>
	/* ── Layout ───────────────────────────────────────────────────────────── */

	.page-layout {
		display: grid;
		grid-template-columns: 16rem 42rem;
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
		display: none;
	}

	@media (max-width: 900px) {
		.page-layout { grid-template-columns: 4.5rem 1fr; }
	}

	@media (max-width: 640px) {
		.page-layout { grid-template-columns: 1fr; }
		.col-left { display: none; }
		.col-center { padding: 0 1rem 4rem; }
	}

	/* ── Header ───────────────────────────────────────────────────────────── */

	.page-header {
		padding: 1.25rem 0;
		position: sticky;
		top: 0;
		background: rgba(12, 12, 12, 0.85);
		backdrop-filter: blur(12px);
		-webkit-backdrop-filter: blur(12px);
		z-index: 10;
	}

	.page-title {
		font-size: 1rem;
		font-weight: 600;
		color: var(--color-text-primary);
		letter-spacing: -0.015em;
		margin: 0;
	}

	/* ── Sections ─────────────────────────────────────────────────────────── */

	.section {
		padding: 1.25rem 0;
		border-bottom: 1px solid var(--color-border);
		display: flex;
		flex-direction: column;
		gap: 0.625rem;
	}

	.section-last {
		border-bottom: none;
	}

	.section-label {
		font-size: 0.75rem;
		font-weight: 500;
		color: var(--color-text-muted);
		margin: 0;
		letter-spacing: 0.02em;
	}

	/* ── Setting row ──────────────────────────────────────────────────────── */

	.setting-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 1rem;
	}

	.setting-info {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		min-width: 0;
	}

	.setting-info.col {
		flex-direction: column;
		align-items: flex-start;
		gap: 0.125rem;
	}

	.setting-value {
		font-size: 0.9375rem;
		color: var(--color-text-primary);
		font-weight: 400;
	}

	.setting-placeholder {
		font-size: 0.9375rem;
		color: var(--color-text-muted);
	}

	.setting-sub {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
	}

	/* ── Profile row ──────────────────────────────────────────────────────── */

	.profile-row {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		text-decoration: none;
		padding: 0.25rem 0;
		transition: opacity 0.15s ease;
	}

	.profile-row:hover { opacity: 0.7; }

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
		font-size: 0.875rem;
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
		width: 0.875rem;
		height: 0.875rem;
		color: var(--color-text-muted);
		flex-shrink: 0;
		opacity: 0.5;
	}

	/* ── Status indicators ────────────────────────────────────────────────── */

	.status-dot {
		width: 0.5rem;
		height: 0.5rem;
		border-radius: 50%;
		flex-shrink: 0;
	}

	.status-dot.verified { background: #10b981; }

	.status-badge {
		font-size: 0.6875rem;
		font-weight: 600;
		padding: 0.1875rem 0.5rem;
		border-radius: 999px;
		letter-spacing: 0.02em;
	}

	.status-badge.enabled {
		background: rgba(16, 185, 129, 0.1);
		color: #10b981;
	}

	.badge-pending {
		font-size: 0.6875rem;
		font-weight: 500;
		padding: 0.1875rem 0.5rem;
		border-radius: 999px;
		background: rgba(202, 138, 4, 0.1);
		color: #ca8a04;
	}

	/* ── Link-style buttons ───────────────────────────────────────────────── */

	.link-btn {
		background: none;
		border: none;
		padding: 0;
		font-size: 0.875rem;
		font-family: inherit;
		font-weight: 500;
		color: var(--color-text-primary);
		cursor: pointer;
		white-space: nowrap;
		transition: opacity 0.15s ease;
		flex-shrink: 0;
	}

	.link-btn:hover:not(:disabled) { opacity: 0.6; }
	.link-btn:disabled { opacity: 0.35; cursor: not-allowed; }
	.link-btn.danger { color: var(--color-accent); }

	/* ── Inline actions (for confirm patterns) ────────────────────────────── */

	.inline-actions {
		display: flex;
		align-items: center;
		gap: 0.875rem;
	}

	.tfa-actions {
		display: flex;
		align-items: center;
		gap: 0.75rem;
	}

	.dot-sep {
		color: var(--color-text-muted);
		font-size: 0.875rem;
		line-height: 1;
	}

	/* ── Inline form ──────────────────────────────────────────────────────── */

	.inline-form {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	.input-row {
		display: flex;
		gap: 0.5rem;
		align-items: center;
	}

	.input-row .input { flex: 1; }

	/* ── Inputs ───────────────────────────────────────────────────────────── */

	.input {
		background: var(--color-surface-raised);
		border: 1px solid var(--color-border);
		border-radius: 0.5rem;
		padding: 0.5rem 0.75rem;
		font-size: 0.875rem;
		color: var(--color-text-primary);
		font-family: inherit;
		outline: none;
		transition: border-color 0.15s ease;
		box-sizing: border-box;
	}

	.input::placeholder { color: var(--color-text-muted); opacity: 0.45; }
	.input:focus { border-color: rgba(240, 240, 240, 0.22); }
	.input:disabled { opacity: 0.45; cursor: not-allowed; }

	/* ── Buttons ──────────────────────────────────────────────────────────── */

	.btn-primary {
		padding: 0.5rem 0.875rem;
		border-radius: 0.5rem;
		border: none;
		background: var(--color-text-primary);
		color: var(--color-bg);
		font-size: 0.8125rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		white-space: nowrap;
		transition: opacity 0.15s ease;
	}

	.btn-primary:hover:not(:disabled) { opacity: 0.85; }
	.btn-primary:disabled { opacity: 0.35; cursor: not-allowed; }

	.btn-ghost {
		padding: 0.5rem 0.875rem;
		border-radius: 0.5rem;
		border: 1px solid var(--color-border);
		background: transparent;
		color: var(--color-text-primary);
		font-size: 0.8125rem;
		font-weight: 500;
		font-family: inherit;
		cursor: pointer;
		white-space: nowrap;
		transition: background-color 0.15s ease;
	}

	.btn-ghost:hover:not(:disabled) { background: var(--color-surface-raised); }
	.btn-ghost:disabled { opacity: 0.35; cursor: not-allowed; }

	.btn-danger {
		padding: 0.5rem 0.875rem;
		border-radius: 0.5rem;
		border: none;
		background: rgba(224, 82, 82, 0.1);
		color: var(--color-accent);
		font-size: 0.8125rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		white-space: nowrap;
		transition: background-color 0.15s ease;
	}

	.btn-danger:hover:not(:disabled) { background: rgba(224, 82, 82, 0.18); }
	.btn-danger:disabled { opacity: 0.35; cursor: not-allowed; }

	/* ── Hints / feedback ─────────────────────────────────────────────────── */

	.hint {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		margin: 0;
		line-height: 1.5;
	}

	.hint-ok { color: #10b981; }
	.hint-err { color: var(--color-accent); }
	.hint-warn { color: #ca8a04; }

	/* ── 2FA panel ────────────────────────────────────────────────────────── */

	.tfa-panel {
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}

	.qr {
		width: 8rem;
		height: 8rem;
		border-radius: 0.5rem;
		background: #fff;
		padding: 0.375rem;
	}

	.manual-entry { font-size: 0.8125rem; color: var(--color-text-muted); }
	.manual-entry summary { cursor: pointer; }

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

	/* ── Skeleton ─────────────────────────────────────────────────────────── */

	.skeleton {
		height: 1.5rem;
		width: 8rem;
		border-radius: 0.375rem;
		background: var(--color-surface-raised);
		animation: pulse 1.4s ease-in-out infinite;
	}

	@keyframes pulse {
		0%, 100% { opacity: 0.4; }
		50% { opacity: 0.8; }
	}
</style>
