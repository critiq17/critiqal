<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import { fly, slide } from 'svelte/transition';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { stravaStore } from '$lib/stores/strava.store.svelte';
	import type { AuthSession } from '$lib/types';
	import { UseSettings } from '$lib/features/settings/useSettings.svelte';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';
	import DeviceIcon from '$lib/components/DeviceIcon.svelte';
	import LanguageSwitcher from '$lib/i18n/LanguageSwitcher.svelte';
	import { t } from '$lib/i18n';
	import { formatRelativeTime } from '$lib/utils/formatRelativeTime';

	const settings = new UseSettings();

	// ── Email (UI-only state) ─────────────────────────────────────────────────

	let emailEditing = $state(false);

	function startEmailEdit(): void {
		settings.emailInput = '';
		emailEditing = true;
		settings.emailError = '';
		settings.emailSuccess = false;
	}

	function cancelEmailEdit(): void {
		emailEditing = false;
		settings.emailInput = '';
		settings.emailError = '';
	}

	async function handleSetEmail(): Promise<void> {
		await settings.handleSetEmail(settings.emailInput);
		if (settings.emailSuccess) emailEditing = false;
	}

	// ── Strava (UI-only state) ────────────────────────────────────────────────

	let stravaComingSoon = $state(false);
	let confirmDisconnect = $state(false);

	function handleStravaConnect(): void {
		stravaComingSoon = true;
		setTimeout(() => { stravaComingSoon = false; }, 4000);
	}

	async function handleStravaDisconnect(): Promise<void> {
		await settings.handleStravaDisconnect();
		confirmDisconnect = false;
	}

	// ── Sessions (UI-only state) ──────────────────────────────────────────────

	let confirmRevokeId = $state<string | null>(null);

	async function handleRevokeSession(id: string): Promise<void> {
		await settings.handleRevokeSession(id);
		if (!settings.sessionsError) confirmRevokeId = null;
	}

	// ── Session display helpers ───────────────────────────────────────────────

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

	onMount(() => {
		settings.loadTfaStatus();
		stravaStore.load();
		settings.loadSessions();
	});
</script>

<svelte:head>
	<title>{t('settings.title')} — Critiqal</title>
	<meta name="description" content="Manage your account settings" />
</svelte:head>

