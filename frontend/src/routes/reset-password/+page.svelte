<script lang="ts">
	import { page } from '$app/stores';
	import { ApiError } from '$lib/types';
	import { recoveryService } from '$lib/services/recovery.service';
	import PasswordStrengthIndicator from '$lib/components/PasswordStrengthIndicator.svelte';

	const token = $page.url.searchParams.get('token') ?? '';

	let newPassword = $state('');
	let passwordTouched = $state(false);
	let isSubmitting = $state(false);
	let done = $state(false);
	let error = $state('');
	let shakeKey = $state(0);

	const hasError = $derived(error.length > 0);
	const hasToken = $derived(token.length > 0);
	const passwordScore = $derived(computeScore(newPassword));
	const showPasswordHint = $derived(
		passwordTouched && newPassword.length > 0 && newPassword.length < 8
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
			if (err.status === 400 || err.status === 404) {
				return 'This reset link is invalid or has expired.';
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
			await recoveryService.resetPassword({ token, newPassword });
			done = true;
		} catch (err: unknown) {
			error = mapError(err);
			shakeKey++;
		} finally {
			isSubmitting = false;
		}
	}
</script>

<svelte:head>
	<title>Set new password — Critiqal</title>
	<meta name="description" content="Set a new password for your Critiqal account" />
</svelte:head>

<div class="page">
	<div class="card" aria-label="Reset password form">
		<div class="card-header">
			<span class="logo-text">critiqal</span>
			<p class="subtitle">
				{#if !hasToken}
					Invalid reset link
				{:else if done}
					Password updated
				{:else}
					Set a new password
				{/if}
			</p>
		</div>

		{#if !hasToken}
			<div class="state-icon state-icon-error" aria-hidden="true">
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="20" height="20">
					<line x1="18" y1="6" x2="6" y2="18" />
					<line x1="6" y1="6" x2="18" y2="18" />
				</svg>
			</div>
			<p class="state-text">Invalid or expired reset link.</p>
			<a href="/forgot-password" class="submit-btn submit-btn-link">Request a new link</a>
		{:else if done}
			<div class="state-icon state-icon-success" aria-hidden="true">
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="22" height="22">
					<polyline points="20 6 9 17 4 12" />
				</svg>
			</div>
			<p class="state-text">Password updated. You can now sign in.</p>
			<a href="/login" class="submit-btn submit-btn-link">Sign in</a>
		{:else}
			{#if hasError}
				{#key shakeKey}
					<div class="error-box shake" role="alert">
						{error}
					</div>
				{/key}
			{/if}

			<form class="form" onsubmit={(e) => { e.preventDefault(); handleSubmit(); }}>
				<div class="field">
					<label for="new-password" class="field-label">New password</label>
					<input
						id="new-password"
						type="password"
						class="field-input"
						bind:value={newPassword}
						autocomplete="new-password"
						required
						minlength={8}
						disabled={isSubmitting}
						placeholder="••••••••"
						oninput={() => { passwordTouched = true; }}
					/>
					<PasswordStrengthIndicator password={newPassword} />
					{#if showPasswordHint}
						<p class="field-hint">Password must be at least 8 characters</p>
					{/if}
				</div>

				<button
					type="submit"
					class="submit-btn"
					disabled={isSubmitting || newPassword.length === 0}
				>
					{isSubmitting ? 'Updating...' : 'Update password'}
				</button>
			</form>
		{/if}
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
		text-align: center;
		text-decoration: none;
		display: block;
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

	.submit-btn-link {
		display: block;
	}

	.state-icon {
		width: 3rem;
		height: 3rem;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		align-self: center;
	}

	.state-icon-success {
		background: rgba(16, 185, 129, 0.12);
		color: #10b981;
	}

	.state-icon-error {
		background: rgba(224, 82, 82, 0.1);
		color: var(--color-accent);
	}

	.state-text {
		font-size: 0.875rem;
		color: var(--color-text-muted);
		text-align: center;
		line-height: 1.5;
		margin: 0;
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
