<script lang="ts">
	import { goto } from '$app/navigation';
	import { fly } from 'svelte/transition';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { authService } from '$lib/services/auth.service';
	import { emailVerificationService } from '$lib/services/email-verification.service';
	import { ApiError } from '$lib/types';
	import PasswordStrengthIndicator from '$lib/components/PasswordStrengthIndicator.svelte';

	type Step = 'register' | 'onboarding';

	let step = $state<Step>('register');

	// ── Register ─────────────────────────────────────────────────────────────
	let username = $state('');
	let password = $state('');
	let isSubmitting = $state(false);
	let error = $state('');
	let passwordTouched = $state(false);
	let shakeKey = $state(0);

	const passwordScore = $derived(computeScore(password));
	const showPasswordHint = $derived(passwordTouched && password.length > 0 && password.length < 8);

	function computeScore(pwd: string): number {
		if (pwd.length === 0) return 0;
		let score = 0;
		if (pwd.length >= 8) score++;
		if (/[0-9]/.test(pwd)) score++;
		if (/[A-Z]/.test(pwd)) score++;
		if (/[^A-Za-z0-9]/.test(pwd)) score++;
		return score;
	}

	async function handleRegister(): Promise<void> {
		if (passwordScore < 2) {
			error = 'Please choose a stronger password.';
			shakeKey++;
			return;
		}
		isSubmitting = true;
		error = '';
		try {
			const user = await authService.register({ username, password });
			await authStore.login(user);
			if (user.emailVerified) {
				goto('/');
			} else {
				step = 'onboarding';
			}
		} catch (err: unknown) {
			if (err instanceof ApiError) {
				error = err.message?.toLowerCase().includes('already taken')
					? 'That username is already taken.'
					: err.message || 'Something went wrong.';
			} else {
				error = 'Something went wrong.';
			}
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
			// non-blocking — user can always add email later in settings
		} finally {
			emailSubmitting = false;
			goto('/');
		}
	}

	function skip(): void {
		goto('/');
	}
</script>

<svelte:head>
	<title>Create account — Critiqal</title>
	<meta name="description" content="Create your Critiqal account" />
</svelte:head>

<div class="page">
	{#if step === 'register'}
		<div class="card" aria-label="Create account" in:fly={{ y: 10, duration: 220 }}>
			<div class="card-header">
				<span class="logo">critiqal</span>
				<p class="subtitle">Create your account</p>
			</div>

			{#if error}
				{#key shakeKey}
					<div class="error shake" role="alert">{error}</div>
				{/key}
			{/if}

			<form class="form" onsubmit={(e) => { e.preventDefault(); handleRegister(); }}>
				<div class="field">
					<label for="username" class="field-label">Username</label>
					<input
						id="username"
						type="text"
						class="field-input"
						bind:value={username}
						autocomplete="username"
						required
						minlength={3}
						maxlength={30}
						disabled={isSubmitting}
						placeholder="your_username"
					/>
				</div>

				<div class="field">
					<label for="password" class="field-label">Password</label>
					<input
						id="password"
						type="password"
						class="field-input"
						bind:value={password}
						autocomplete="new-password"
						required
						minlength={8}
						disabled={isSubmitting}
						placeholder="••••••••"
						oninput={() => { passwordTouched = true; }}
					/>
					<PasswordStrengthIndicator {password} />
					{#if showPasswordHint}
						<p class="field-hint">At least 8 characters</p>
					{/if}
				</div>

				<button type="submit" class="submit-btn" disabled={isSubmitting}>
					{isSubmitting ? 'Creating account…' : 'Create account'}
				</button>
			</form>

			<p class="switch-link">Already have an account? <a href="/login">Sign in</a></p>
		</div>

	{:else}
		<div class="card" aria-label="Account setup" in:fly={{ y: 10, duration: 220 }}>
			<div class="card-header">
				<div class="onboarding-icon" aria-hidden="true">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="20" height="20">
						<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
						<circle cx="12" cy="7" r="4"/>
					</svg>
				</div>
				<p class="onboarding-title">Add a recovery email</p>
				<p class="onboarding-sub">
					If you ever lose access to your account, your email is the only way to recover it.
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

				<button type="submit" class="submit-btn" disabled={emailSubmitting || emailInput.trim().length === 0}>
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
	}

	/* ── Onboarding header ──────────────────────────────────────────────── */

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

	/* ── Form ───────────────────────────────────────────────────────────── */

	.form { display: flex; flex-direction: column; gap: 1rem; }

	.field { display: flex; flex-direction: column; gap: 0.375rem; }

	.field-label {
		font-size: 0.8125rem;
		font-weight: 500;
		color: var(--color-text-muted);
	}

	.field-hint {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		margin: 0;
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

	/* ── Feedback ───────────────────────────────────────────────────────── */

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

	/* ── Animations ─────────────────────────────────────────────────────── */

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
