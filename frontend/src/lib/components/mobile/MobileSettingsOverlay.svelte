<script lang="ts">
	import { onMount } from 'svelte';
	import { slide } from 'svelte/transition';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { stravaStore } from '$lib/stores/strava.store.svelte';
	import { closeSettings } from '$lib/stores/settings-nav.store.svelte';
	import { twoFactorService } from '$lib/services/two-factor.service';
	import { emailVerificationService } from '$lib/services/email-verification.service';
	import { recoveryService } from '$lib/services/recovery.service';
	import { getTelegramWebApp } from '$lib/telegram';
	import { goto } from '$app/navigation';
	import { apiClient } from '$lib/api/client';
	import { ApiError } from '$lib/types';
	import type { User, TotpSetupResponse, TwoFactorStatusResponse } from '$lib/types';
	import { notifyOverlaySwipe } from '$lib/overlay-swipe';

	// ── Swipe-to-dismiss ─────────────────────────────────────────────────────

	let overlayEl: HTMLElement | null = null;

	let _touchStartX = 0;
	let _touchStartY = 0;
	let _dirLocked: 'h' | 'v' | null = null;
	let _lastX = 0;
	let _lastT = 0;
	let _velocity = 0;
	let _currentX = 0;

	const DISMISS_RATIO = 0.35;
	const VELOCITY_PX_MS = 0.45;

	function _applyTransform(x: number, transition: string): void {
		if (!overlayEl) return;
		overlayEl.style.transition = transition;
		overlayEl.style.transform = `translateX(${x}px)`;
	}

	function onSwipeTouchStart(): void {
		_applyTransform(_currentX, 'none');
		_touchStartX = 0;
		_dirLocked = null;
		_velocity = 0;
	}

	function onSwipeTouchMove(e: TouchEvent): void {
		const t = e.touches[0];
		if (!t) return;

		if (!_dirLocked && _touchStartX === 0) {
			_touchStartX = t.clientX;
			_touchStartY = t.clientY;
			_lastX = t.clientX;
			_lastT = Date.now();
		}

		const dx = t.clientX - _touchStartX;
		const dy = t.clientY - _touchStartY;

		if (!_dirLocked && (Math.abs(dx) > 5 || Math.abs(dy) > 5)) {
			_dirLocked = Math.abs(dx) >= Math.abs(dy) ? 'h' : 'v';
		}

		if (_dirLocked !== 'h' || dx < 0) return;

		const now = Date.now();
		const dt = now - _lastT;
		if (dt > 0) _velocity = (t.clientX - _lastX) / dt;
		_lastX = t.clientX;
		_lastT = now;
		_currentX = dx;

		if (overlayEl) overlayEl.style.transform = `translateX(${dx}px)`;
		notifyOverlaySwipe(dx, window.innerWidth, 'drag');
	}

	function onSwipeTouchEnd(): void {
		if (_currentX <= 0) return;

		const sw = window.innerWidth;
		const threshold = sw * DISMISS_RATIO;

		if (_currentX > threshold || _velocity > VELOCITY_PX_MS) {
			getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
			_applyTransform(sw, 'transform 0.24s cubic-bezier(0.4, 0, 1, 1)');
			notifyOverlaySwipe(sw, sw, 'dismiss');
			setTimeout(() => closeSettings(), 260);
		} else {
			_applyTransform(0, 'transform 0.38s cubic-bezier(0.34, 1.56, 0.64, 1)');
			notifyOverlaySwipe(0, sw, 'cancel');
		}
		_currentX = 0;
	}

	function handleBack(): void {
		getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
		closeSettings();
	}

	// ── Telegram Back Button ─────────────────────────────────────────────────

	$effect(() => {
		const tg = getTelegramWebApp();
		if (!tg) return;
		tg.BackButton.show();
		tg.BackButton.onClick(handleBack);
		return () => {
			tg.BackButton.offClick(handleBack);
			tg.BackButton.hide();
		};
	});

	// ── Slide-in on mount ────────────────────────────────────────────────────

	onMount(() => {
		if (overlayEl) {
			overlayEl.style.transform = `translateX(${window.innerWidth}px)`;
			overlayEl.style.transition = 'none';
			requestAnimationFrame(() => {
				if (!overlayEl) return;
				overlayEl.style.transition = 'transform 0.28s cubic-bezier(0.4, 0, 0.2, 1)';
				overlayEl.style.transform = 'translateX(0)';
			});
		}
		loadTfaStatus();
		stravaStore.load();
	});

	// ── Email ────────────────────────────────────────────────────────────────

	let emailEditing = $state(false);
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
			await authStore.refresh();
			emailSuccess = true;
			emailInput = '';
			emailEditing = false;
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

	// ── External links ───────────────────────────────────────────────────────

	function openSupport(): void {
		const tg = getTelegramWebApp();
		if (tg) tg.openTelegramLink('https://t.me/critiq1');
		else window.open('https://t.me/critiq1', '_blank');
	}

	function openDesktopVersion(): void {
		const tg = getTelegramWebApp();
		if (tg) tg.openLink('https://dev.critiqal.xyz');
		else window.open('https://dev.critiqal.xyz', '_blank');
	}

	// ── Account ──────────────────────────────────────────────────────────────

	async function handleLogout(): Promise<void> {
		await authStore.logout();
		closeSettings();
		goto('/');
	}

	function mapError(err: unknown): string {
		if (err instanceof ApiError) return err.message || 'Something went wrong';
		if (err instanceof Error) return err.message;
		return 'Something went wrong';
	}
