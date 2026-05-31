<script lang="ts">
	import { goto } from '$app/navigation';
	import { fly } from 'svelte/transition';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { authService } from '$lib/services/auth.service';
	import { recoveryService } from '$lib/services/recovery.service';
	import { emailVerificationService } from '$lib/services/email-verification.service';
	import { ApiError, isTwoFactorChallenge, isBanResponse } from '$lib/types';
	import type { User, BanInfo } from '$lib/types';
	import { verifyEmailStore } from '$lib/stores/verify-email.store.svelte';
	import { t } from '$lib/i18n';
	import StarfieldBackdrop from '$lib/ui/StarfieldBackdrop.svelte';
	import BannedScreen from '$lib/components/BannedScreen.svelte';

	type AuthMode = 'credentials' | 'totp' | 'email' | 'recovery';
	type Step = 'auth' | 'onboarding';

	let step = $state<Step>('auth');
	let mode = $state<AuthMode>('credentials');
	let banInfo = $state<BanInfo | null>(null);

	// ── Auth ─────────────────────────────────────────────────────────────────
	let username = $state('');
	let password = $state('');
	let challengeToken = $state<string | null>(null);
	let totpCode = $state('');
	let emailCode = $state('');
	let recoveryCode = $state('');
	let isSubmitting = $state(false);
	let error = $state('');
	let shakeKey = $state(0);

	function mapError(err: unknown): string {
		if (err instanceof ApiError) {
			if (err.isUnauthorized) {
				if (mode === 'totp' || mode === 'email' || mode === 'recovery') {
					return t('auth.errors.invalid2FA');
				}
				return t('auth.errors.invalidCredentials');
			}
			return err.message || t('common.somethingWentWrong');
		}
		if (err instanceof Error) return err.message;
		return t('common.somethingWentWrong');
	}

	async function routeAfterLogin(user: User): Promise<void> {
		if (user.emailVerified) {
			await goto('/');
			return;
		}
		if (user.pendingEmail) {
			verifyEmailStore.start(user.pendingEmail);
			await goto('/verify-email');
			return;
		}
		step = 'onboarding';
	}

	function resetChallenge(): void {
		challengeToken = null; totpCode = ''; emailCode = ''; recoveryCode = '';
		mode = 'credentials'; error = '';
	}

	async function handleSubmit(): Promise<void> {
		isSubmitting = true;
		error = '';
		try {
			if (mode === 'totp' && challengeToken) {
				const user = await authService.verifyTwoFactor({ challengeToken, code: totpCode });
				await authStore.login(user);
				await routeAfterLogin(user);
				return;
			}

			if (mode === 'email' && challengeToken) {
				const user = await authService.verifyEmailLogin({ challengeToken, code: emailCode });
				await authStore.login(user);
				await routeAfterLogin(user);
				return;
			}

			if (mode === 'recovery') {
				const user = await recoveryService.useRecoveryCode({ username, recoveryCode });
				await authStore.login(user);
				await routeAfterLogin(user);
				return;
			}

			const result = await authService.login({ username, password });
			if (isTwoFactorChallenge(result)) {
				challengeToken = result.challengeToken;
				if (result.method === 'EMAIL') {
					emailCode = '';
					mode = 'email';
				} else {
					totpCode = '';
					mode = 'totp';
				}
				return;
			}

			await authStore.login(result);
			await routeAfterLogin(result);
		} catch (err: unknown) {
			if (err instanceof ApiError && err.status === 403 && isBanResponse(err.body)) {
				banInfo = err.body;
				return;
			}
			error = mapError(err);
			shakeKey++;
		} finally {
			isSubmitting = false;
		}
	}

	// ── Onboarding ───────────────────────────────────────────────────────────
	let emailInput = $state('');
	let emailSubmitting = $state(false);

	async function handleAddEmail(): Promise<void> {
		const value = emailInput.trim();
		if (!value) return;
		emailSubmitting = true;
		try {
			await emailVerificationService.setEmail({ email: value });
			verifyEmailStore.start(value);
			await goto('/verify-email');
		} catch {
			// non-blocking
			await goto('/');
		} finally {
			emailSubmitting = false;
		}
	}

	function skip(): void { goto('/'); }
</script>

<svelte:head>
	<title>{t('auth.login.title')} — Critiqal</title>
	<meta name="description" content="Sign in to your Critiqal account" />
</svelte:head>

