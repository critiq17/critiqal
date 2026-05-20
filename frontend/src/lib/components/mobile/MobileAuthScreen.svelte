<script lang="ts">
	import PasswordStrengthIndicator from '$lib/components/PasswordStrengthIndicator.svelte';
	import { authService } from '$lib/services/auth.service';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import {
		ApiError,
		isTwoFactorChallenge,
		type LoginRequest,
		type RegisterRequest
	} from '$lib/types';

	type AuthMode = 'login' | 'register';

	let activeMode = $state<AuthMode>('login');
	let username = $state('');
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
				: username.trim().length === 0 || password.length === 0)
	);
	const passwordScore = $derived(computeScore(password));
	const showPasswordHint = $derived(
		activeMode === 'register' && passwordTouched && password.length > 0 && password.length < 8
	);

	function switchMode(mode: AuthMode): void {
		if (mode === activeMode) return;
		activeMode = mode;
		username = '';
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
			if (challengeToken && err.isUnauthorized) {
				return 'Invalid authentication code.';
			}

			if (activeMode === 'login' && err.isUnauthorized) {
				return 'Invalid username or password.';
			}

			if (activeMode === 'register' && err.message?.toLowerCase().includes('already taken')) {
				return 'That username is already taken. Try another.';
			}

			return err.message || 'Something went wrong. Please try again.';
		}

		if (err instanceof Error) return err.message;
		return 'Something went wrong. Please try again.';
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
			bumpError('Please choose a stronger password.');
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
				const req: RegisterRequest = { username, password };
				const user = await authService.register(req);
				await authStore.login(user);
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
	<div class="auth-shell">
		<div class="auth-card" aria-label={activeMode === 'login' ? 'Sign in form' : 'Create account form'}>
			<div class="card-header">
				<span class="logo-text">critiqal</span>
				<p class="subtitle">
					{#if challengeToken}
						Enter the 6-digit code from your authenticator app
					{:else}
						{activeMode === 'login' ? 'Sign in to your account' : 'Create your account'}
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
						<label for="mobile-auth-code" class="field-label">Authentication code</label>
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
							placeholder="123456"
							oninput={() => {
								if (error) error = '';
							}}
						/>
					</div>
				{:else}
					<div class="field">
						<label for="mobile-auth-username" class="field-label">Username</label>
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
							placeholder="your_username"
							oninput={() => {
								if (error) error = '';
							}}
						/>
					</div>

					<div class="field">
						<label for="mobile-auth-password" class="field-label">Password</label>
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
									<p class="field-hint">Password must be at least 8 characters.</p>
								{/if}
							</div>
						{/if}
					</div>
				{/if}

				<button type="submit" class="submit-btn" disabled={isSubmitDisabled}>
					{#if loading}
						{#if challengeToken}
							Verifying...
						{:else}
							{activeMode === 'login' ? 'Signing in...' : 'Creating account...'}
						{/if}
					{:else}
						{#if challengeToken}
							Verify code
						{:else}
							{activeMode === 'login' ? 'Sign in' : 'Create account'}
						{/if}
					{/if}
				</button>

				{#if challengeToken}
					<button type="button" class="ghost-btn" disabled={loading} onclick={resetChallenge}>
						Use another account
					</button>
				{/if}
			</form>

			{#if !challengeToken}
				<p class="switch-link">
					{#if activeMode === 'login'}
						Don't have an account?
						<button type="button" class="switch-btn" onclick={() => switchMode('register')}>
							Create one
						</button>
					{:else}
						Already have an account?
						<button type="button" class="switch-btn" onclick={() => switchMode('login')}>
							Sign in
						</button>
					{/if}
				</p>
			{/if}
		</div>
	</div>
</div>

<style>
	.auth-page {
		height: 100%;
		background: var(--color-bg);
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
	}

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
		background: var(--color-surface);
		border: 1px solid var(--surface-tint-soft);
		border-radius: 1rem;
		padding: 2.4rem 1.55rem 1.7rem;
		display: flex;
		flex-direction: column;
		gap: 1.35rem;
		box-shadow: 0 18px 40px rgba(0, 0, 0, 0.2);
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
		color: var(--text-faint);
		text-align: center;
	}

	.error-box {
		background: rgba(224, 82, 82, 0.08);
		border: 1px solid rgba(224, 82, 82, 0.2);
		border-radius: 0.75rem;
		padding: 0.75rem 0.9rem;
		font-size: 0.875rem;
		line-height: 1.4;
		color: #f27272;
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
		color: var(--text-faint);
	}

	.field-input {
		width: 100%;
		height: 3.2rem;
		background: var(--color-surface);
		border: 1px solid var(--surface-tint-subtle);
		border-radius: 0.72rem;
		padding: 0 0.9rem;
		font-size: 1rem;
		color: var(--color-text-primary);
		font-family: inherit;
		transition:
			border-color 0.16s ease,
			box-shadow 0.16s ease,
			background-color 0.16s ease;
		outline: none;
		box-sizing: border-box;
	}

	.field-input::placeholder {
		color: var(--surface-tint-strong);
	}

	.field-input:focus {
		border-color: var(--surface-tint-strong);
		box-shadow: 0 0 0 3px var(--surface-tint-subtle);
		background: var(--color-surface-raised);
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
		background: var(--color-text-primary);
		color: var(--color-bg);
		font-size: 1rem;
		font-weight: 700;
		font-family: inherit;
		cursor: pointer;
		transition:
			transform 0.12s ease,
			opacity 0.15s ease;
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
		border: 1px solid var(--surface-tint-medium);
		background: transparent;
		color: var(--text-strong);
		font-size: 0.94rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition:
			background-color 0.15s ease,
			border-color 0.15s ease,
			opacity 0.15s ease;
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
		color: var(--text-faint);
	}

	.switch-btn {
		border: none;
		background: none;
		padding: 0;
		margin-left: 0.25rem;
		font: inherit;
		font-weight: 700;
		color: var(--text-strong);
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
