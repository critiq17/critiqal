<script lang="ts">
	import { goto } from '$app/navigation';
	import { onMount, onDestroy } from 'svelte';
	import { fly, fade } from 'svelte/transition';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { emailVerificationService } from '$lib/services/email-verification.service';
	import { verifyEmailStore } from '$lib/stores/verify-email.store.svelte';
	import { ApiError } from '$lib/types';
	import StarfieldBackdrop from '$lib/ui/StarfieldBackdrop.svelte';
	import { t } from '$lib/i18n';

	const RESEND_COOLDOWN_S = 30;

	let code = $state('');
	let submitting = $state(false);
	let resending = $state(false);
	let error = $state('');
	let success = $state(false);
	let info = $state('');
	let cooldown = $state(0);
	let shakeKey = $state(0);

	let cooldownTimer: ReturnType<typeof setInterval> | null = null;
	let inputEl: HTMLInputElement | null = $state(null);

	const pending = $derived(verifyEmailStore.current);
	const canSubmit = $derived(code.length === 6 && !submitting && !success);

	function startCooldown(): void {
		cooldown = RESEND_COOLDOWN_S;
		if (cooldownTimer) clearInterval(cooldownTimer);
		cooldownTimer = setInterval(() => {
			cooldown = Math.max(0, cooldown - 1);
			if (cooldown === 0 && cooldownTimer) {
				clearInterval(cooldownTimer);
				cooldownTimer = null;
			}
		}, 1000);
	}

	function handleCodeInput(value: string): void {
		const digits = value.replace(/\D/g, '').slice(0, 6);
		code = digits;
		if (error) error = '';
		if (digits.length === 6) void handleVerify();
	}

	async function handleVerify(): Promise<void> {
		if (!canSubmit) return;
		submitting = true;
		error = '';
		try {
			await emailVerificationService.verifyEmail({ code });
			success = true;
			verifyEmailStore.clear();
			await authStore.refresh();
			setTimeout(() => goto('/'), 900);
		} catch (err: unknown) {
			if (err instanceof ApiError) {
				if (err.message?.toLowerCase().includes('expired')) {
					error = t('auth.verifyEmail.expired');
				} else {
					error = t('auth.verifyEmail.invalidCode');
				}
			} else {
				error = t('common.somethingWentWrong');
			}
			shakeKey += 1;
			code = '';
		} finally {
			submitting = false;
		}
	}

	async function handleResend(): Promise<void> {
		if (resending || cooldown > 0) return;
		resending = true;
		error = '';
		try {
			await emailVerificationService.resend();
			verifyEmailStore.refresh();
			info = t('auth.verifyEmail.resent');
			startCooldown();
			setTimeout(() => { info = ''; }, 2500);
		} catch (err: unknown) {
			error = err instanceof ApiError ? err.message : t('common.somethingWentWrong');
		} finally {
			resending = false;
		}
	}

	function handleChangeEmail(): void {
		verifyEmailStore.clear();
		void authStore.logout();
		goto('/register');
	}

	function handleVisibility(): void {
		// User just returned to the tab — re-read sessionStorage in case the
		// state was written by another tab/instance, and don't reset form.
		if (document.visibilityState === 'visible') {
			verifyEmailStore.reload();
		}
	}

	onMount(() => {
		verifyEmailStore.reload();
		// Auto-start cooldown so the user can't instantly spam resend after register
		startCooldown();
		document.addEventListener('visibilitychange', handleVisibility);
		setTimeout(() => inputEl?.focus(), 50);
	});

	onDestroy(() => {
		document.removeEventListener('visibilitychange', handleVisibility);
		if (cooldownTimer) clearInterval(cooldownTimer);
	});
</script>

<svelte:head>
	<title>{t('auth.verifyEmail.title')} — Critiqal</title>
</svelte:head>