</script>

<div
	class="overlay"
	bind:this={overlayEl}
	ontouchstart={onSwipeTouchStart}
	ontouchmove={onSwipeTouchMove}
	ontouchend={onSwipeTouchEnd}
>
	<!-- Header -->
	<header class="overlay-header">
		<button class="back-btn" onclick={handleBack} aria-label="Back">
			<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
				stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
				<polyline points="15 18 9 12 15 6" />
			</svg>
		</button>
		<span class="header-title">Settings</span>
		<div class="header-spacer" aria-hidden="true"></div>
	</header>

	<div class="scroll-area mobile-scroll-container">

		<!-- Profile -->
		{#if authStore.user}
			<div class="group-label">Profile</div>
			<div class="group">
				<a href="/{authStore.user.username}" class="row row-link" onclick={closeSettings}>
					<div class="avatar">
						{#if authStore.user.avatarUrl}
							<img src={authStore.user.avatarUrl} alt={authStore.user.username} class="avatar-img" />
						{:else}
							<span class="avatar-initial">
								{(authStore.user.name ?? authStore.user.username).charAt(0).toUpperCase()}
							</span>
						{/if}
					</div>
					<div class="row-body">
						<span class="row-title">{authStore.user.name ?? authStore.user.username}</span>
						<span class="row-sub">@{authStore.user.username}</span>
					</div>
					<svg class="row-chevron" width="16" height="16" viewBox="0 0 24 24" fill="none"
						stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
						<polyline points="9 18 15 12 9 6" />
					</svg>
				</a>
			</div>
		{/if}

		<!-- Account Security -->
		<div class="group-label">Account</div>
		<div class="group">

			<!-- Email row -->
			<div class="row-section">
				<button class="row row-tappable" onclick={() => { emailEditing = !emailEditing; emailError = ''; emailSuccess = false; }}>
					<div class="row-icon">
						<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
							stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
							<path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
							<polyline points="22,6 12,13 2,6"/>
						</svg>
					</div>
					<div class="row-body">
						<span class="row-title">Email</span>
						{#if authStore.user?.email && authStore.user.emailVerified}
							<span class="row-sub">{authStore.user.email}</span>
						{:else if authStore.user?.pendingEmail}
							<span class="row-sub">{authStore.user.pendingEmail}</span>
						{:else}
							<span class="row-sub dim">Not set</span>
						{/if}
					</div>
					<div class="row-right">
						{#if authStore.user?.emailVerified}
							<span class="status-pill green">Verified</span>
						{:else if authStore.user?.pendingEmail}
							<span class="status-pill amber">Pending</span>
						{/if}
						<svg class="row-chevron" class:rotated={emailEditing} width="14" height="14" viewBox="0 0 24 24"
							fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
							<polyline points="6 9 12 15 18 9" />
						</svg>
					</div>
				</button>

				{#if emailEditing}
					<div class="row-expanded" transition:slide={{ duration: 200 }}>
						{#if emailError}
							<p class="feedback err">{emailError}</p>
						{/if}
						{#if emailSuccess}
							<p class="feedback ok">Verification email sent — check your inbox.</p>
						{/if}
						{#if authStore.user?.pendingEmail && !emailSuccess}
							<p class="feedback muted">Waiting for confirmation at <strong>{authStore.user.pendingEmail}</strong></p>
							<div class="form-actions">
								<button class="m-btn ghost" disabled={emailSubmitting} onclick={handleResendVerification}>
									{emailSubmitting ? 'Sending…' : 'Resend email'}
								</button>
								<button class="m-btn ghost" disabled={emailSubmitting}
									onclick={() => { emailEditing = false; emailError = ''; }}>
									Cancel
								</button>
							</div>
						{:else if !emailSuccess}
							<div class="inline-form">
								<input
									type="email"
									class="m-input"
									bind:value={emailInput}
									placeholder={authStore.user?.email && authStore.user.emailVerified ? 'New email address' : 'you@example.com'}
									autocomplete="email"
									disabled={emailSubmitting}
								/>
								<div class="form-actions">
									<button class="m-btn primary" disabled={emailSubmitting || !emailInput.trim()}
										onclick={handleSetEmail}>
										{emailSubmitting ? 'Saving…' : 'Save'}
									</button>
									<button class="m-btn ghost" disabled={emailSubmitting}
										onclick={() => { emailEditing = false; emailInput = ''; emailError = ''; }}>
										Cancel
									</button>
								</div>
							</div>
						{/if}
					</div>
				{/if}
			</div>

			<div class="row-divider"></div>

			<!-- 2FA row -->
			<div class="row-section">
				<button class="row row-tappable" onclick={() => {
					if (tfaView !== 'idle') { tfaView = 'idle'; return; }
					if (!tfaStatus?.enabled) handleSetupTfa();
				}}>
					<div class="row-icon">
						<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
							stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
							<rect x="5" y="11" width="14" height="10" rx="2"/>
							<path d="M8 11V7a4 4 0 018 0v4"/>
						</svg>
					</div>
					<div class="row-body">
						<span class="row-title">Two-factor auth</span>
						{#if tfaLoading}
							<span class="row-sub dim">Loading…</span>
						{:else if tfaStatus?.enabled && tfaCodesCount !== null}
							<span class="row-sub">{tfaCodesCount} recovery codes</span>
						{:else if !tfaStatus?.enabled}
							<span class="row-sub dim">Not enabled</span>
						{/if}
					</div>
					<div class="row-right">
						{#if tfaLoading}
							<span class="spinner-xs" aria-label="Loading"></span>
						{:else if tfaStatus?.enabled}
							<span class="status-pill green">On</span>
						{:else}
							<span class="status-pill muted">Off</span>
						{/if}
					</div>
				</button>

				{#if tfaError}
					<div class="row-expanded">
						<p class="feedback err">{tfaError}</p>
					</div>
				{/if}

				{#if !tfaLoading && tfaView === 'setup-qr' && tfaSetup}
					<div class="row-expanded" transition:slide={{ duration: 200 }}>
						<p class="feedback muted">Scan with Google Authenticator, Authy, or any TOTP app.</p>
						<img class="qr" src={tfaSetup.qrCodeUri} alt="QR code" />
						<details class="manual-entry">
							<summary>Can't scan?</summary>
							<code class="tfa-secret">{tfaSetup.secret}</code>
						</details>
						<div class="inline-form">
							<input type="text" class="m-input" bind:value={tfaConfirmCode}
								inputmode="numeric" maxlength={6} placeholder="6-digit code"
								autocomplete="one-time-code" disabled={tfaSubmitting} />
							<div class="form-actions">
								<button class="m-btn primary"
									disabled={tfaSubmitting || tfaConfirmCode.trim().length !== 6}
									onclick={handleConfirmTfa}>
									{tfaSubmitting ? 'Confirming…' : 'Confirm'}
								</button>
								<button class="m-btn ghost" disabled={tfaSubmitting}
									onclick={() => { tfaView = 'idle'; tfaSetup = null; }}>
									Cancel
								</button>
							</div>
						</div>
					</div>

				{:else if !tfaLoading && (tfaView === 'show-codes' || tfaView === 'regen-codes')}
					<div class="row-expanded" transition:slide={{ duration: 200 }}>
						<p class="feedback amber">Save these — each code works once if you lose your app.</p>
						<ul class="codes-grid">
							{#each tfaRecoveryCodes as code}
								<li class="code-chip">{code}</li>
							{/each}
						</ul>
						<button class="m-btn primary" style="align-self:flex-start"
							onclick={tfaView === 'regen-codes'
								? () => { tfaRecoveryCodes = []; tfaView = 'idle'; loadTfaStatus(); }
								: handleDoneWithCodes}>
							Done, I've saved them
						</button>
					</div>

				{:else if !tfaLoading && tfaView === 'disable-confirm'}
					<div class="row-expanded" transition:slide={{ duration: 200 }}>
						<p class="feedback muted">Enter your 6-digit code to confirm.</p>
						<div class="inline-form">
							<input type="text" class="m-input" bind:value={tfaDisableCode}
								inputmode="numeric" maxlength={6} placeholder="6-digit code"
								autocomplete="one-time-code" disabled={tfaSubmitting} />
							<div class="form-actions">
								<button class="m-btn danger"
									disabled={tfaSubmitting || tfaDisableCode.trim().length !== 6}
									onclick={handleDisableTfa}>
									{tfaSubmitting ? 'Disabling…' : 'Disable'}
								</button>
								<button class="m-btn ghost" disabled={tfaSubmitting}
									onclick={() => { tfaView = 'idle'; tfaDisableCode = ''; }}>
									Cancel
								</button>
							</div>
						</div>
					</div>

				{:else if !tfaLoading && tfaStatus?.enabled && tfaView === 'idle'}
					<div class="row-expanded" transition:slide={{ duration: 200 }}>
						<div class="tfa-actions">
							<button class="m-text-btn" disabled={tfaSubmitting} onclick={handleRegenCodes}>
								{tfaSubmitting ? 'Regenerating…' : 'Regenerate codes'}
							</button>
							<span class="dot-sep" aria-hidden="true">·</span>
							<button class="m-text-btn danger" onclick={() => { tfaView = 'disable-confirm'; tfaError = ''; }}>
								Disable
							</button>
						</div>
					</div>
				{/if}
			</div>
		</div>

		<!-- Integrations -->
		<div class="group-label">Integrations</div>
		<div class="group">
			<div class="row">
				<div class="row-icon strava-icon" aria-hidden="true">
					<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
						<path d="M15.387 17.944l-2.089-4.116h-3.065L15.387 24l5.15-10.172h-3.066m-7.008-5.599l2.836 5.598h4.172L10.463 0l-7 13.828h4.169"/>
					</svg>
				</div>
				<div class="row-body">
					<span class="row-title">Strava</span>
					{#if stravaStore.connection}
						<span class="row-sub">{stravaStore.connection.firstname} {stravaStore.connection.lastname}</span>
					{:else}
						<span class="row-sub dim">Connect your activities</span>
					{/if}
				</div>
				<div class="row-right">
					{#if stravaStore.connection}
						<span class="status-pill green">Connected</span>
						{#if confirmDisconnect}
							<button class="m-text-btn danger sm" disabled={stravaStore.loading}
								onclick={handleStravaDisconnect}>
								{stravaStore.loading ? '…' : 'Disconnect'}
							</button>
						{:else}
							<button class="m-text-btn sm" onclick={() => { confirmDisconnect = true; }}>
								Manage
							</button>
						{/if}
					{:else if stravaComingSoon}
						<span class="status-pill muted">Soon</span>
					{:else}
						<button class="m-text-btn" onclick={handleStravaConnect}>Connect</button>
					{/if}
				</div>
			</div>
		</div>

		<!-- More -->
		<div class="group-label">More</div>
		<div class="group">
			<button class="row row-tappable" onclick={openSupport}>
				<div class="row-icon">
					<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
						stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
						<circle cx="12" cy="12" r="10"/>
						<path d="M9.09 9a3 3 0 015.83 1c0 2-3 3-3 3"/>
						<line x1="12" y1="17" x2="12.01" y2="17"/>
					</svg>
				</div>
				<span class="row-title">Support</span>
				<svg class="row-chevron" width="14" height="14" viewBox="0 0 24 24" fill="none"
					stroke="currentColor" stroke-width="2" aria-hidden="true">
					<polyline points="9 18 15 12 9 6" />
				</svg>
			</button>

			<div class="row-divider"></div>

			<button class="row row-tappable" onclick={openDesktopVersion}>
				<div class="row-icon">
					<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
						stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
						<rect x="2" y="3" width="20" height="14" rx="2"/>
						<path d="M8 21h8M12 17v4"/>
					</svg>
				</div>
				<span class="row-title">Desktop version</span>
				<svg class="row-chevron" width="14" height="14" viewBox="0 0 24 24" fill="none"
					stroke="currentColor" stroke-width="2" aria-hidden="true">
					<polyline points="9 18 15 12 9 6" />
				</svg>
			</button>
		</div>

		<!-- Version -->
		<div class="group">
			<div class="row">
				<span class="row-title dim">Version</span>
				<span class="row-right dim small">Critiqal v0.1</span>
			</div>
		</div>

		<!-- Sign out -->
		<div class="group">
			<button class="row row-tappable" onclick={handleLogout}>
				<div class="row-icon danger-icon">
					<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
						stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
						<path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/>
						<polyline points="16 17 21 12 16 7"/>
						<line x1="21" y1="12" x2="9" y2="12"/>
					</svg>
				</div>
				<span class="row-title danger-text">Sign out</span>
			</button>
		</div>

		<!-- Safe area spacer -->
		<div class="safe-bottom" aria-hidden="true"></div>
	</div>
</div>

<style>
	/* ── Overlay shell ────────────────────────────────────────────────────── */

	.overlay {
		position: fixed;
		inset: 0;
		background: var(--tg-bg, var(--color-bg, #0a0a0a));
		z-index: 200;
		display: flex;
		flex-direction: column;
		will-change: transform;
		overflow: hidden;
	}

	/* ── Header ───────────────────────────────────────────────────────────── */

	.overlay-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0 4px 0 4px;
		padding-top: var(--tg-content-top, var(--tg-content-safe-area-inset-top, env(safe-area-inset-top, 0px)));
		height: calc(52px + var(--tg-content-top, var(--tg-content-safe-area-inset-top, env(safe-area-inset-top, 0px))));
		border-bottom: 1px solid rgba(255, 255, 255, 0.06);
		flex-shrink: 0;
		background: var(--tg-bg, var(--color-bg, #0a0a0a));
	}

	.back-btn {
		background: none;
		border: none;
		cursor: pointer;
		color: var(--tg-text, var(--color-text-primary, #f0f0f0));
		padding: 10px 12px;
		-webkit-tap-highlight-color: transparent;
		display: flex;
		align-items: center;
		justify-content: center;
		opacity: 0.7;
		transition: opacity 0.15s;
	}

	.back-btn:active { opacity: 0.4; }

	.header-title {
		font-size: 1rem;
		font-weight: 600;
		color: var(--tg-text, var(--color-text-primary, #f0f0f0));
		letter-spacing: -0.01em;
	}

	.header-spacer { width: 44px; }

	/* ── Scroll area ──────────────────────────────────────────────────────── */

	.scroll-area {
		flex: 1;
		overflow-y: auto;
		-webkit-overflow-scrolling: touch;
		padding-top: 0 !important;
		padding-bottom: 0 !important;
	}

	/* ── Group labels ─────────────────────────────────────────────────────── */

	.group-label {
		font-size: 0.6875rem;
		font-weight: 500;
		color: rgba(255, 255, 255, 0.35);
		text-transform: uppercase;
		letter-spacing: 0.07em;
		padding: 20px 20px 6px;
	}

	/* ── Groups ───────────────────────────────────────────────────────────── */

	.group {
		margin: 0 12px;
		background: rgba(255, 255, 255, 0.04);
		border-radius: 14px;
		overflow: hidden;
		margin-bottom: 8px;
	}

	.row-divider {
		height: 1px;
		background: rgba(255, 255, 255, 0.06);
		margin: 0 16px;
	}

	/* ── Rows ─────────────────────────────────────────────────────────────── */

	.row {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 14px 16px;
		background: none;
		border: none;
		width: 100%;
		text-align: left;
		font-family: inherit;
		cursor: default;
		-webkit-tap-highlight-color: transparent;
		color: var(--tg-text, var(--color-text-primary, #f0f0f0));
		min-height: 52px;
	}

	.row-tappable {
		cursor: pointer;
		transition: background-color 0.12s ease;
	}

	.row-tappable:active { background: rgba(255, 255, 255, 0.05); }

	.row-link {
		text-decoration: none;
		cursor: pointer;
		transition: background-color 0.12s ease;
	}

	.row-link:active { background: rgba(255, 255, 255, 0.05); }

	.row-section {
		display: flex;
		flex-direction: column;
	}

	/* ── Row parts ────────────────────────────────────────────────────────── */

	.row-icon {
		width: 28px;
		height: 28px;
		border-radius: 7px;
		background: rgba(255, 255, 255, 0.08);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		color: rgba(255, 255, 255, 0.6);
	}

	.strava-icon {
		background: rgba(252, 82, 0, 0.15);
		color: #fc5200;
	}

	.danger-icon {
		background: rgba(224, 82, 82, 0.12);
		color: #e05252;
	}

	.avatar {
		width: 36px;
		height: 36px;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.08);
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
		color: rgba(255, 255, 255, 0.6);
		user-select: none;
	}

	.row-body {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 1px;
	}

	.row-title {
		font-size: 0.9375rem;
		font-weight: 400;
		color: var(--tg-text, #f0f0f0);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.row-sub {
		font-size: 0.75rem;
		color: rgba(255, 255, 255, 0.45);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.row-right {
		display: flex;
		align-items: center;
		gap: 6px;
		flex-shrink: 0;
	}

	.row-chevron {
		color: rgba(255, 255, 255, 0.25);
		flex-shrink: 0;
		transition: transform 0.2s ease;
	}

	.row-chevron.rotated { transform: rotate(180deg); }

	/* ── Status pills ─────────────────────────────────────────────────────── */

	.status-pill {
		font-size: 0.6875rem;
		font-weight: 600;
		padding: 3px 8px;
		border-radius: 999px;
		letter-spacing: 0.02em;
		white-space: nowrap;
	}

	.status-pill.green {
		background: rgba(16, 185, 129, 0.12);
		color: #10b981;
	}

	.status-pill.amber {
		background: rgba(245, 158, 11, 0.12);
		color: #f59e0b;
	}

	.status-pill.muted {
		background: rgba(255, 255, 255, 0.06);
		color: rgba(255, 255, 255, 0.35);
	}

	/* ── Expanded content ─────────────────────────────────────────────────── */

	.row-expanded {
		padding: 0 16px 14px;
		display: flex;
		flex-direction: column;
		gap: 10px;
	}

	.tfa-actions {
		display: flex;
		align-items: center;
		gap: 10px;
	}

	/* ── Inline form ──────────────────────────────────────────────────────── */

	.inline-form {
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.form-actions {
		display: flex;
		gap: 8px;
	}

	/* ── Inputs ───────────────────────────────────────────────────────────── */

	.m-input {
		width: 100%;
		background: rgba(255, 255, 255, 0.06);
		border: 1px solid rgba(255, 255, 255, 0.1);
		border-radius: 10px;
		padding: 11px 14px;
		font-size: 0.9375rem;
		color: var(--tg-text, #f0f0f0);
		font-family: inherit;
		outline: none;
		box-sizing: border-box;
		-webkit-tap-highlight-color: transparent;
	}

	.m-input::placeholder { color: rgba(255, 255, 255, 0.25); }
	.m-input:focus { border-color: rgba(255, 255, 255, 0.2); }
	.m-input:disabled { opacity: 0.4; }

	/* ── Buttons ──────────────────────────────────────────────────────────── */

	.m-btn {
		padding: 10px 18px;
		border-radius: 10px;
		border: none;
		font-size: 0.875rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		white-space: nowrap;
		-webkit-tap-highlight-color: transparent;
		transition: opacity 0.15s;
	}

	.m-btn:disabled { opacity: 0.35; cursor: not-allowed; }
	.m-btn:active:not(:disabled) { opacity: 0.7; }

	.m-btn.primary {
		background: var(--tg-text, #f0f0f0);
		color: var(--tg-bg, #0a0a0a);
	}

	.m-btn.ghost {
		background: rgba(255, 255, 255, 0.06);
		color: var(--tg-text, #f0f0f0);
		border: 1px solid rgba(255, 255, 255, 0.1);
	}

	.m-btn.danger {
		background: rgba(224, 82, 82, 0.15);
		color: #e05252;
	}

	.m-text-btn {
		background: none;
		border: none;
		padding: 0;
		font-size: 0.875rem;
		font-weight: 500;
		font-family: inherit;
		color: rgba(255, 255, 255, 0.6);
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
		transition: opacity 0.15s;
	}

	.m-text-btn:disabled { opacity: 0.35; cursor: not-allowed; }
	.m-text-btn:active:not(:disabled) { opacity: 0.5; }
	.m-text-btn.danger { color: #e05252; }
	.m-text-btn.sm { font-size: 0.8125rem; }

	/* ── Feedback ─────────────────────────────────────────────────────────── */

	.feedback {
		font-size: 0.8125rem;
		margin: 0;
		line-height: 1.4;
	}

	.feedback.ok { color: #10b981; }
	.feedback.err { color: #e05252; }
	.feedback.amber { color: #f59e0b; }
	.feedback.muted { color: rgba(255, 255, 255, 0.45); }

	/* ── 2FA ──────────────────────────────────────────────────────────────── */

	.qr {
		width: 9rem;
		height: 9rem;
		border-radius: 10px;
		align-self: center;
		background: #fff;
		padding: 6px;
	}

	.manual-entry {
		font-size: 0.8125rem;
		color: rgba(255, 255, 255, 0.4);
	}

	.manual-entry summary { cursor: pointer; }

	.tfa-secret {
		display: block;
		margin-top: 6px;
		font-family: 'Courier New', monospace;
		font-size: 0.75rem;
		color: var(--tg-text, #f0f0f0);
		background: rgba(255, 255, 255, 0.05);
		border-radius: 8px;
		padding: 8px 10px;
		word-break: break-all;
		letter-spacing: 0.04em;
	}

	.codes-grid {
		display: grid;
		grid-template-columns: 1fr 1fr;
		gap: 6px;
		list-style: none;
		padding: 0;
		margin: 0;
	}

	.code-chip {
		font-family: 'Courier New', monospace;
		font-size: 0.75rem;
		color: var(--tg-text, #f0f0f0);
		background: rgba(255, 255, 255, 0.05);
		border-radius: 8px;
		padding: 6px 10px;
		letter-spacing: 0.04em;
	}

	.dot-sep {
		color: rgba(255, 255, 255, 0.25);
	}

	/* ── Misc ─────────────────────────────────────────────────────────────── */

	.dim { color: rgba(255, 255, 255, 0.35) !important; }
	.danger-text { color: #e05252; }
	.small { font-size: 0.8125rem; }

	.spinner-xs {
		display: inline-block;
		width: 12px;
		height: 12px;
		border: 2px solid rgba(255, 255, 255, 0.1);
		border-top-color: rgba(255, 255, 255, 0.4);
		border-radius: 50%;
		animation: spin 0.7s linear infinite;
		flex-shrink: 0;
	}

	@keyframes spin { to { transform: rotate(360deg); } }

	.safe-bottom {
		height: calc(16px + env(safe-area-inset-bottom, 0px));
	}
</style>
