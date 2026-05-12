<script lang="ts">
	import { goto } from '$app/navigation';
	import { fly } from 'svelte/transition';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { authService } from '$lib/services/auth.service';
	import { recoveryService } from '$lib/services/recovery.service';
	import { emailVerificationService } from '$lib/services/email-verification.service';
	import { ApiError, isTwoFactorChallenge } from '$lib/types';

	type AuthMode = 'credentials' | 'totp' | 'recovery';
	type Step = 'auth' | 'onboarding';

	let step = $state<Step>('auth');
	let mode = $state<AuthMode>('credentials');

	// ── Auth ─────────────────────────────────────────────────────────────────
	let username = $state('');
	let password = $state('');
	let challengeToken = $state<string | null>(null);
	let totpCode = $state('');
	let recoveryCode = $state('');
	let isSubmitting = $state(false);
	let error = $state('');
	let shakeKey = $state(0);

	function mapError(err: unknown): string {
		if (err instanceof ApiError) {
			if (err.isUnauthorized) {
				if (mode === 'totp') return 'Invalid authentication code.';
				if (mode === 'recovery') return 'Invalid recovery code.';
				return 'Invalid username or password.';
			}
			return err.message || 'Something went wrong.';
		}
		if (err instanceof Error) return err.message;
		return 'Something went wrong.';
	}

	function resetChallenge(): void {
		challengeToken = null; totpCode = ''; recoveryCode = '';
		mode = 'credentials'; error = '';
	}

	async function handleSubmit(): Promise<void> {
		isSubmitting = true;
		error = '';
		try {
			if (mode === 'totp' && challengeToken) {
				const user = await authService.verifyTwoFactor({ challengeToken, code: totpCode });
				await authStore.login(user);
				await goto('/');
				return;
			}

			if (mode === 'recovery') {
				const user = await recoveryService.useRecoveryCode({ username, recoveryCode });
				await authStore.login(user);
				await goto('/');
				return;
			}

			const result = await authService.login({ username, password });
			if (isTwoFactorChallenge(result)) {
				challengeToken = result.challengeToken;
				totpCode = '';
				mode = 'totp';
				return;
			}

			await authStore.login(result);
			if (result.emailVerified) {
				goto('/');
			} else {
				step = 'onboarding';
			}
		} catch (err: unknown) {
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
		if (!emailInput.trim()) return;
		emailSubmitting = true;
		try {
			await emailVerificationService.setEmail({ email: emailInput });
		} catch {
			// non-blocking
		} finally {
			emailSubmitting = false;
			goto('/');
		}
	}

	function skip(): void { goto('/'); }
</script>

<svelte:head>
	<title>Sign in — Critiqal</title>
	<meta name="description" content="Sign in to your Critiqal account" />
</svelte:head>

<div class="page">
	{#if step === 'auth'}
		<div class="card" aria-label="Sign in" in:fly={{ y: 10, duration: 220 }}>
			<div class="card-header">
				<span class="logo">critiqal</span>
				<p class="subtitle">
					{#if mode === 'totp'}
						Enter the 6-digit code from your authenticator app
					{:else if mode === 'recovery'}
						Enter a recovery code to access your account
					{:else}
						Sign in to your account
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
						<label for="totp-code" class="field-label">Authentication code</label>
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
							placeholder="123456"
						/>
					</div>
				{:else if mode === 'recovery'}
					<div class="field">
						<label for="rec-username" class="field-label">Username</label>
						<input id="rec-username" type="text" class="field-input" bind:value={username}
							autocomplete="username" required disabled={isSubmitting} placeholder="your_username" />
					</div>
					<div class="field">
						<label for="rec-code" class="field-label">Recovery code</label>
						<input id="rec-code" type="text" class="field-input" bind:value={recoveryCode}
							autocomplete="off" required disabled={isSubmitting} placeholder="xxxxxxxx-xxxx" />
					</div>
				{:else}
					<div class="field">
						<label for="username" class="field-label">Username</label>
						<input id="username" type="text" class="field-input" bind:value={username}
							autocomplete="username" required disabled={isSubmitting} placeholder="your_username" />
					</div>
					<div class="field">
						<label for="password" class="field-label">Password</label>
						<input id="password" type="password" class="field-input" bind:value={password}
							autocomplete="current-password" required disabled={isSubmitting} placeholder="••••••••" />
					</div>
					<a href="/forgot-password" class="forgot-link">Forgot password?</a>
				{/if}

				<button
					type="submit"
					class="submit-btn"
					disabled={isSubmitting || (
						mode === 'totp' ? totpCode.trim().length !== 6 :
						mode === 'recovery' ? !username.trim() || !recoveryCode.trim() :
						!username.trim() || !password
					)}
				>
					{isSubmitting ? 'Verifying…' : mode === 'credentials' ? 'Sign in' : 'Verify'}
				</button>

				{#if mode === 'totp'}
					<button type="button" class="ghost-btn" disabled={isSubmitting}
						onclick={() => { mode = 'recovery'; totpCode = ''; error = ''; }}>
						Use recovery code instead
					</button>
					<button type="button" class="ghost-btn" disabled={isSubmitting} onclick={resetChallenge}>
						Use another account
					</button>
				{:else if mode === 'recovery'}
					<button type="button" class="ghost-btn" disabled={isSubmitting}
						onclick={() => { mode = 'totp'; recoveryCode = ''; error = ''; }}>
						Use authenticator app instead
					</button>
					<button type="button" class="ghost-btn" disabled={isSubmitting} onclick={resetChallenge}>
						Back to sign in
					</button>
				{/if}
			</form>

			{#if mode === 'credentials'}
				<p class="switch-link">Don't have an account? <a href="/register">Create one</a></p>
			{/if}
		</div>

	{:else}
		<div class="card" aria-label="Account setup" in:fly={{ y: 10, duration: 220 }}>
			<div class="card-header">
				<div class="onboarding-icon" aria-hidden="true">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="20" height="20">
						<path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
						<polyline points="22,6 12,13 2,6"/>
					</svg>
				</div>
				<p class="onboarding-title">Add a recovery email</p>
				<p class="onboarding-sub">
					Protect your account with an email address for password recovery.
				</p>
			</div>

			<form class="form" onsubmit={(e) => { e.preventDefault(); handleAddEmail(); }}>
				<div class="field">
					<label for="email" class="field-label">Email address</label>
					<input
						id="email"
						type="email"
						class="field-input"
						bind:value={emailInput}
						autocomplete="email"
						disabled={emailSubmitting}
						placeholder="you@example.com"
					/>
				</div>

				<button type="submit" class="submit-btn" disabled={emailSubmitting || !emailInput.trim()}>
					{emailSubmitting ? 'Saving…' : 'Add email'}
				</button>
			</form>

			<button type="button" class="skip-btn" onclick={skip} disabled={emailSubmitting}>
				Skip for now
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
	}

	.card {
		width: 100%;
		max-width: 22rem;
		background: var(--color-surface);
		border: 1px solid var(--color-border);
		border-radius: 0.75rem;
		padding: 2.5rem;
		display: flex;
		flex-direction: column;
		gap: 1.5rem;
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
		border: 1px solid var(--color-border);
		border-radius: 0.5rem;
		padding: 0.625rem 0.75rem;
		font-size: 0.9375rem;
		color: var(--color-text-primary);
		font-family: inherit;
		transition: border-color 0.15s ease, box-shadow 0.15s ease;
		outline: none;
	}

	.field-input::placeholder { color: var(--color-text-muted); opacity: 0.5; }
	.field-input:focus { border-color: rgba(240, 240, 240, 0.25); box-shadow: 0 0 0 3px rgba(240, 240, 240, 0.04); }
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
		padding: 0.625rem 1rem;
		border-radius: 0.5rem;
		border: none;
		background: var(--color-text-primary);
		color: var(--color-bg);
		font-size: 0.9375rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition: opacity 0.15s ease, transform 0.1s ease;
		margin-top: 0.25rem;
	}

	.submit-btn:hover:not(:disabled) { opacity: 0.85; }
	.submit-btn:active:not(:disabled) { transform: scale(0.97); }
	.submit-btn:disabled { opacity: 0.4; cursor: not-allowed; }

	.ghost-btn {
		width: 100%;
		padding: 0.625rem 1rem;
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

	.ghost-btn:hover:not(:disabled) { background: var(--color-surface-raised); }
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