<div class="page">
	<StarfieldBackdrop />

	<div class="card" in:fly={{ y: 12, duration: 260 }}>
		{#if success}
			<div class="success" in:fade={{ duration: 240 }}>
				<div class="success-star" aria-hidden="true">
					<span class="halo"></span>
					<span class="ring"></span>
					<svg class="star" viewBox="0 0 24 24" width="40" height="40" fill="currentColor">
						<path d="M12 2l2.39 7.36H22l-6.18 4.49L18.21 22 12 17.27 5.79 22l2.39-8.15L2 9.36h7.61z"/>
					</svg>
				</div>
				<p class="success-title">{t('auth.verifyEmail.success')}</p>
			</div>
		{:else if !pending && !authStore.isAuthenticated}
			<div class="header">
				<span class="logo">critiqal</span>
				<p class="subtitle">{t('auth.verifyEmail.noPending')}</p>
			</div>
			<a class="btn primary" href="/login">{t('nav.signIn')}</a>
		{:else}
			<div class="header">
				<span class="logo">critiqal</span>
				<p class="title">{t('auth.verifyEmail.codeTitle')}</p>
				{#if pending}
					<p class="subtitle">
						{t('auth.verifyEmail.codeSubtitle')}
						<strong>{pending.email}</strong>
					</p>
				{/if}
			</div>

			{#if error}
				{#key shakeKey}
					<div class="error shake" role="alert">{error}</div>
				{/key}
			{/if}

			{#if info}
				<div class="info" in:fade={{ duration: 150 }}>{info}</div>
			{/if}

			<form onsubmit={(e) => { e.preventDefault(); void handleVerify(); }} class="form">
				<input
					bind:this={inputEl}
					class="code-input"
					inputmode="numeric"
					autocomplete="one-time-code"
					pattern="[0-9]*"
					maxlength="6"
					value={code}
					oninput={(e) => handleCodeInput((e.target as HTMLInputElement).value)}
					placeholder={t('auth.verifyEmail.codePlaceholder')}
					aria-label={t('auth.verifyEmail.codeTitle')}
					disabled={submitting}
				/>

				<button type="submit" class="btn primary" disabled={!canSubmit}>
					{submitting ? t('auth.verifyEmail.verifying') : t('auth.verifyEmail.verify')}
				</button>
			</form>

			<p class="hint">{t('auth.verifyEmail.checkSpam')}</p>

			<div class="actions">
				<button
					type="button"
					class="link-btn"
					onclick={handleResend}
					disabled={resending || cooldown > 0}
				>
					{#if resending}
						{t('auth.verifyEmail.resending')}
					{:else if cooldown > 0}
						{t('auth.verifyEmail.resendIn').replace('{seconds}', String(cooldown))}
					{:else}
						{t('auth.verifyEmail.resend')}
					{/if}
				</button>
				<button type="button" class="link-btn muted" onclick={handleChangeEmail}>
					{t('auth.verifyEmail.changeEmail')}
				</button>
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
		background: var(--color-bg);
		position: relative;
		overflow: hidden;
	}

	.card {
		position: relative;
		z-index: 1;
		width: 100%;
		max-width: 22rem;
		background: var(--color-surface);
		border: 1px solid var(--color-border);
		border-radius: 0.75rem;
		padding: 2.5rem;
		display: flex;
		flex-direction: column;
		gap: 1.25rem;
	}

	.header {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.4rem;
		text-align: center;
	}

	.logo {
		font-size: 1.125rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.03em;
		margin-bottom: 0.25rem;
	}

	.title {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text-primary);
		margin: 0;
	}

	.subtitle {
		font-size: 0.875rem;
		color: var(--color-text-muted);
		margin: 0;
		line-height: 1.45;
	}

	.subtitle strong {
		color: var(--color-text-secondary);
		font-weight: 600;
		word-break: break-all;
	}

	.form {
		display: flex;
		flex-direction: column;
		gap: 1rem;
	}

	.code-input {
		width: 100%;
		background: var(--color-surface-raised);
		border: 1px solid var(--color-border);
		border-radius: 0.625rem;
		padding: 1rem;
		font-size: 1.75rem;
		font-weight: 600;
		letter-spacing: 0.4em;
		text-align: center;
		color: var(--color-text-primary);
		font-family: ui-monospace, 'SF Mono', Menlo, monospace;
		outline: none;
		transition: border-color 0.15s ease, box-shadow 0.15s ease;
	}

	.code-input:focus {
		border-color: rgba(240, 240, 240, 0.3);
		box-shadow: 0 0 0 3px rgba(240, 240, 240, 0.06);
	}

	.code-input:disabled { opacity: 0.55; }

	.btn {
		width: 100%;
		padding: 0.7rem 1rem;
		border-radius: 0.5rem;
		border: none;
		font-size: 0.9375rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		text-decoration: none;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		transition: opacity 0.15s ease, transform 0.1s ease;
	}

	.btn.primary {
		background: var(--color-text-primary);
		color: var(--color-bg);
	}

	.btn.primary:disabled { opacity: 0.4; cursor: not-allowed; }
	.btn.primary:not(:disabled):hover { opacity: 0.88; }
	.btn.primary:not(:disabled):active { transform: scale(0.98); }

	.hint {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		text-align: center;
		margin: 0;
	}

	.actions {
		display: flex;
		justify-content: space-between;
		gap: 0.5rem;
	}

	.link-btn {
		background: none;
		border: none;
		color: var(--color-text-primary);
		font-family: inherit;
		font-size: 0.875rem;
		font-weight: 500;
		cursor: pointer;
		padding: 0.25rem 0;
		transition: opacity 0.15s ease;
	}

	.link-btn.muted { color: var(--color-text-muted); }
	.link-btn:hover:not(:disabled) { opacity: 0.75; }
	.link-btn:disabled { opacity: 0.5; cursor: not-allowed; }

	.error {
		background: rgba(224, 82, 82, 0.08);
		border: 1px solid rgba(224, 82, 82, 0.2);
		border-radius: 0.5rem;
		padding: 0.6rem 0.85rem;
		font-size: 0.875rem;
		color: var(--color-accent);
	}

	.info {
		background: rgba(120, 200, 140, 0.08);
		border: 1px solid rgba(120, 200, 140, 0.18);
		border-radius: 0.5rem;
		padding: 0.6rem 0.85rem;
		font-size: 0.875rem;
		color: #8fcf9b;
		text-align: center;
	}

	.success {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 1.1rem;
		padding: 1rem 0 0.25rem;
	}

	.success-star {
		position: relative;
		width: 5rem;
		height: 5rem;
		display: flex;
		align-items: center;
		justify-content: center;
		isolation: isolate;
	}

	.success-star .halo {
		position: absolute;
		inset: -25%;
		border-radius: 50%;
		background: radial-gradient(circle at center,
			rgba(224, 82, 82, 0.32) 0%,
			rgba(224, 82, 82, 0.12) 35%,
			transparent 70%);
		filter: blur(2px);
		opacity: 0;
		animation: haloBreathe 2.6s ease-in-out 0.45s infinite;
	}

	.success-star .ring {
		position: absolute;
		inset: 0;
		border-radius: 50%;
		border: 1.5px solid rgba(224, 82, 82, 0.55);
		opacity: 0;
		transform: scale(0.4);
		animation: ringExpand 0.95s cubic-bezier(0.22, 1, 0.36, 1) 0.15s forwards;
	}

	.success-star .star {
		position: relative;
		z-index: 1;
		color: var(--color-accent);
		filter: drop-shadow(0 0 12px rgba(224, 82, 82, 0.55));
		transform-origin: center;
		opacity: 0;
		animation:
			starEnter 0.7s cubic-bezier(0.34, 1.4, 0.5, 1) forwards,
			starGlow 2.6s ease-in-out 0.7s infinite;
	}

	.success-title {
		font-size: 1rem;
		font-weight: 600;
		color: var(--color-text-primary);
		margin: 0;
		letter-spacing: -0.005em;
		opacity: 0;
		animation: titleRise 0.5s ease-out 0.3s forwards;
	}

	@keyframes starEnter {
		0%   { transform: scale(0.2) rotate(-25deg); opacity: 0; }
		55%  { transform: scale(1.15) rotate(4deg); opacity: 1; }
		100% { transform: scale(1) rotate(0deg); opacity: 1; }
	}

	@keyframes starGlow {
		0%, 100% { filter: drop-shadow(0 0 10px rgba(224, 82, 82, 0.45)); }
		50%      { filter: drop-shadow(0 0 18px rgba(224, 82, 82, 0.75)); }
	}

	@keyframes haloBreathe {
		0%, 100% { transform: scale(0.85); opacity: 0.55; }
		50%      { transform: scale(1.05); opacity: 0.95; }
	}

	@keyframes ringExpand {
		0%   { transform: scale(0.4); opacity: 0; }
		35%  { opacity: 0.9; }
		100% { transform: scale(1.9); opacity: 0; }
	}

	@keyframes titleRise {
		from { transform: translateY(4px); opacity: 0; }
		to   { transform: translateY(0);    opacity: 1; }
	}

	.shake { animation: shake 0.3s ease-out; }
	@keyframes shake {
		0%, 100% { transform: translateX(0); }
		20% { transform: translateX(-4px); }
		40% { transform: translateX(4px); }
		60% { transform: translateX(-3px); }
		80% { transform: translateX(2px); }
	}
</style>
