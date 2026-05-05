<script lang="ts">
	import PasswordStrengthIndicator from '$lib/components/PasswordStrengthIndicator.svelte';
	import { authService } from '$lib/services/auth.service';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import { ApiError, type LoginRequest, type RegisterRequest } from '$lib/types';

	type AuthMode = 'login' | 'register';

	let activeMode = $state<AuthMode>('login');
	let username = $state('');
	let password = $state('');
	let error = $state('');
	let loading = $state(false);
	let passwordTouched = $state(false);
	let shakeKey = $state(0);

	const hasError = $derived(error.length > 0);
	const isSubmitDisabled = $derived(
		loading || username.trim().length === 0 || password.length === 0
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

	async function handleSubmit(): Promise<void> {
		if (loading) return;

		if (activeMode === 'register' && passwordScore < 2) {
			bumpError('Please choose a stronger password.');
			return;
		}

		error = '';
		loading = true;

		try {
			if (activeMode === 'login') {
				const req: LoginRequest = { username, password };
				const user = await authService.login(req);
				await authStore.login(user);
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
					{activeMode === 'login' ? 'Sign in to your account' : 'Create your account'}
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

				<button type="submit" class="submit-btn" disabled={isSubmitDisabled}>
					{#if loading}
						{activeMode === 'login' ? 'Signing in...' : 'Creating account...'}
					{:else}
						{activeMode === 'login' ? 'Sign in' : 'Create account'}
					{/if}
				</button>
			</form>

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
		</div>
	</div>
</div>

<style>
	.auth-page {
		height: 100%;
		background: #090909;
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
		background: #121212;
		border: 1px solid rgba(255, 255, 255, 0.07);
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
		color: #f4f4f4;
	}

	.subtitle {
		margin: 0;
		font-size: 0.95rem;
		color: rgba(255, 255, 255, 0.24);
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
		color: rgba(255, 255, 255, 0.34);
	}

	.field-input {
		width: 100%;
		height: 3.2rem;
		background: #1b1b1b;
		border: 1px solid rgba(255, 255, 255, 0.05);
		border-radius: 0.72rem;
		padding: 0 0.9rem;
		font-size: 1rem;
		color: #f2f2f2;
		font-family: inherit;
		transition:
			border-color 0.16s ease,
			box-shadow 0.16s ease,
			background-color 0.16s ease;
		outline: none;
		box-sizing: border-box;
	}

	.field-input::placeholder {
		color: rgba(255, 255, 255, 0.16);
	}

	.field-input:focus {
		border-color: rgba(255, 255, 255, 0.12);
		box-shadow: 0 0 0 3px rgba(255, 255, 255, 0.035);
		background: #1d1d1d;
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
		color: rgba(255, 255, 255, 0.36);
	}

	.submit-btn {
		width: 100%;
		height: 3.2rem;
		border-radius: 0.72rem;
		border: none;
		background: #f0f0f0;
		color: #222;
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

	.switch-link {
		margin: 0;
		text-align: center;
		font-size: 0.98rem;
		line-height: 1.45;
		color: rgba(255, 255, 255, 0.26);
	}

	.switch-btn {
		border: none;
		background: none;
		padding: 0;
		margin-left: 0.25rem;
		font: inherit;
		font-weight: 700;
		color: rgba(255, 255, 255, 0.82);
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
