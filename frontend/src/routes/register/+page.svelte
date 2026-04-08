<script lang="ts">
	import { goto } from '$app/navigation';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { authService } from '$lib/services/auth.service';
	import { ApiError } from '$lib/types';
	import PasswordStrengthIndicator from '$lib/components/PasswordStrengthIndicator.svelte';

	let username = $state('');
	let password = $state('');
	let isSubmitting = $state(false);
	let error = $state('');
	let passwordTouched = $state(false);
	let shakeKey = $state(0);

	const hasError = $derived(error.length > 0);
	const passwordScore = $derived(computeScore(password));
	const showPasswordHint = $derived(
		passwordTouched && password.length > 0 && password.length < 8
	);

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
			if (err.message?.toLowerCase().includes('already taken')) {
				return 'That username is already taken. Try another.';
			}
			return err.message || 'Something went wrong. Please try again.';
		}
		if (err instanceof Error) return err.message;
		return 'Something went wrong. Please try again.';
	}

	async function handleSubmit(): Promise<void> {
		if (passwordScore < 2) {
			error = 'Please choose a stronger password.';
			shakeKey++;
			return;
		}

		isSubmitting = true;
		error = '';

		try {
			const response = await authService.register({ username, password });
			authStore.login(response.user, response.token);
			await goto('/');
		} catch (err: unknown) {
			error = mapError(err);
			shakeKey++;
		} finally {
			isSubmitting = false;
		}
	}

	function handlePasswordInput(): void {
		passwordTouched = true;
	}
</script>

<svelte:head>
	<title>Create account — Critiqal</title>
	<meta name="description" content="Create your Critiqal account" />
</svelte:head>

<div class="page">
	<div class="card" aria-label="Create account form">
		<div class="card-header">
			<span class="logo-text">critiqal</span>
			<p class="subtitle">Create your account</p>
		</div>

		{#if hasError}
			{#key shakeKey}
				<div class="error-box shake" role="alert">
					{error}
				</div>
			{/key}
		{/if}

		<form class="form" onsubmit={(e) => { e.preventDefault(); handleSubmit(); }}>
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
					oninput={handlePasswordInput}
				/>
				<PasswordStrengthIndicator {password} />
				{#if showPasswordHint}
					<p class="field-hint">Password must be at least 8 characters</p>
				{/if}
			</div>

			<button type="submit" class="submit-btn" disabled={isSubmitting}>
				{isSubmitting ? 'Creating account...' : 'Create account'}
			</button>
		</form>

		<p class="switch-link">
			Already have an account?
			<a href="/login">Sign in</a>
		</p>
	</div>
</div>

<style>
	.page {
		min-height: 100vh;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 1.5rem;
		background-color: var(--color-bg);
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
		animation: cardFadeIn 0.25s ease-out;
	}

	.card-header {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
	}

	.logo-text {
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

	.error-box {
		background: rgba(224, 82, 82, 0.08);
		border: 1px solid rgba(224, 82, 82, 0.2);
		border-radius: 0.5rem;
		padding: 0.75rem 1rem;
		font-size: 0.875rem;
		color: var(--color-accent);
		line-height: 1.4;
	}

	.form {
		display: flex;
		flex-direction: column;
		gap: 1rem;
	}

	.field {
		display: flex;
		flex-direction: column;
		gap: 0.375rem;
	}

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
		transition:
			border-color 0.15s ease,
			box-shadow 0.15s ease;
		outline: none;
	}

	.field-input::placeholder {
		color: var(--color-text-muted);
		opacity: 0.5;
	}

	.field-input:focus {
		border-color: var(--color-text-muted);
		box-shadow: 0 0 0 3px rgba(240, 240, 240, 0.06);
	}

	.field-input:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}

	.field-hint {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		margin: 0;
	}

	.submit-btn {
		width: 100%;
		padding: 0.625rem 1rem;
		border-radius: 0.5rem;
		border: none;
		background-color: var(--color-text-primary);
		color: var(--color-bg);
		font-size: 0.9375rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition:
			background-color 0.15s ease,
			transform 0.1s ease;
		margin-top: 0.25rem;
	}

	.submit-btn:hover:not(:disabled) {
		background-color: var(--color-text-muted);
	}

	.submit-btn:active:not(:disabled) {
		transform: scale(0.97);
	}

	.submit-btn:disabled {
		opacity: 0.6;
		cursor: not-allowed;
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
		transition: opacity 0.15s ease;
	}

	.switch-link a:hover {
		opacity: 0.75;
	}

	@keyframes cardFadeIn {
		from {
			opacity: 0;
			transform: translateY(0.5rem);
		}
		to {
			opacity: 1;
			transform: translateY(0);
		}
	}

	.shake {
		animation: errorShake 0.3s ease-out;
	}

	@keyframes errorShake {
		0% { transform: translateX(0); }
		20% { transform: translateX(-4px); }
		40% { transform: translateX(4px); }
		60% { transform: translateX(-4px); }
		80% { transform: translateX(2px); }
		100% { transform: translateX(0); }
	}
</style>
