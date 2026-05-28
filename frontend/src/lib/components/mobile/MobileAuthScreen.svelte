<script lang="ts">
	import { goto } from '$app/navigation';
	import PasswordStrengthIndicator from '$lib/components/PasswordStrengthIndicator.svelte';
	import { authService } from '$lib/services/auth.service';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { verifyEmailStore } from '$lib/stores/verify-email.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import {
		ApiError,
		isTwoFactorChallenge,
		type LoginRequest,
		type RegisterRequest
	} from '$lib/types';
	import { t } from '$lib/i18n';

	type AuthMode = 'login' | 'register';

	interface Props {
		initialMode?: AuthMode;
		onClose?: () => void;
	}

	let { initialMode = 'login', onClose }: Props = $props();

	let activeMode = $state<AuthMode>(initialMode);
	let username = $state('');
	let email = $state('');
	let password = $state('');
	let challengeToken = $state<string | null>(null);
	let totpCode = $state('');
	let error = $state('');
	let loading = $state(false);
	let passwordTouched = $state(false);
	let shakeKey = $state(0);

	const hasError = $derived(error.length > 0);
	const isSubmitDisabled = $derived(
		loading ||
			(challengeToken
				? totpCode.trim().length === 0
				: username.trim().length === 0 ||
				  password.length === 0 ||
				  (activeMode === 'register' && email.trim().length === 0))
	);
	const passwordScore = $derived(computeScore(password));
	const showPasswordHint = $derived(
		activeMode === 'register' && passwordTouched && password.length > 0 && password.length < 8
	);

	function switchMode(mode: AuthMode): void {
		if (mode === activeMode) return;
		activeMode = mode;
		username = '';
		email = '';
		password = '';
		challengeToken = null;
		totpCode = '';
		error = '';
		passwordTouched = false;
	}

	function computeScore(pwd: string): number {
		if (pwd.length === 0) return 0;
		let score = 0;
		if (pwd.length >= 8) score++;
		if (/[0-9]/.test(pwd)) score++;
		if (/[A-Z]/.test(pwd)) score++;
		if (/[^A-Za-z0-9]/.test(pwd)) score++;
		return score;
	}

	function mapError(err: unknown): string {
		if (err instanceof ApiError) {
			if (challengeToken && err.isUnauthorized) return t('auth.errors.invalid2FA');
			if (activeMode === 'login' && err.isUnauthorized) return t('auth.errors.invalidCredentials');
			if (activeMode === 'register' && err.message?.toLowerCase().includes('already taken')) {
				return t('auth.register.usernameTakenError');
			}
			return err.message || t('common.somethingWentWrong');
		}
		if (err instanceof Error) return err.message;
		return t('common.somethingWentWrong');
	}

	function bumpError(message: string): void {
		error = message;
		shakeKey += 1;
		getTelegramWebApp()?.HapticFeedback.notificationOccurred('error');
	}

	function handlePasswordInput(): void {
		passwordTouched = true;
		if (error) error = '';
	}

	function resetChallenge(): void {
		challengeToken = null;
		totpCode = '';
		error = '';
	}

	async function handleSubmit(): Promise<void> {
		if (loading) return;

		if (activeMode === 'register' && passwordScore < 2) {
			bumpError(t('auth.register.weakPasswordError'));
			return;
		}

		error = '';
		loading = true;

		try {
			if (challengeToken) {
				const user = await authService.verifyTwoFactor({ challengeToken, code: totpCode });
				await authStore.login(user);
				getTelegramWebApp()?.HapticFeedback.notificationOccurred('success');
				return;
			}

			if (activeMode === 'login') {
				const req: LoginRequest = { username, password };
				const result = await authService.login(req);
				if (isTwoFactorChallenge(result)) {
					challengeToken = result.challengeToken;
					totpCode = '';
					return;
				}
				await authStore.login(result);
			} else {
				const req: RegisterRequest = {
					username: username.trim(),
					password,
					email: email.trim(),
				};
				const user = await authService.register(req);
				await authStore.login(user);
				getTelegramWebApp()?.HapticFeedback.notificationOccurred('success');
				if (!user.emailVerified) {
					verifyEmailStore.start(email.trim());
					await goto('/verify-email');
					return;
				}
			}

			getTelegramWebApp()?.HapticFeedback.notificationOccurred('success');
		} catch (err: unknown) {
			bumpError(mapError(err));
		} finally {
			loading = false;
		}
	}
</script>

