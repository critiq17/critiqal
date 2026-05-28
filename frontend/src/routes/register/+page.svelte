<script lang="ts">
	import { goto } from '$app/navigation';
	import { fly } from 'svelte/transition';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { authService } from '$lib/services/auth.service';
	import { verifyEmailStore } from '$lib/stores/verify-email.store.svelte';
	import { ApiError } from '$lib/types';
	import PasswordStrengthIndicator from '$lib/components/PasswordStrengthIndicator.svelte';
	import StarfieldBackdrop from '$lib/ui/StarfieldBackdrop.svelte';
	import { t } from '$lib/i18n';

	let username = $state('');
	let email = $state('');
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
			error = t('auth.register.weakPasswordError');
			shakeKey++;
			return;
		}
		isSubmitting = true;
		error = '';
		try {
			const user = await authService.register({
				username: username.trim(),
				password,
				email: email.trim(),
			});
			await authStore.login(user);
			if (user.emailVerified) {
				goto('/');
			} else {
				verifyEmailStore.start(email.trim());
				goto('/verify-email');
			}
		} catch (err: unknown) {
			if (err instanceof ApiError) {
				const msg = err.message?.toLowerCase() ?? '';
				if (msg.includes('already taken')) error = t('auth.register.usernameTakenError');
				else if (msg.includes('email')) error = t('auth.errors.emailTaken');
				else error = err.message || t('common.somethingWentWrong');
			} else {
				error = t('common.somethingWentWrong');
			}
			shakeKey++;
		} finally {
			isSubmitting = false;
		}
	}
</script>

<svelte:head>
	<title>{t('auth.register.title')} — Critiqal</title>
	<meta name="description" content="Create your Critiqal account" />
</svelte:head>

<div class="page">
	<StarfieldBackdrop />
	<div class="card glass glass-strong" aria-label={t('auth.register.title')} in:fly={{ y: 10, duration: 220 }}>
		<div class="card-header">
			<span class="logo">critiqal</span>
			<p class="subtitle">{t('auth.register.subtitle')}</p>
		</div>

		{#if error}
			{#key shakeKey}
				<div class="error shake" role="alert">{error}</div>
			{/key}
		{/if}

		<form class="form" onsubmit={(e) => { e.preventDefault(); handleRegister(); }}>
			<div class="field">
				<label for="username" class="field-label">{t('auth.register.username')}</label>
				<input
					id="username"
					type="text"
					class="field-input"
					bind:value={username}
					autocomplete="username"
					autocapitalize="none"
					spellcheck={false}
					required
					minlength={3}
					maxlength={30}
					disabled={isSubmitting}
				/>
			</div>

			<div class="field">
				<label for="email" class="field-label">{t('auth.register.email')}</label>
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

			<div class="field">
				<label for="password" class="field-label">{t('auth.register.password')}</label>
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
					<p class="field-hint">{t('auth.register.passwordHint')}</p>
				{/if}
			</div>

			<button type="submit" class="submit-btn" disabled={isSubmitting}>
				{isSubmitting ? t('auth.register.submitting') : t('auth.register.submit')}
			</button>
		</form>

		<p class="switch-link">{t('auth.register.haveAccount')} <a href="/login">{t('auth.register.signIn')}</a></p>
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
		border-radius: 1rem;
		padding: 2.5rem;
		display: flex;
		flex-direction: column;
		gap: 1.5rem;
		box-shadow:
			inset 0 1px 0 var(--surface-tint-strong),
			0 1px 2px rgba(0, 0, 0, 0.06),
			0 24px 48px -16px rgba(0, 0, 0, 0.35);
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
		border: none;
		box-shadow:
			inset 0 1px 0 var(--surface-tint-soft),
			inset 0 0 0 1px var(--glass-border),
			0 1px 2px rgba(0, 0, 0, 0.08);
		border-radius: 0.625rem;
		padding: 0.7rem 0.85rem;
		font-size: 0.9375rem;
		color: var(--color-text-primary);
		font-family: inherit;
		outline: none;
		transition:
			box-shadow var(--duration-micro) var(--ease-out-quart),
			background-color var(--duration-micro) var(--ease-out-quart),
			transform var(--duration-micro) var(--ease-out-quart);
	}

	.field-input::placeholder { color: var(--color-text-muted); opacity: 0.5; }

	.field-input:hover:not(:disabled):not(:focus) {
		background: var(--color-surface-elevated, var(--color-surface-raised));
		box-shadow:
			inset 0 1px 0 var(--surface-tint-medium),
			inset 0 0 0 1px var(--surface-tint-medium),
			0 2px 6px rgba(0, 0, 0, 0.12);
	}

	.field-input:focus {
		background: var(--color-surface-elevated, var(--color-surface-raised));
		box-shadow:
			inset 0 1px 0 var(--surface-tint-strong),
			inset 0 0 0 1.5px var(--color-text-primary),
			0 0 0 4px rgba(255, 255, 255, 0.05),
			0 4px 12px rgba(0, 0, 0, 0.18);
	}

	.field-input:disabled { opacity: 0.5; cursor: not-allowed; }

	.submit-btn {
		width: 100%;
		padding: 0.75rem 1rem;
		border-radius: 0.625rem;
		border: none;
		background: var(--color-text-primary);
		color: var(--color-bg);
		font-size: 0.9375rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		box-shadow:
			inset 0 1px 0 rgba(255, 255, 255, 0.15),
			0 1px 2px rgba(0, 0, 0, 0.1),
			0 6px 16px -4px rgba(0, 0, 0, 0.2);
		transition:
			transform var(--duration-press) var(--ease-out-quart),
			box-shadow var(--duration-micro) var(--ease-out-quart),
			opacity var(--duration-micro) var(--ease-out-quart);
		margin-top: 0.25rem;
	}

	.submit-btn:hover:not(:disabled) {
		transform: translateY(-1px);
		box-shadow:
			inset 0 1px 0 rgba(255, 255, 255, 0.2),
			0 2px 4px rgba(0, 0, 0, 0.12),
			0 10px 24px -6px rgba(0, 0, 0, 0.28);
	}
	.submit-btn:active:not(:disabled) { transform: scale(0.97); }
	.submit-btn:disabled { opacity: 0.4; cursor: not-allowed; }

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
