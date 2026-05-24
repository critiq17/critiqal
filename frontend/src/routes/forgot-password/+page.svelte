<script lang="ts">
	import { ApiError } from '$lib/types';
	import { recoveryService } from '$lib/services/recovery.service';
	import { t } from '$lib/i18n';

	let email = $state('');
	let isSubmitting = $state(false);
	let sent = $state(false);
	let error = $state('');
	let shakeKey = $state(0);

	const hasError = $derived(error.length > 0);

	function mapError(err: unknown): string {
		if (err instanceof ApiError) return err.message || t('common.somethingWentWrong');
		if (err instanceof Error) return err.message;
		return t('common.somethingWentWrong');
	}

	async function handleSubmit(): Promise<void> {
		isSubmitting = true;
		error = '';

		try {
			await recoveryService.requestReset({ email });
			sent = true;
		} catch (err: unknown) {
			error = mapError(err);
			shakeKey++;
		} finally {
			isSubmitting = false;
		}
	}
</script>

<svelte:head>
	<title>{t('auth.forgot.title')} — Critiqal</title>
	<meta name="description" content="Reset your Critiqal account password" />
</svelte:head>

<div class="page">
	<div class="card" aria-label={t('auth.forgot.title')}>
		<div class="card-header">
			<span class="logo-text">critiqal</span>
			<p class="subtitle">{t('auth.forgot.subtitle')}</p>
		</div>

		{#if sent}
			<div class="success-state">
				<div class="icon-success" aria-hidden="true">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="22" height="22">
						<polyline points="20 6 9 17 4 12" />
					</svg>
				</div>
				<p class="success-text">{t('auth.forgot.sent')}</p>
			</div>
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
					<label for="email" class="field-label">{t('auth.forgot.email')}</label>
					<input
						id="email"
						type="email"
						class="field-input"
						bind:value={email}
						autocomplete="email"
						required
						disabled={isSubmitting}
						placeholder="you@example.com"
					/>
				</div>

				<button
					type="submit"
					class="submit-btn"
					disabled={isSubmitting || email.trim().length === 0}
				>
					{isSubmitting ? t('auth.forgot.submitting') : t('auth.forgot.submit')}
				</button>
			</form>
		{/if}

		<p class="switch-link">
			<a href="/login">{t('auth.forgot.back')}</a>
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

	.success-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1rem;
		text-align: center;
	}

	.icon-success {
		width: 3rem;
		height: 3rem;
		border-radius: 50%;
		background: rgba(16, 185, 129, 0.12);
		color: #10b981;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.success-text {
		font-size: 0.875rem;
		color: var(--color-text-muted);
		line-height: 1.5;
		margin: 0;
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