<div class="page">
	<StarfieldBackdrop />

	{#if banInfo}
		<div class="card glass glass-strong" in:fly={{ y: 10, duration: 220 }}>
			<BannedScreen reason={banInfo.reason} expiresAt={banInfo.expiresAt} />
		</div>
	{:else if step === 'auth'}
		<div class="card glass glass-strong" aria-label={t('auth.login.title')} in:fly={{ y: 10, duration: 220 }}>
			<div class="card-header">
				<span class="logo">critiqal</span>
				<p class="subtitle">
					{#if mode === 'totp'}
						{t('auth.login.twoFactorHint')}
					{:else if mode === 'email'}
						{t('auth.login.emailCodeHint')}
					{:else if mode === 'recovery'}
						{t('auth.login.recoveryPlaceholder')}
					{:else}
						{t('auth.login.subtitle')}
					{/if}
				</p>
			</div>

			{#if error}
				{#key shakeKey}
					<div class="error shake" role="alert">{error}</div>
				{/key}
			{/if}

			<form class="form" onsubmit={(e) => { e.preventDefault(); handleSubmit(); }}>
				{#if mode === 'totp'}
					<div class="field">
						<label for="totp-code" class="field-label">{t('auth.login.twoFactorTitle')}</label>
						<input
							id="totp-code"
							type="text"
							class="field-input"
							bind:value={totpCode}
							autocomplete="one-time-code"
							inputmode="numeric"
							maxlength="6"
							required
							disabled={isSubmitting}
							placeholder={t('auth.login.twoFactorPlaceholder')}
						/>
					</div>
				{:else if mode === 'email'}
					<div class="field">
						<label for="email-code" class="field-label">{t('auth.login.emailCodeTitle')}</label>
						<input
							id="email-code"
							type="text"
							class="field-input"
							bind:value={emailCode}
							autocomplete="one-time-code"
							inputmode="numeric"
							maxlength="6"
							required
							disabled={isSubmitting}
							placeholder={t('auth.login.emailCodePlaceholder')}
						/>
					</div>
				{:else if mode === 'recovery'}
					<div class="field">
						<label for="rec-username" class="field-label">{t('auth.register.username')}</label>
						<input id="rec-username" type="text" class="field-input" bind:value={username}
							autocomplete="username" required disabled={isSubmitting} />
					</div>
					<div class="field">
						<label for="rec-code" class="field-label">{t('auth.login.recoveryPlaceholder')}</label>
						<input id="rec-code" type="text" class="field-input" bind:value={recoveryCode}
							autocomplete="off" required disabled={isSubmitting} placeholder="xxxxxxxx-xxxx" />
					</div>
				{:else}
					<div class="field">
						<label for="username" class="field-label">{t('auth.login.identifier')}</label>
						<input id="username" type="text" class="field-input" bind:value={username}
							autocomplete="username" required disabled={isSubmitting} />
					</div>
					<div class="field">
						<label for="password" class="field-label">{t('auth.login.password')}</label>
						<input id="password" type="password" class="field-input" bind:value={password}
							autocomplete="current-password" required disabled={isSubmitting} placeholder="••••••••" />
					</div>
					<a href="/forgot-password" class="forgot-link">{t('auth.login.forgot')}</a>
				{/if}

				<button
					type="submit"
					class="submit-btn"
					disabled={isSubmitting || (
						mode === 'totp' ? totpCode.trim().length !== 6 :
						mode === 'email' ? emailCode.trim().length !== 6 :
						mode === 'recovery' ? !username.trim() || !recoveryCode.trim() :
						!username.trim() || !password
					)}
				>
					{isSubmitting ? t('auth.login.submitting') : t('auth.login.submit')}
				</button>

				{#if mode === 'totp'}
					<button type="button" class="ghost-btn" disabled={isSubmitting}
						onclick={() => { mode = 'recovery'; totpCode = ''; error = ''; }}>
						{t('auth.login.useRecovery')}
					</button>
					<button type="button" class="ghost-btn" disabled={isSubmitting} onclick={resetChallenge}>
						{t('common.back')}
					</button>
				{:else if mode === 'email'}
					<button type="button" class="ghost-btn" disabled={isSubmitting} onclick={resetChallenge}>
						{t('common.back')}
					</button>
				{:else if mode === 'recovery'}
					<button type="button" class="ghost-btn" disabled={isSubmitting}
						onclick={() => { mode = 'totp'; recoveryCode = ''; error = ''; }}>
						{t('auth.login.useTotp')}
					</button>
					<button type="button" class="ghost-btn" disabled={isSubmitting} onclick={resetChallenge}>
						{t('common.back')}
					</button>
				{/if}
			</form>

			{#if mode === 'credentials'}
				<p class="switch-link">{t('auth.login.noAccount')} <a href="/register">{t('auth.login.createOne')}</a></p>
			{/if}
		</div>

	{:else}
		<div class="card glass glass-strong" aria-label={t('auth.onboarding.title')} in:fly={{ y: 10, duration: 220 }}>
			<div class="card-header">
				<div class="onboarding-icon" aria-hidden="true">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="20" height="20">
						<path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
						<polyline points="22,6 12,13 2,6"/>
					</svg>
				</div>
				<p class="onboarding-title">{t('auth.onboarding.title')}</p>
				<p class="onboarding-sub">{t('auth.onboarding.subtitle')}</p>
			</div>

			<form class="form" onsubmit={(e) => { e.preventDefault(); handleAddEmail(); }}>
				<div class="field">
					<label for="email" class="field-label">{t('auth.onboarding.emailLabel')}</label>
					<input
						id="email"
						type="email"
						class="field-input"
						bind:value={emailInput}
						autocomplete="email"
						disabled={emailSubmitting}
						placeholder={t('auth.onboarding.emailPlaceholder')}
					/>
				</div>

				<button type="submit" class="submit-btn" disabled={emailSubmitting || !emailInput.trim()}>
					{emailSubmitting ? t('auth.onboarding.submitting') : t('auth.onboarding.submit')}
				</button>
			</form>

			<button type="button" class="skip-btn" onclick={skip} disabled={emailSubmitting}>
				{t('auth.onboarding.skip')}
			</button>
		</div>
	{/if}
</div>

<style>
	.page {
		min-height: 100vh;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 1.5rem;
		background: var(--color-bg);
		position: relative;
		overflow: hidden;
	}

	.card {
		position: relative;
		z-index: 1;
		width: 100%;
		max-width: 22rem;
		border-radius: 1rem;
		padding: 2.5rem;
		display: flex;
		flex-direction: column;
		gap: 1.5rem;
		box-shadow:
			inset 0 1px 0 var(--surface-tint-strong),
			0 1px 2px rgba(0, 0, 0, 0.06),
			0 24px 48px -16px rgba(0, 0, 0, 0.35);
		animation: fadeUp 0.25s ease-out;
	}

	.card-header {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
	}

	.logo {
		font-size: 1.25rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.03em;
	}

	.subtitle {
		font-size: 0.875rem;
		color: var(--color-text-muted);
		margin: 0;
		text-align: center;
	}

	/* ── Onboarding ──────────────────────────────────────────────────────── */

	.onboarding-icon {
		width: 2.75rem;
		height: 2.75rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		color: var(--color-text-muted);
		margin-bottom: 0.25rem;
	}

	.onboarding-title {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.01em;
		margin: 0;
		text-align: center;
	}

	.onboarding-sub {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		margin: 0;
		text-align: center;
		line-height: 1.5;
	}

	/* ── Form ────────────────────────────────────────────────────────────── */

	.form { display: flex; flex-direction: column; gap: 1rem; }

	.field { display: flex; flex-direction: column; gap: 0.375rem; }

	.field-label {
		font-size: 0.8125rem;
		font-weight: 500;
		color: var(--color-text-muted);
	}

	.field-input {
		width: 100%;
		background: var(--color-surface-raised);
		border: none;
		box-shadow:
			inset 0 1px 0 var(--surface-tint-soft),
			inset 0 0 0 1px var(--glass-border),
			0 1px 2px rgba(0, 0, 0, 0.08);
		border-radius: 0.625rem;
		padding: 0.7rem 0.85rem;
		font-size: 0.9375rem;
		color: var(--color-text-primary);
		font-family: inherit;
		outline: none;
		transition:
			box-shadow var(--duration-micro) var(--ease-out-quart),
			background-color var(--duration-micro) var(--ease-out-quart),
			transform var(--duration-micro) var(--ease-out-quart);
	}

	.field-input::placeholder { color: var(--color-text-muted); opacity: 0.5; }
	.field-input:hover:not(:disabled):not(:focus) {
		background: var(--color-surface-elevated, var(--color-surface-raised));
		box-shadow:
			inset 0 1px 0 var(--surface-tint-medium),
			inset 0 0 0 1px var(--surface-tint-medium),
			0 2px 6px rgba(0, 0, 0, 0.12);
	}
	.field-input:focus {
		background: var(--color-surface-elevated, var(--color-surface-raised));
		box-shadow:
			inset 0 1px 0 var(--surface-tint-strong),
			inset 0 0 0 1.5px var(--color-text-primary),
			0 0 0 4px rgba(255, 255, 255, 0.05),
			0 4px 12px rgba(0, 0, 0, 0.18);
	}
	.field-input:disabled { opacity: 0.5; cursor: not-allowed; }

	.forgot-link {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		text-decoration: none;
		text-align: right;
		display: block;
		transition: color 0.15s ease;
	}

	.forgot-link:hover { color: var(--color-text-primary); }

	.submit-btn {
		width: 100%;
		padding: 0.75rem 1rem;
		border-radius: 0.625rem;
		border: none;
		background: var(--color-text-primary);
		color: var(--color-bg);
		font-size: 0.9375rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		box-shadow:
			inset 0 1px 0 rgba(255, 255, 255, 0.15),
			0 1px 2px rgba(0, 0, 0, 0.1),
			0 6px 16px -4px rgba(0, 0, 0, 0.2);
		transition:
			transform var(--duration-press) var(--ease-out-quart),
			box-shadow var(--duration-micro) var(--ease-out-quart),
			opacity var(--duration-micro) var(--ease-out-quart);
		margin-top: 0.25rem;
	}

	.submit-btn:hover:not(:disabled) {
		transform: translateY(-1px);
		box-shadow:
			inset 0 1px 0 rgba(255, 255, 255, 0.2),
			0 2px 4px rgba(0, 0, 0, 0.12),
			0 10px 24px -6px rgba(0, 0, 0, 0.28);
	}
	.submit-btn:active:not(:disabled) { transform: scale(0.97); }
	.submit-btn:disabled { opacity: 0.4; cursor: not-allowed; }

	.ghost-btn {
		width: 100%;
		padding: 0.625rem 1rem;
		border-radius: 0.5rem;
		border: none;
		box-shadow: inset 0 0 0 1px var(--divider-soft);
		background: transparent;
		color: var(--color-text-primary);
		font-size: 0.875rem;
		font-weight: 500;
		font-family: inherit;
		cursor: pointer;
		transition:
			background-color var(--duration-micro) var(--ease-out-quart),
			box-shadow var(--duration-micro) var(--ease-out-quart);
	}

	.ghost-btn:hover:not(:disabled) {
		background: var(--surface-tint-soft);
		box-shadow: inset 0 0 0 1px var(--divider-strong);
	}
	.ghost-btn:disabled { opacity: 0.4; cursor: not-allowed; }

	.skip-btn {
		background: none;
		border: none;
		font-size: 0.875rem;
		color: var(--color-text-muted);
		font-family: inherit;
		cursor: pointer;
		padding: 0;
		text-align: center;
		transition: color 0.15s ease;
	}

	.skip-btn:hover:not(:disabled) { color: var(--color-text-primary); }
	.skip-btn:disabled { opacity: 0.4; cursor: not-allowed; }

	/* ── Feedback ────────────────────────────────────────────────────────── */

	.error {
		background: rgba(224, 82, 82, 0.08);
		border: 1px solid rgba(224, 82, 82, 0.2);
		border-radius: 0.5rem;
		padding: 0.75rem 1rem;
		font-size: 0.875rem;
		color: var(--color-accent);
		line-height: 1.4;
	}

	.switch-link {
		font-size: 0.875rem;
		color: var(--color-text-muted);
		text-align: center;
		margin: 0;
	}

	.switch-link a {
		color: var(--color-text-primary);
		text-decoration: none;
		font-weight: 500;
	}

	.switch-link a:hover { opacity: 0.75; }

	/* ── Animations ──────────────────────────────────────────────────────── */

	@keyframes fadeUp {
		from { opacity: 0; transform: translateY(0.5rem); }
		to { opacity: 1; transform: translateY(0); }
	}

	.shake { animation: errorShake 0.3s ease-out; }

	@keyframes errorShake {
		0% { transform: translateX(0); }
		20% { transform: translateX(-4px); }
		40% { transform: translateX(4px); }
		60% { transform: translateX(-4px); }
		80% { transform: translateX(2px); }
		100% { transform: translateX(0); }
	}
</style>
