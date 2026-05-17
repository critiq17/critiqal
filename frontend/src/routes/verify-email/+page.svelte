<script lang="ts">
	import { onMount } from 'svelte';
	import { page } from '$app/stores';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { emailVerificationService } from '$lib/services/email-verification.service';
	import { ApiError } from '$lib/types';

	type VerifyState = 'loading' | 'success' | 'error';

	const token = $page.url.searchParams.get('token') ?? '';

	let verifyState = $state<VerifyState>('loading');
	let errorMessage = $state('This verification link is invalid or has expired.');

	const backHref = $derived(authStore.isAuthenticated ? '/settings' : '/login');

	onMount(async () => {
		if (!token) {
			verifyState = 'error';
			return;
		}

		try {
			await emailVerificationService.verifyEmail({ token });
			verifyState = 'success';
		} catch (err: unknown) {
			if (err instanceof ApiError) {
				errorMessage = err.message || 'This verification link is invalid or has expired.';
			}
			verifyState = 'error';
		}
	});
</script>

<svelte:head>
	<title>Verify email — Critiqal</title>
	<meta name="description" content="Verify your email address" />
</svelte:head>

<div class="page">
	<div class="card" aria-live="polite">
		<div class="card-header">
			<span class="logo-text">critiqal</span>
		</div>

		{#if verifyState === 'loading'}
			<div class="loading-state">
				<div class="spinner" aria-label="Verifying…"></div>
				<p class="state-text">Verifying your email…</p>
			</div>
		{:else if verifyState === 'success'}
			<div class="result-state">
				<div class="state-icon state-icon-success" aria-hidden="true">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="22" height="22">
						<polyline points="20 6 9 17 4 12" />
					</svg>
				</div>
				<p class="state-title">Email verified!</p>
				<p class="state-text">Your email address has been confirmed successfully.</p>
				<a href={backHref} class="action-btn">
					{authStore.isAuthenticated ? 'Go to settings' : 'Sign in'}
				</a>
			</div>
		{:else}
			<div class="result-state">
				<div class="state-icon state-icon-error" aria-hidden="true">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="20" height="20">
						<line x1="18" y1="6" x2="6" y2="18" />
						<line x1="6" y1="6" x2="18" y2="18" />
					</svg>
				</div>
				<p class="state-title">Verification failed</p>
				<p class="state-text">{errorMessage}</p>
				<a href={backHref} class="action-btn">
					{authStore.isAuthenticated ? 'Go to settings' : 'Sign in'}
				</a>
			</div>
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
	}

	.logo-text {
		font-size: 1.25rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.03em;
	}

	.loading-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1rem;
	}

	.spinner {
		width: 2rem;
		height: 2rem;
		border: 2px solid var(--color-border);
		border-top-color: var(--color-text-primary);
		border-radius: 50%;
		animation: spin 0.7s linear infinite;
	}

	.result-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.75rem;
		text-align: center;
	}

	.state-icon {
		width: 3rem;
		height: 3rem;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		margin-bottom: 0.25rem;
	}

	.state-icon-success {
		background: rgba(16, 185, 129, 0.12);
		color: #10b981;
	}

	.state-icon-error {
		background: rgba(224, 82, 82, 0.1);
		color: var(--color-accent);
	}

	.state-title {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.01em;
		margin: 0;
	}

	.state-text {
		font-size: 0.875rem;
		color: var(--color-text-muted);
		line-height: 1.5;
		margin: 0;
	}

	.action-btn {
		margin-top: 0.5rem;
		width: 100%;
		padding: 0.625rem 1rem;
		border-radius: 0.5rem;
		background-color: var(--color-text-primary);
		color: var(--color-bg);
		font-size: 0.9375rem;
		font-weight: 600;
		font-family: inherit;
		text-decoration: none;
		text-align: center;
		display: block;
		transition:
			background-color 0.15s ease,
			transform 0.1s ease;
	}

	.action-btn:hover {
		background-color: var(--color-text-muted);
	}

	.action-btn:active {
		transform: scale(0.97);
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

	@keyframes spin {
		to { transform: rotate(360deg); }
	}
</style>