<div class="auth-page">
	{#if onClose}
		<button
			type="button"
			class="close-btn"
			aria-label={t('common.close')}
			onclick={() => onClose?.()}
		>
			<svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.2" aria-hidden="true">
				<line x1="18" y1="6" x2="6" y2="18" />
				<line x1="6" y1="6" x2="18" y2="18" />
			</svg>
		</button>
	{/if}
	<div class="auth-shell">
		<div class="auth-card glass glass-strong" aria-label={activeMode === 'login' ? t('auth.login.title') : t('auth.register.title')}>
			<div class="card-header">
				<span class="logo-text">critiqal</span>
				<p class="subtitle">
					{#if challengeToken}
						{t('auth.login.twoFactorHint')}
					{:else}
						{activeMode === 'login' ? t('auth.login.subtitle') : t('auth.register.subtitle')}
					{/if}
				</p>
			</div>

			{#if hasError}
				{#key shakeKey}
					<div class="error-box shake" role="alert">
						{error}
					</div>
				{/key}
			{/if}

			<form class="form" onsubmit={(event) => {
				event.preventDefault();
				void handleSubmit();
			}}>
				{#if challengeToken}
					<div class="field">
						<label for="mobile-auth-code" class="field-label">{t('auth.login.twoFactorTitle')}</label>
						<input
							id="mobile-auth-code"
							type="text"
							class="field-input"
							bind:value={totpCode}
							autocomplete="one-time-code"
							inputmode="numeric"
							pattern={'[0-9]{6}'}
							required
							disabled={loading}
							placeholder={t('auth.login.twoFactorPlaceholder')}
							oninput={() => {
								if (error) error = '';
							}}
						/>
					</div>
				{:else}
					<div class="field">
						<label for="mobile-auth-username" class="field-label">{t('auth.register.username')}</label>
						<input
							id="mobile-auth-username"
							type="text"
							class="field-input"
							bind:value={username}
							autocomplete="username"
							autocapitalize="none"
							spellcheck={false}
							required
							minlength={3}
							maxlength={30}
							disabled={loading}
							oninput={() => {
								if (error) error = '';
							}}
						/>
					</div>

					{#if activeMode === 'register'}
						<div class="field">
							<label for="mobile-auth-email" class="field-label">{t('auth.register.email')}</label>
							<input
								id="mobile-auth-email"
								type="email"
								class="field-input"
								bind:value={email}
								autocomplete="email"
								autocapitalize="none"
								spellcheck={false}
								required
								disabled={loading}
								placeholder="you@example.com"
								oninput={() => { if (error) error = ''; }}
							/>
						</div>
					{/if}

					<div class="field">
						<label for="mobile-auth-password" class="field-label">{t('auth.login.password')}</label>
						<input
							id="mobile-auth-password"
							type="password"
							class="field-input"
							bind:value={password}
							autocomplete={activeMode === 'login' ? 'current-password' : 'new-password'}
							required
							minlength={activeMode === 'register' ? 8 : 1}
							disabled={loading}
							placeholder="••••••••"
							oninput={handlePasswordInput}
						/>

						{#if activeMode === 'register'}
							<div class="password-meta">
								<PasswordStrengthIndicator {password} />
								{#if showPasswordHint}
									<p class="field-hint">{t('auth.register.passwordHint')}</p>
								{/if}
							</div>
						{/if}
					</div>
				{/if}

				<button type="submit" class="submit-btn" disabled={isSubmitDisabled}>
					{#if loading}
						{#if challengeToken}
							{t('auth.login.submitting')}
						{:else}
							{activeMode === 'login' ? t('auth.login.submitting') : t('auth.register.submitting')}
						{/if}
					{:else}
						{#if challengeToken}
							{t('auth.login.submit')}
						{:else}
							{activeMode === 'login' ? t('auth.login.submit') : t('auth.register.submit')}
						{/if}
					{/if}
				</button>

				{#if challengeToken}
					<button type="button" class="ghost-btn" disabled={loading} onclick={resetChallenge}>
						{t('common.back')}
					</button>
				{/if}
			</form>

			{#if !challengeToken}
				<p class="switch-link">
					{#if activeMode === 'login'}
						{t('auth.login.noAccount')}
						<button type="button" class="switch-btn" onclick={() => switchMode('register')}>
							{t('auth.login.createOne')}
						</button>
					{:else}
						{t('auth.register.haveAccount')}
						<button type="button" class="switch-btn" onclick={() => switchMode('login')}>
							{t('auth.register.signIn')}
						</button>
					{/if}
				</p>
			{/if}
		</div>
	</div>
</div>

<style>
	.auth-page {
		position: relative;
		height: 100%;
		background: var(--color-bg);
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
	}

	.close-btn {
		position: absolute;
		top: calc(var(--tg-content-top, env(safe-area-inset-top, 0px)) + 0.85rem);
		right: 1rem;
		z-index: 5;
		width: 2.25rem;
		height: 2.25rem;
		border-radius: 50%;
		border: none;
		background: var(--color-surface);
		box-shadow: inset 0 0 0 1px var(--divider-soft);
		color: var(--color-text-secondary);
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		transform-origin: center;
		will-change: transform;
		transition:
			background-color var(--duration-micro) var(--ease-out-quart),
			color var(--duration-micro) var(--ease-out-quart),
			box-shadow var(--duration-micro) var(--ease-out-quart),
			transform var(--duration-press) var(--ease-out-quart);
	}

	.close-btn:hover {
		background: var(--color-surface-raised);
		color: var(--color-text-primary);
		box-shadow: inset 0 0 0 1px var(--divider-strong);
	}

	.close-btn:active { transform: scale(0.95); }

	.auth-shell {
		min-height: 100%;
		display: flex;
		align-items: center;
		justify-content: flex-start;
		padding:
			calc(var(--tg-content-top, env(safe-area-inset-top, 0px)) + 4.75rem)
			1.5rem
			max(2rem, calc(env(safe-area-inset-bottom, 0px) + 1.5rem));
	}

	.auth-card {
		width: 100%;
		max-width: 21.8rem;
		border-radius: 1.1rem;
		padding: 2.4rem 1.55rem 1.7rem;
		display: flex;
		flex-direction: column;
		gap: 1.35rem;
		box-shadow:
			inset 0 1px 0 var(--surface-tint-strong),
			0 1px 2px rgba(0, 0, 0, 0.06),
			0 24px 48px -16px rgba(0, 0, 0, 0.35);
	}

	.card-header {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.6rem;
	}

	.logo-text {
		font-size: 2rem;
		font-weight: 700;
		line-height: 1;
		letter-spacing: -0.04em;
		color: var(--color-text-primary);
	}

	.subtitle {
		margin: 0;
		font-size: 0.95rem;
		color: var(--text-tertiary);
		text-align: center;
	}

	.error-box {
		background: rgba(224, 82, 82, 0.08);
		border: 1px solid rgba(224, 82, 82, 0.2);
		border-radius: 0.75rem;
		padding: 0.75rem 0.9rem;
		font-size: 0.875rem;
		line-height: 1.4;
		color: var(--color-accent);
	}

	.form {
		display: flex;
		flex-direction: column;
		gap: 1rem;
	}

	.field {
		display: flex;
		flex-direction: column;
		gap: 0.45rem;
	}

	.field-label {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--text-secondary-2);
	}

	.field-input {
		width: 100%;
		height: 3.2rem;
		background: var(--color-surface);
		border: none;
		box-shadow:
			inset 0 1px 0 var(--surface-tint-soft),
			inset 0 0 0 1px var(--glass-border),
			0 1px 2px rgba(0, 0, 0, 0.08);
		border-radius: 0.85rem;
		padding: 0 0.9rem;
		font-size: 1rem;
		color: var(--color-text-primary);
		font-family: inherit;
		outline: none;
		box-sizing: border-box;
		transition:
			box-shadow var(--duration-micro) var(--ease-out-quart),
			background-color var(--duration-micro) var(--ease-out-quart);
	}

	.field-input::placeholder {
		color: var(--text-quaternary);
	}

	.field-input:focus {
		background: var(--color-surface-raised);
		box-shadow:
			inset 0 1px 0 var(--surface-tint-strong),
			inset 0 0 0 1.5px var(--color-text-primary),
			0 0 0 4px rgba(255, 255, 255, 0.05),
			0 4px 12px rgba(0, 0, 0, 0.18);
	}

	.field-input:disabled {
		opacity: 0.58;
		cursor: not-allowed;
	}

	.password-meta {
		display: flex;
		flex-direction: column;
		gap: 0.45rem;
	}

	.field-hint {
		margin: 0;
		font-size: 0.78rem;
		color: var(--text-faint);
	}

	.submit-btn {
		width: 100%;
		height: 3.2rem;
		border-radius: 0.72rem;
		border: none;
		background: #c92a2a;
		color: #ffffff;
		font-size: 1rem;
		font-weight: 700;
		font-family: inherit;
		cursor: pointer;
		box-shadow: 0 6px 18px rgba(201, 42, 42, 0.38);
		transition:
			transform 0.12s ease,
			opacity 0.15s ease,
			box-shadow 0.15s ease;
		margin-top: 0.15rem;
	}

	.submit-btn:not(:disabled):active {
		transform: scale(0.992);
	}

	.submit-btn:disabled {
		opacity: 0.52;
		cursor: not-allowed;
	}

	.ghost-btn {
		width: 100%;
		height: 3rem;
		border-radius: 0.72rem;
		border: none;
		box-shadow: inset 0 0 0 1px var(--divider-soft);
		background: transparent;
		color: var(--color-text-primary);
		font-size: 0.94rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition:
			background-color var(--duration-micro) var(--ease-out-quart),
			box-shadow var(--duration-micro) var(--ease-out-quart),
			opacity var(--duration-micro) var(--ease-out-quart);
	}

	.ghost-btn:hover:not(:disabled) {
		background: var(--surface-tint-soft);
		box-shadow: inset 0 0 0 1px var(--divider-strong);
	}

	.ghost-btn:disabled {
		opacity: 0.52;
		cursor: not-allowed;
	}

	.switch-link {
		margin: 0;
		text-align: center;
		font-size: 0.98rem;
		line-height: 1.45;
		color: var(--text-tertiary);
	}

	.switch-btn {
		border: none;
		background: none;
		padding: 0;
		margin-left: 0.25rem;
		font: inherit;
		font-weight: 700;
		color: #c92a2a;
		-webkit-tap-highlight-color: transparent;
	}

	.shake {
		animation: shake 0.32s ease-in-out;
	}

	@keyframes shake {
		0%, 100% { transform: translateX(0); }
		20% { transform: translateX(-5px); }
		40% { transform: translateX(4px); }
		60% { transform: translateX(-3px); }
		80% { transform: translateX(2px); }
	}
</style>