<div class="page-layout">
	<div class="col-left">
		<LeftSidebar />
	</div>

	<main class="col-center">
		<header class="page-header">
			<h1 class="page-title">{t('settings.title')}</h1>
		</header>

		<!-- Language -->
		<section class="section">
			<div class="setting-info col">
				<p class="section-label">{t('settings.sections.language')}</p>
				<span class="setting-sub">{t('settings.language.subtitle')}</span>
			</div>
			<LanguageSwitcher />
		</section>

		<!-- Profile -->
		{#if authStore.isAuthenticated && authStore.user}
			<section class="section">
				<p class="section-label">{t('settings.sections.profile')}</p>
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
			<p class="section-label">{t('settings.sections.email')}</p>

			<!-- Verified email row -->
			{#if authStore.user?.email && authStore.user.emailVerified}
				<div class="setting-row">
					<div class="setting-info">
						<span class="setting-value">{authStore.user.email}</span>
						<span class="status-dot verified" title={t('settings.email.verified')}></span>
					</div>
					{#if !emailEditing}
						<button class="link-btn" onclick={startEmailEdit}>{t('common.change')}</button>
					{/if}
				</div>
			{/if}

			<!-- Pending email row -->
			{#if authStore.user?.pendingEmail}
				<div class="setting-row" in:fly={{ y: -4, duration: 180 }}>
					<div class="setting-info col">
						<div class="setting-info">
							<span class="setting-value">{authStore.user.pendingEmail}</span>
							<span class="badge-pending">{t('settings.email.pending')}</span>
						</div>
						<span class="setting-sub">{t('settings.email.pendingHint')}</span>
					</div>
					{#if !emailEditing}
						<button class="link-btn" disabled={settings.emailLoading} onclick={() => settings.handleResendVerification()}>
							{settings.emailLoading ? '…' : t('settings.email.resend')}
						</button>
					{/if}
				</div>
			{/if}

			<!-- No email at all -->
			{#if !authStore.user?.email && !authStore.user?.pendingEmail && !emailEditing}
				<div class="setting-row">
					<span class="setting-placeholder">{t('settings.email.notSet')}</span>
					<button class="link-btn" onclick={startEmailEdit}>{t('common.add')}</button>
				</div>
			{/if}

			{#if settings.emailSuccess}
				<p class="hint hint-ok" in:fly={{ y: -4, duration: 150 }}>{t('settings.email.sent')}</p>
			{/if}

			{#if settings.emailError && !emailEditing}
				<p class="hint hint-err" in:fly={{ y: -4, duration: 150 }}>{settings.emailError}</p>
			{/if}

			{#if emailEditing}
				<div class="inline-form" transition:slide={{ duration: 180 }}>
					{#if settings.emailError}
						<p class="hint hint-err">{settings.emailError}</p>
					{/if}
					<div class="input-row">
						<input
							type="email"
							class="input"
							bind:value={settings.emailInput}
							autocomplete="email"
							required
							disabled={settings.emailLoading}
							placeholder={t('settings.email.newPlaceholder')}
						/>
						<button
							class="btn-primary"
							disabled={settings.emailLoading || settings.emailInput.trim().length === 0}
							onclick={handleSetEmail}
						>
							{settings.emailLoading ? t('common.saving') : t('common.save')}
						</button>
						<button class="btn-ghost" disabled={settings.emailLoading} onclick={cancelEmailEdit}>
							{t('common.cancel')}
						</button>
					</div>
				</div>
			{/if}
		</section>

		<!-- 2FA -->
		<section class="section">
			<div class="setting-row">
				<div class="setting-info col">
					<p class="section-label">{t('settings.sections.twoFactor')}</p>
					{#if !settings.tfaLoading}
						<span class="setting-sub">
							{#if settings.tfaStatus?.enabled}
								{t('settings.twoFactor.enabled')} · {settings.backupCodesCount ?? '…'} {t('settings.twoFactor.recoveryCodes')}
							{:else}
								{t('settings.twoFactor.notEnabled')}
							{/if}
						</span>
					{/if}
				</div>
				{#if !settings.tfaLoading && settings.tfaSetupStep === 'idle'}
					{#if settings.tfaStatus?.enabled}
						<span class="status-badge enabled">{t('common.on')}</span>
					{:else}
						<button class="link-btn" disabled={settings.tfaSubmitting} onclick={() => settings.handleSetupTfa()}>
							{settings.tfaSubmitting ? t('settings.twoFactor.settingUp') : t('common.enable')}
						</button>
					{/if}
				{/if}
			</div>

			{#if settings.tfaError}
				<p class="hint hint-err" in:fly={{ y: -4, duration: 150 }}>{settings.tfaError}</p>
			{/if}

			{#if settings.tfaLoading}
				<div class="skeleton"></div>

			{:else if settings.tfaSetupStep === 'setup' && settings.tfaSetupData}
				<div class="tfa-panel" in:fly={{ y: 6, duration: 200 }}>
					<p class="hint">{t('settings.twoFactor.setupHint')}</p>
					<img class="qr" src={settings.tfaSetupData.qrCodeUri} alt="QR" />
					<details class="manual-entry">
						<summary>{t('settings.twoFactor.cantScan')}</summary>
						<code class="secret">{settings.tfaSetupData.secret}</code>
					</details>
					<div class="input-row">
						<input
							type="text"
							class="input"
							bind:value={settings.tfaCode}
							inputmode="numeric"
							maxlength={6}
							placeholder={t('settings.twoFactor.codePlaceholder')}
							autocomplete="one-time-code"
							disabled={settings.tfaSubmitting}
						/>
						<button class="btn-primary" disabled={settings.tfaSubmitting || settings.tfaCode.trim().length !== 6} onclick={() => settings.handleConfirmTfa(settings.tfaCode)}>
							{settings.tfaSubmitting ? t('settings.twoFactor.confirming') : t('settings.twoFactor.confirm')}
						</button>
						<button class="btn-ghost" disabled={settings.tfaSubmitting} onclick={() => { settings.tfaSetupStep = 'idle'; settings.tfaSetupData = null; }}>
							{t('common.cancel')}
						</button>
					</div>
				</div>

			{:else if settings.tfaSetupStep === 'confirm' || settings.tfaSetupStep === 'regen-confirm'}
				<div class="tfa-panel" in:fly={{ y: 6, duration: 200 }}>
					<p class="hint hint-warn">{t('settings.twoFactor.saveCodes')}</p>
					<ul class="codes-grid">
						{#each settings.tfaRecoveryCodes as code}
							<li class="code">{code}</li>
						{/each}
					</ul>
					<button class="btn-primary" style="align-self: flex-start" onclick={settings.tfaSetupStep === 'regen-confirm' ? () => { settings.tfaRecoveryCodes = []; settings.tfaSetupStep = 'idle'; settings.loadTfaStatus(); } : () => settings.handleDoneWithCodes()}>
						{t('settings.twoFactor.savedDone')}
					</button>
				</div>

			{:else if settings.tfaSetupStep === 'disabled-confirm'}
				<div class="tfa-panel" in:fly={{ y: 6, duration: 200 }}>
					<p class="hint">{t('settings.twoFactor.disableHint')}</p>
					<div class="input-row">
						<input
							type="text"
							class="input"
							bind:value={settings.tfaDisableCode}
							inputmode="numeric"
							maxlength={6}
							placeholder={t('settings.twoFactor.codePlaceholder')}
							autocomplete="one-time-code"
							disabled={settings.tfaSubmitting}
						/>
						<button class="btn-danger" disabled={settings.tfaSubmitting || settings.tfaDisableCode.trim().length !== 6} onclick={() => settings.handleDisableTfa()}>
							{settings.tfaSubmitting ? t('settings.twoFactor.disabling') : t('common.disable')}
						</button>
						<button class="btn-ghost" disabled={settings.tfaSubmitting} onclick={() => { settings.tfaSetupStep = 'idle'; settings.tfaDisableCode = ''; }}>
							{t('common.cancel')}
						</button>
					</div>
				</div>

			{:else if settings.tfaStatus?.enabled && settings.tfaSetupStep === 'idle'}
				<div class="tfa-actions">
					<button class="link-btn" disabled={settings.tfaSubmitting} onclick={() => settings.handleRegenCodes()}>
						{t('settings.twoFactor.regenerate')}
					</button>
					<span class="dot-sep" aria-hidden="true">·</span>
					<button class="link-btn danger" onclick={() => { settings.tfaSetupStep = 'disabled-confirm'; settings.tfaError = ''; }}>
						{t('common.disable')}
					</button>
				</div>
			{/if}
		</section>

		<!-- Integrations -->
		<section class="section">
			<p class="section-label">{t('settings.sections.integrations')}</p>

			<div class="setting-row">
				<div class="setting-info col">
					<span class="setting-value">{t('settings.integrations.strava')}</span>
					{#if stravaStore.connection}
						<span class="setting-sub">{stravaStore.connection.firstname} {stravaStore.connection.lastname}</span>
					{:else}
						<span class="setting-sub">{t('settings.integrations.stravaConnect')}</span>
					{/if}
				</div>

				{#if stravaStore.connection}
					{#if confirmDisconnect}
						<div class="inline-actions">
							<button class="link-btn danger" disabled={stravaStore.loading} onclick={handleStravaDisconnect}>
								{stravaStore.loading ? '…' : t('settings.integrations.disconnect')}
							</button>
							<button class="link-btn" disabled={stravaStore.loading} onclick={() => { confirmDisconnect = false; }}>
								{t('common.cancel')}
							</button>
						</div>
					{:else}
						<button class="link-btn" onclick={() => { confirmDisconnect = true; }}>{t('settings.integrations.disconnect')}</button>
					{/if}
				{:else if stravaComingSoon}
					<span class="setting-sub" in:fly={{ x: 4, duration: 150 }}>{t('common.comingSoon')}</span>
				{:else}
					<button class="link-btn" onclick={handleStravaConnect}>{t('settings.integrations.connect')}</button>
				{/if}
			</div>
		</section>

		<!-- Sessions -->
		<section class="section sessions">
			<div class="setting-row">
				<div class="setting-info col">
					<p class="section-label">{t('settings.sections.sessions')}</p>
					<span class="setting-sub">{t('settings.sessions.subtitle')}</span>
				</div>
				<button class="link-btn" disabled={settings.sessionsLoading} onclick={() => settings.loadSessions()} title={t('settings.sessions.refresh')}>
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"
						stroke-linecap="round" stroke-linejoin="round" class="refresh-icon"
						class:spinning={settings.sessionsLoading} aria-hidden="true">
						<path d="M3 12a9 9 0 0 1 15.5-6.2L21 8" />
						<path d="M21 3v5h-5" />
						<path d="M21 12a9 9 0 0 1-15.5 6.2L3 16" />
						<path d="M3 21v-5h5" />
					</svg>
				</button>
			</div>

			{#if settings.sessionsError}
				<p class="hint hint-err" in:fly={{ y: -4, duration: 150 }}>{settings.sessionsError}</p>
			{/if}

			{#if settings.sessionsLoading && settings.sessions.length === 0}
				<div class="session-skel-list">
					{#each Array(2) as _, i (i)}
						<div class="session-skel"></div>
					{/each}
				</div>
			{:else if settings.sessions.length === 0}
				<p class="hint">{t('settings.sessions.empty')}</p>
			{:else}
				<ul class="session-list">
					{#each settings.sessions as session (session.id)}
						<li class="session" class:current={session.current}
							in:fly|local={{ y: 6, duration: 220 }}>
							<div class="session-icon" aria-hidden="true">
								<DeviceIcon platform={session.platform} />
							</div>
							<div class="session-body">
								<div class="session-line-1">
									<span class="session-device">{sessionDevice(session)}</span>
									{#if session.current}
										<span class="session-badge current-badge">{t('settings.sessions.current')}</span>
									{:else if session.countryCode}
										<span class="session-badge country">{session.countryCode}</span>
									{/if}
								</div>
								<div class="session-line-2">
									<span class="session-loc">{sessionLocation(session)}</span>
								</div>
								<div class="session-line-3">
									<span>{t('settings.sessions.signedIn')} · {formatRelativeTime(session.createdAt)}</span>
									<span class="dot-sep" aria-hidden="true">·</span>
									<span>{t('settings.sessions.lastSeen')} · {formatRelativeTime(session.lastSeenAt)}</span>
								</div>
							</div>
							{#if !session.current}
								<div class="session-action">
									{#if confirmRevokeId === session.id}
										<div class="inline-actions" in:fly={{ x: 4, duration: 140 }}>
											<button
												class="link-btn danger"
												disabled={settings.revokingSessionId === session.id}
												onclick={() => handleRevokeSession(session.id)}
											>
												{settings.revokingSessionId === session.id ? t('settings.sessions.revoking') : t('settings.sessions.revoke')}
											</button>
											<button
												class="link-btn"
												disabled={settings.revokingSessionId === session.id}
												onclick={() => (confirmRevokeId = null)}
											>
												{t('common.cancel')}
											</button>
										</div>
									{:else}
										<button class="link-btn danger" onclick={() => (confirmRevokeId = session.id)}>
											{t('settings.sessions.revoke')}
										</button>
									{/if}
								</div>
							{/if}
						</li>
					{/each}
				</ul>
			{/if}
		</section>

		<!-- Account -->
		<section class="section section-last">
			<button class="link-btn danger" onclick={() => settings.handleLogout((path) => goto(path))}>{t('settings.signOut')}</button>
		</section>
	</main>

	<aside class="col-right" aria-hidden="true"></aside>
</div>

<style>
	/* ── Layout ───────────────────────────────────────────────────────────── */

	.page-layout {
		height: 100vh;
		overflow: hidden;
	}

	.col-left {
		position: fixed;
		right: calc(50% + 21rem);
		top: 0;
		bottom: 0;
		width: 16rem;
		overflow-y: auto;
		padding: 0 1.5rem 0 1rem;
		z-index: 20;
	}

	.col-center {
		height: 100vh;
		max-width: 42rem;
		margin: 0 auto;
		overflow-y: auto;
		padding: 0 2rem 6rem;
		scrollbar-width: none;
	}

	.col-center::-webkit-scrollbar { display: none; }

	.col-right {
		display: none;
	}

	@media (max-width: 900px) {
		.col-left { width: 4.5rem; padding: 0 0.5rem; }
	}

	@media (max-width: 640px) {
		.col-left { display: none; }
		.col-center { padding: 0 1rem 4rem; }
	}

	/* ── Header ───────────────────────────────────────────────────────────── */

	.page-header {
		padding: 1.25rem 0 1.75rem;
		margin-bottom: -0.75rem;
		position: sticky;
		top: 0;
		background: linear-gradient(
			to bottom,
			var(--color-bg) 0%,
			rgba(12, 12, 12, 0.85) 45%,
			rgba(12, 12, 12, 0) 100%
		);
		backdrop-filter: blur(var(--glass-blur-sm)) saturate(var(--glass-saturate-soft));
		-webkit-backdrop-filter: blur(var(--glass-blur-sm)) saturate(var(--glass-saturate-soft));
		-webkit-mask-image: linear-gradient(to bottom, #000 0%, #000 55%, transparent 100%);
		mask-image: linear-gradient(to bottom, #000 0%, #000 55%, transparent 100%);
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
		padding: 1.125rem 1.25rem;
		margin-bottom: 0.75rem;
		border-radius: 1rem;
		background: var(--glass-bg-soft);
		backdrop-filter: blur(calc(var(--glass-blur) + 4px)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(calc(var(--glass-blur) + 4px)) saturate(var(--glass-saturate));
		border: 1px solid var(--glass-border);
		box-shadow: inset 0 1px 0 var(--glass-highlight);
		display: flex;
		flex-direction: column;
		gap: 0.625rem;
		transition: box-shadow 0.2s ease, transform 0.16s ease;
	}

	.section:hover {
		box-shadow: inset 0 1px 0 var(--glass-highlight),
			0 0 0 1px var(--glass-highlight);
	}

	.section-last {
		margin-bottom: 0;
	}

	@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
		.section {
			background: var(--color-surface);
		}
	}

	.section-label {
		font-size: 0.6875rem;
		font-weight: 600;
		color: var(--color-text-muted);
		margin: 0;
		letter-spacing: 0.06em;
		text-transform: uppercase;
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

	/* ── Sessions ─────────────────────────────────────────────────────────── */

	.session-list {
		list-style: none;
		padding: 0;
		margin: 0;
		display: flex;
		flex-direction: column;
		gap: 0;
	}

	.session {
		display: grid;
		grid-template-columns: auto 1fr auto;
		align-items: center;
		gap: 0.875rem;
		padding: 0.75rem 0.5rem;
		border-radius: 0.625rem;
		background: transparent;
		border: none;
		transition: background-color 0.18s ease;
	}

	.session + .session {
		border-top: 1px solid var(--divider-faint);
	}

	.session:hover {
		background: var(--surface-tint-faint);
	}

	.session.current {
		background: transparent;
	}

	.session-icon {
		width: 2.25rem;
		height: 2.25rem;
		border-radius: 0.625rem;
		background: var(--surface-tint-medium);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		color: var(--text-strong);
		transition: transform 0.32s cubic-bezier(0.34, 1.56, 0.64, 1);
	}

	.session.current .session-icon {
		color: #10b981;
		background: rgba(16, 185, 129, 0.12);
	}

	.session:hover .session-icon {
		transform: scale(1.04);
	}

	.session-body {
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 0.1875rem;
	}

	.session-line-1 {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		min-width: 0;
	}

	.session-device {
		font-size: 0.9375rem;
		font-weight: 500;
		color: var(--color-text-primary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.session-badge {
		font-size: 0.625rem;
		font-weight: 700;
		letter-spacing: 0.06em;
		padding: 0.125rem 0.4375rem;
		border-radius: 999px;
		text-transform: uppercase;
		flex-shrink: 0;
	}

	.session-badge.current-badge {
		background: rgba(16, 185, 129, 0.14);
		color: #10b981;
	}

	.session-badge.country {
		background: var(--surface-tint-medium);
		color: var(--text-strong);
	}

	.session-line-2 {
		display: flex;
		align-items: center;
		gap: 0.375rem;
		font-size: 0.8125rem;
		color: var(--color-text-secondary);
	}

	.session-loc {
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.session-line-3 {
		display: flex;
		align-items: center;
		gap: 0.375rem;
		font-size: 0.75rem;
		color: var(--color-text-muted);
	}

	.session-action {
		display: flex;
		align-items: center;
		flex-shrink: 0;
	}

	.sessions .setting-row .link-btn {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 1.75rem;
		height: 1.75rem;
		border-radius: 999px;
		color: var(--color-text-secondary);
	}

	.sessions .setting-row .link-btn:hover:not(:disabled) {
		color: var(--color-text-primary);
		background: var(--surface-tint-soft);
		opacity: 1;
	}

	.refresh-icon {
		width: 0.9375rem;
		height: 0.9375rem;
		transition: transform 0.5s cubic-bezier(0.4, 0, 0.2, 1);
	}

	.refresh-icon.spinning {
		animation: spin 0.9s linear infinite;
	}

	@keyframes spin {
		to { transform: rotate(360deg); }
	}

	.session-skel-list {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	.session-skel {
		height: 4.25rem;
		border-radius: 0.75rem;
		background: var(--color-skeleton);
		animation: pulse 1.4s ease-in-out infinite;
	}

	@media (max-width: 520px) {
		.session-line-3 {
			flex-wrap: wrap;
		}
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
