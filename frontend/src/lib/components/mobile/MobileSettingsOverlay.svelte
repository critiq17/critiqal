<script lang="ts">
	import { onMount } from 'svelte';
	import { slide, fade } from 'svelte/transition';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { stravaStore } from '$lib/stores/strava.store.svelte';
	import { navStack } from '$lib/stores/nav-stack.store.svelte';
	import { twoFactorService } from '$lib/services/two-factor.service';
	import { emailVerificationService } from '$lib/services/email-verification.service';
	import { recoveryService } from '$lib/services/recovery.service';
	import { sessionService } from '$lib/services/session.service';
	import { getTelegramWebApp } from '$lib/telegram';
	import { goto } from '$app/navigation';
	import { apiClient } from '$lib/api/client';
	import { ApiError } from '$lib/types';
	import type { User, TotpSetupResponse, TwoFactorStatusResponse, AuthSession } from '$lib/types';
	import { t } from '$lib/i18n';
	import LanguageSwitcher from '$lib/i18n/LanguageSwitcher.svelte';
	import DeviceIcon from '$lib/components/DeviceIcon.svelte';
	import { formatRelativeTime } from '$lib/utils/formatRelativeTime';

	// Navigation (swipe + Telegram BackButton + slide-in) is owned by
	// OverlayHost — this view is purely presentational content.
	interface Props {
		onBack: () => void;
	}

	// Back is handled by the native Telegram BackButton (wired in OverlayHost);
	// onBack is kept for the OverlayHost contract.
	let { onBack }: Props = $props();
	void onBack;

	let scrollEl = $state<HTMLDivElement | undefined>(undefined);
	// "Settings" header is invisible at rest and frosts in on scroll.
	let scrolled = $state(false);
	function onScroll(): void {
		scrolled = (scrollEl?.scrollTop ?? 0) > 8;
	}

	function openOwnProfile(): void {
		if (authStore.user) navStack.pushProfile(authStore.user.username);
	}

	onMount(() => {
		loadTfaStatus();
		stravaStore.load();
		loadSessions();
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

	type TwoFaView =
		| 'idle'
		| 'setup-qr'
		| 'show-codes'
		| 'disable-confirm'
		| 'regen-confirm'
		| 'regen-codes';

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

	// ── Sessions ─────────────────────────────────────────────────────────────

	let sessions = $state<AuthSession[]>([]);
	let sessionsLoading = $state(true);
	let sessionsError = $state('');
	let revokingId = $state<string | null>(null);
	let confirmRevokeId = $state<string | null>(null);

	async function loadSessions(): Promise<void> {
		sessionsLoading = true;
		sessionsError = '';
		try {
			sessions = await sessionService.list();
		} catch (err) {
			sessionsError = mapError(err);
		} finally {
			sessionsLoading = false;
		}
	}

	async function handleRevokeSession(id: string): Promise<void> {
		revokingId = id;
		try {
			await sessionService.revoke(id);
			sessions = sessions.filter((s) => s.id !== id);
			confirmRevokeId = null;
		} catch (err) {
			sessionsError = mapError(err);
		} finally {
			revokingId = null;
		}
	}

	function sessionLocation(s: AuthSession): string {
		const parts: string[] = [];
		if (s.city) parts.push(s.city);
		if (s.countryName) parts.push(s.countryName);
		else if (s.countryCode) parts.push(s.countryCode);
		return parts.length === 0 ? t('settings.sessions.unknownLocation') : parts.join(', ');
	}

	function sessionDevice(s: AuthSession): string {
		const parts: string[] = [];
		if (s.browser) parts.push(s.browser);
		const p = prettyPlatform(s.platform);
		if (p) parts.push(p);
		return parts.length === 0 ? t('settings.sessions.unknownDevice') : parts.join(' · ');
	}

	function prettyPlatform(p: string | null): string | null {
		if (!p) return null;
		switch (p.toLowerCase()) {
			case 'ios': return 'iOS';
			case 'macos': return 'macOS';
			case 'android': return 'Android';
			case 'windows': return 'Windows';
			case 'linux': return 'Linux';
			case 'telegram': return 'Telegram';
			case 'unknown':
			case 'other': return null;
			default: return p;
		}
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
		navStack.reset();
		goto('/');
	}

	function mapError(err: unknown): string {
		if (err instanceof ApiError) return err.message || t('common.somethingWentWrong');
		if (err instanceof Error) return err.message;
		return t('common.somethingWentWrong');
	}
</script>

<header class="settings-header" class:scrolled>
	<h1 class="settings-header__title">{t('settings.title')}</h1>
</header>

<div
	class="scroll-area mobile-scroll-container"
	bind:this={scrollEl}
	onscroll={onScroll}
>

		<!-- Profile -->
		{#if authStore.user}
			<div class="group-label">{t('settings.sections.profile')}</div>
			<div class="group">
				<button type="button" class="row row-link" onclick={openOwnProfile}>
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
				</button>
			</div>
		{/if}

		<!-- Language -->
		<div class="group-label">{t('settings.sections.language')}</div>
		<div class="group">
			<div class="row row-block">
				<LanguageSwitcher />
			</div>
		</div>

		<!-- Account Security -->
		<div class="group-label">{t('settings.sections.account')}</div>
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
						<span class="row-title">{t('settings.sections.email')}</span>
						{#if authStore.user?.email && authStore.user.emailVerified}
							<span class="row-sub">{authStore.user.email}</span>
						{:else if authStore.user?.pendingEmail}
							<span class="row-sub">{authStore.user.pendingEmail}</span>
						{:else}
							<span class="row-sub dim">{t('settings.email.notSet')}</span>
						{/if}
					</div>
					<div class="row-right">
						{#if authStore.user?.emailVerified}
							<span class="status-pill green">{t('settings.email.verified')}</span>
						{:else if authStore.user?.pendingEmail}
							<span class="status-pill amber">{t('settings.email.pending')}</span>
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
							<p class="feedback ok">{t('settings.email.sent')}</p>
						{/if}
						{#if authStore.user?.pendingEmail && !emailSuccess}
							<p class="feedback muted">{t('settings.email.pendingHint')} — <strong>{authStore.user.pendingEmail}</strong></p>
							<div class="form-actions">
								<button class="m-btn ghost" disabled={emailSubmitting} onclick={handleResendVerification}>
									{emailSubmitting ? '…' : t('settings.email.resend')}
								</button>
								<button class="m-btn ghost" disabled={emailSubmitting}
									onclick={() => { emailEditing = false; emailError = ''; }}>
									{t('common.cancel')}
								</button>
							</div>
						{:else if !emailSuccess}
							<div class="inline-form">
								<input
									type="email"
									class="m-input"
									bind:value={emailInput}
									placeholder={t('settings.email.newPlaceholder')}
									autocomplete="email"
									disabled={emailSubmitting}
								/>
								<div class="form-actions">
									<button class="m-btn primary" disabled={emailSubmitting || !emailInput.trim()}
										onclick={handleSetEmail}>
										{emailSubmitting ? t('common.saving') : t('common.save')}
									</button>
									<button class="m-btn ghost" disabled={emailSubmitting}
										onclick={() => { emailEditing = false; emailInput = ''; emailError = ''; }}>
										{t('common.cancel')}
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
						<span class="row-title">{t('settings.sections.twoFactor')}</span>
						{#if tfaLoading}
							<span class="row-sub dim">{t('common.loading')}</span>
						{:else if tfaStatus?.enabled && tfaCodesCount !== null}
							<span class="row-sub">{tfaCodesCount} {t('settings.twoFactor.recoveryCodes')}</span>
						{:else if !tfaStatus?.enabled}
							<span class="row-sub dim">{t('settings.twoFactor.notEnabled')}</span>
						{/if}
					</div>
					<div class="row-right">
						{#if tfaLoading}
							<span class="spinner-xs" aria-label={t('common.loading')}></span>
						{:else if tfaStatus?.enabled}
							<span class="status-pill green">{t('common.on')}</span>
						{:else}
							<span class="status-pill muted">{t('common.off')}</span>
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
						<p class="feedback muted">{t('settings.twoFactor.setupHint')}</p>
						<img class="qr" src={tfaSetup.qrCodeUri} alt="QR" />
						<details class="manual-entry">
							<summary>{t('settings.twoFactor.cantScan')}</summary>
							<code class="tfa-secret">{tfaSetup.secret}</code>
						</details>
						<div class="inline-form">
							<input type="text" class="m-input" bind:value={tfaConfirmCode}
								inputmode="numeric" maxlength={6} placeholder={t('settings.twoFactor.codePlaceholder')}
								autocomplete="one-time-code" disabled={tfaSubmitting} />
							<div class="form-actions">
								<button class="m-btn primary"
									disabled={tfaSubmitting || tfaConfirmCode.trim().length !== 6}
									onclick={handleConfirmTfa}>
									{tfaSubmitting ? t('settings.twoFactor.confirming') : t('settings.twoFactor.confirm')}
								</button>
								<button class="m-btn ghost" disabled={tfaSubmitting}
									onclick={() => { tfaView = 'idle'; tfaSetup = null; }}>
									{t('common.cancel')}
								</button>
							</div>
						</div>
					</div>

				{:else if !tfaLoading && (tfaView === 'show-codes' || tfaView === 'regen-codes')}
					<div class="row-expanded" transition:slide={{ duration: 200 }}>
						<p class="feedback amber">{t('settings.twoFactor.saveCodes')}</p>
						<ul class="codes-grid">
							{#each tfaRecoveryCodes as code}
								<li class="code-chip">{code}</li>
							{/each}
						</ul>
						<button class="m-btn primary" style="align-self:flex-start"
							onclick={tfaView === 'regen-codes'
								? () => { tfaRecoveryCodes = []; tfaView = 'idle'; loadTfaStatus(); }
								: handleDoneWithCodes}>
							{t('settings.twoFactor.savedDone')}
						</button>
					</div>

				{:else if !tfaLoading && tfaView === 'disable-confirm'}
					<div class="row-expanded" transition:slide={{ duration: 200 }}>
						<p class="feedback muted">{t('settings.twoFactor.disableHint')}</p>
						<div class="inline-form">
							<input type="text" class="m-input" bind:value={tfaDisableCode}
								inputmode="numeric" maxlength={6} placeholder={t('settings.twoFactor.codePlaceholder')}
								autocomplete="one-time-code" disabled={tfaSubmitting} />
							<div class="form-actions">
								<button class="m-btn danger"
									disabled={tfaSubmitting || tfaDisableCode.trim().length !== 6}
									onclick={handleDisableTfa}>
									{tfaSubmitting ? t('settings.twoFactor.disabling') : t('common.disable')}
								</button>
								<button class="m-btn ghost" disabled={tfaSubmitting}
									onclick={() => { tfaView = 'idle'; tfaDisableCode = ''; }}>
									{t('common.cancel')}
								</button>
							</div>
						</div>
					</div>

				{:else if !tfaLoading && tfaView === 'regen-confirm'}
					<div class="row-expanded" transition:slide={{ duration: 200 }}>
						<p class="feedback amber">{t('settings.twoFactor.saveCodes')}</p>
						<div class="form-actions">
							<button class="m-btn primary" disabled={tfaSubmitting} onclick={handleRegenCodes}>
								{tfaSubmitting ? '…' : t('common.confirm')}
							</button>
							<button class="m-btn ghost" disabled={tfaSubmitting}
								onclick={() => { tfaView = 'idle'; tfaError = ''; }}>
								{t('common.cancel')}
							</button>
						</div>
					</div>

				{:else if !tfaLoading && tfaStatus?.enabled && tfaView === 'idle'}
					<div class="row-expanded" transition:slide={{ duration: 200 }}>
						<div class="tfa-actions">
							<button class="m-text-btn"
								onclick={() => { tfaView = 'regen-confirm'; tfaError = ''; }}>
								{t('settings.twoFactor.regenerate')}
							</button>
							<span class="dot-sep" aria-hidden="true">·</span>
							<button class="m-text-btn danger" onclick={() => { tfaView = 'disable-confirm'; tfaError = ''; }}>
								{t('common.disable')}
							</button>
						</div>
					</div>
				{/if}
			</div>
		</div>

		<!-- Sessions -->
		<div class="group-label sessions-label">
			<span>{t('settings.sections.sessions')}</span>
			<button
				type="button"
				class="sessions-refresh"
				disabled={sessionsLoading}
				onclick={loadSessions}
				aria-label={t('settings.sessions.refresh')}
			>
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor"
					stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
					class:spinning={sessionsLoading} aria-hidden="true">
					<path d="M3 12a9 9 0 0 1 15.5-6.2L21 8" />
					<path d="M21 3v5h-5" />
					<path d="M21 12a9 9 0 0 1-15.5 6.2L3 16" />
					<path d="M3 21v-5h5" />
				</svg>
			</button>
		</div>
		<div class="group sessions-group">
			{#if sessionsError}
				<div class="sessions-feedback"><p class="feedback err">{sessionsError}</p></div>
			{/if}
			{#if sessionsLoading && sessions.length === 0}
				<div class="sessions-skel">
					<div class="session-skel"></div>
					<div class="session-skel"></div>
				</div>
			{:else if sessions.length === 0}
				<div class="sessions-feedback"><p class="feedback muted">{t('settings.sessions.empty')}</p></div>
			{:else}
				{#each sessions as session, idx (session.id)}
					{#if idx > 0}
						<div class="row-divider"></div>
					{/if}
					<div class="session-row" class:current={session.current}>
						<div class="row">
							<div class="row-icon session-icon" class:current-ic={session.current}>
								<DeviceIcon platform={session.platform} size={16} stroke={2} />
							</div>
							<div class="row-body session-body">
								<div class="session-head">
									<span class="row-title">{sessionDevice(session)}</span>
									{#if session.current}
										<span class="status-pill green">{t('settings.sessions.current')}</span>
									{:else if session.countryCode}
										<span class="status-pill muted">{session.countryCode}</span>
									{/if}
								</div>
								<span class="row-sub">{sessionLocation(session)}</span>
								<span class="row-sub small dim">
									{t('settings.sessions.signedIn')} · {formatRelativeTime(session.createdAt)} ·
									{t('settings.sessions.lastSeen')} · {formatRelativeTime(session.lastSeenAt)}
								</span>
							</div>
						</div>
						{#if !session.current}
							{#if confirmRevokeId === session.id}
								<div class="row-expanded" transition:slide={{ duration: 180 }}>
									<p class="feedback muted">{t('settings.sessions.revokeConfirm')}</p>
									<div class="form-actions">
										<button
											class="m-btn danger"
											disabled={revokingId === session.id}
											onclick={() => handleRevokeSession(session.id)}
										>
											{revokingId === session.id ? t('settings.sessions.revoking') : t('settings.sessions.revoke')}
										</button>
										<button
											class="m-btn ghost"
											disabled={revokingId === session.id}
											onclick={() => (confirmRevokeId = null)}
										>
											{t('common.cancel')}
										</button>
									</div>
								</div>
							{:else}
								<div class="session-actions">
									<button class="m-text-btn danger sm" onclick={() => (confirmRevokeId = session.id)}>
										{t('settings.sessions.revoke')}
									</button>
								</div>
							{/if}
						{/if}
					</div>
				{/each}
			{/if}
		</div>

		<!-- Integrations -->
		<div class="group-label">{t('settings.sections.integrations')}</div>
		<div class="group">
			<div class="row">
				<div class="row-icon strava-icon" aria-hidden="true">
					<svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
						<path d="M15.387 17.944l-2.089-4.116h-3.065L15.387 24l5.15-10.172h-3.066m-7.008-5.599l2.836 5.598h4.172L10.463 0l-7 13.828h4.169"/>
					</svg>
				</div>
				<div class="row-body">
					<span class="row-title">{t('settings.integrations.strava')}</span>
					{#if stravaStore.connection}
						<span class="row-sub">{stravaStore.connection.firstname} {stravaStore.connection.lastname}</span>
					{:else}
						<span class="row-sub dim">{t('settings.integrations.stravaConnect')}</span>
					{/if}
				</div>
				<div class="row-right">
					{#if stravaStore.connection}
						<span class="status-pill green">{t('profile.strava.connected')}</span>
						{#if confirmDisconnect}
							<button class="m-text-btn danger sm" disabled={stravaStore.loading}
								onclick={handleStravaDisconnect}>
								{stravaStore.loading ? '…' : t('settings.integrations.disconnect')}
							</button>
						{:else}
							<button class="m-text-btn sm" onclick={() => { confirmDisconnect = true; }}>
								{t('common.edit')}
							</button>
						{/if}
					{:else if stravaComingSoon}
						<span class="status-pill muted">{t('common.comingSoon')}</span>
					{:else}
						<button class="m-text-btn" onclick={handleStravaConnect}>{t('settings.integrations.connect')}</button>
					{/if}
				</div>
			</div>
		</div>

		<!-- More -->
		<div class="group-label">{t('common.more')}</div>
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
				<span class="row-title">{t('settings.support')}</span>
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
				<span class="row-title">{t('settings.desktopVersion')}</span>
				<svg class="row-chevron" width="14" height="14" viewBox="0 0 24 24" fill="none"
					stroke="currentColor" stroke-width="2" aria-hidden="true">
					<polyline points="9 18 15 12 9 6" />
				</svg>
			</button>
		</div>

		<!-- Version -->
		<div class="group">
			<div class="row">
				<span class="row-title dim">{t('settings.version')}</span>
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
				<span class="row-title danger-text">{t('settings.signOut')}</span>
			</button>
		</div>

		<!-- Safe area spacer -->
		<div class="safe-bottom" aria-hidden="true"></div>
	</div>

<style>
	/* ── Scroll area ──────────────────────────────────────────────────────── */

	.scroll-area {
		flex: 1;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior: contain;
		/* clear the fixed header + Telegram chrome */
		padding-top: var(--tg-top-clearance) !important;
		padding-bottom: 0 !important;
	}

	/* "Settings" rides high between the native Telegram buttons; invisible at
	   rest, frosts in on scroll — same language as the profile header. */
	.settings-header {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		z-index: 10;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: calc(
				0.4rem +
					var(--tg-content-top, var(--tg-content-safe-area-inset-top, env(safe-area-inset-top, 6px)))
			)
			3rem 0.55rem;
		background: transparent;
		border-bottom: 1px solid transparent;
		transition: background 0.25s ease, border-color 0.25s ease;
	}

	.settings-header.scrolled {
		background: var(--glass-bg);
		backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
		border-bottom-color: var(--glass-border);
	}

	.settings-header__title {
		margin: 0;
		font-size: 1.05rem;
		font-weight: 700;
		color: var(--tg-text, #f0f0f0);
		letter-spacing: -0.015em;
		max-width: 70vw;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		text-align: center;
	}

	/* ── Group labels ─────────────────────────────────────────────────────── */

	.group-label {
		font-size: 0.6875rem;
		font-weight: 500;
		color: var(--text-faint);
		text-transform: uppercase;
		letter-spacing: 0.07em;
		padding: 20px 20px 6px;
	}

	/* ── Groups ───────────────────────────────────────────────────────────── */

	.group {
		margin: 0 12px;
		background: var(--surface-tint-subtle);
		border-radius: 14px;
		overflow: hidden;
		margin-bottom: 8px;
	}

	.row-divider {
		height: 1px;
		background: var(--surface-tint-soft);
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

	.row-tappable,
	.row-link {
		cursor: pointer;
		transition:
			background-color 0.14s ease,
			transform 0.34s cubic-bezier(0.34, 1.56, 0.64, 1);
	}

	.row-link { text-decoration: none; }

	.row-tappable:active,
	.row-link:active {
		background: var(--surface-tint-subtle);
		transform: scale(0.987);
		transition-duration: 0.14s, 0.08s;
	}

	/* Groups gently settle in when Settings opens (staggered). */
	.group {
		animation: group-in 0.42s cubic-bezier(0.22, 1, 0.36, 1) both;
	}
	.group:nth-of-type(2) { animation-delay: 0.04s; }
	.group:nth-of-type(3) { animation-delay: 0.08s; }
	.group:nth-of-type(4) { animation-delay: 0.12s; }

	@keyframes group-in {
		from { opacity: 0; transform: translateY(8px); }
		to { opacity: 1; transform: translateY(0); }
	}

	@media (prefers-reduced-motion: reduce) {
		.row-tappable,
		.row-link { transition: background-color 0.14s ease; }
		.row-tappable:active,
		.row-link:active { transform: none; }
		.group { animation: none; }
	}

	.row-section {
		display: flex;
		flex-direction: column;
	}

	/* ── Row parts ────────────────────────────────────────────────────────── */

	.row-icon {
		width: 28px;
		height: 28px;
		border-radius: 7px;
		background: var(--surface-tint-medium);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		color: var(--text-tertiary);
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
		background: var(--surface-tint-medium);
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
		color: var(--text-tertiary);
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
		color: var(--text-quaternary);
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
		color: var(--text-faint);
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
		background: var(--surface-tint-soft);
		color: var(--text-faint);
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
		background: var(--surface-tint-soft);
		border: 1px solid var(--surface-tint-medium);
		border-radius: 10px;
		padding: 11px 14px;
		font-size: 0.9375rem;
		color: var(--tg-text, #f0f0f0);
		font-family: inherit;
		outline: none;
		box-sizing: border-box;
		-webkit-tap-highlight-color: transparent;
	}

	.m-input::placeholder { color: var(--text-faint); }
	.m-input:focus { border-color: var(--text-ghost); }
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
		background: var(--surface-tint-soft);
		color: var(--tg-text, #f0f0f0);
		border: 1px solid var(--surface-tint-medium);
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
		color: var(--text-tertiary);
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
	.feedback.muted { color: var(--text-quaternary); }

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
		color: var(--text-quaternary);
	}

	.manual-entry summary { cursor: pointer; }

	.tfa-secret {
		display: block;
		margin-top: 6px;
		font-family: 'Courier New', monospace;
		font-size: 0.75rem;
		color: var(--tg-text, #f0f0f0);
		background: var(--surface-tint-subtle);
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
		background: var(--surface-tint-subtle);
		border-radius: 8px;
		padding: 6px 10px;
		letter-spacing: 0.04em;
	}

	.dot-sep {
		color: var(--text-faint);
	}

	/* ── Sessions ─────────────────────────────────────────────────────────── */

	.sessions-label {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 8px;
		padding-right: 20px;
	}

	.sessions-refresh {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 22px;
		height: 22px;
		border-radius: 50%;
		border: none;
		background: transparent;
		color: var(--text-tertiary);
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
		transition: color 0.18s ease, background-color 0.18s ease;
	}

	.sessions-refresh:hover:not(:disabled) {
		color: var(--text-strong);
		background: var(--surface-tint-subtle);
	}

	.sessions-refresh svg {
		width: 13px;
		height: 13px;
	}

	.sessions-refresh svg.spinning {
		animation: spin 0.9s linear infinite;
	}

	.sessions-group {
		overflow: hidden;
	}

	.sessions-group .row-divider {
		background: var(--divider-faint);
		margin: 0 20px;
	}

	.session-row {
		display: flex;
		flex-direction: column;
		transition: background-color 0.18s ease;
	}

	.session-row.current {
		background: transparent;
	}

	.session-icon {
		color: var(--text-strong);
	}

	.session-icon.current-ic {
		background: rgba(16, 185, 129, 0.14);
		color: #10b981;
	}

	.session-body {
		gap: 3px;
	}

	.session-head {
		display: flex;
		align-items: center;
		gap: 6px;
		min-width: 0;
	}

	.session-head .row-title {
		font-weight: 500;
	}

	.row-sub.small {
		font-size: 0.6875rem;
		line-height: 1.4;
		white-space: normal;
	}

	.session-actions {
		display: flex;
		justify-content: flex-end;
		padding: 0 16px 12px;
	}

	.sessions-feedback {
		padding: 14px 16px;
	}

	.sessions-skel {
		display: flex;
		flex-direction: column;
	}

	.session-skel {
		height: 64px;
		background: var(--surface-tint-soft);
		border-bottom: 1px solid var(--surface-tint-subtle);
		position: relative;
		overflow: hidden;
		animation: pulse-bg 1.4s ease-in-out infinite;
	}

	.session-skel:last-child {
		border-bottom: none;
	}

	@keyframes pulse-bg {
		0%, 100% { opacity: 0.5; }
		50% { opacity: 0.9; }
	}

	/* ── Misc ─────────────────────────────────────────────────────────────── */

	.dim { color: var(--text-faint) !important; }
	.danger-text { color: #e05252; }
	.small { font-size: 0.8125rem; }

	.spinner-xs {
		display: inline-block;
		width: 12px;
		height: 12px;
		border: 2px solid var(--surface-tint-medium);
		border-top-color: var(--text-quaternary);
		border-radius: 50%;
		animation: spin 0.7s linear infinite;
		flex-shrink: 0;
	}

	@keyframes spin { to { transform: rotate(360deg); } }

	.safe-bottom {
		height: calc(16px + env(safe-area-inset-bottom, 0px));
	}
</style>
