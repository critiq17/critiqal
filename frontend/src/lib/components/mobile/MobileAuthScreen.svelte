<script lang="ts">
	import { authService } from '$lib/services/auth.service';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import type { LoginRequest, RegisterRequest } from '$lib/types';

	type AuthMode = 'login' | 'register';

	let activeMode = $state<AuthMode>('login');
	let username = $state('');
	let password = $state('');
	let error = $state('');
	let loading = $state(false);

	let tabLoginEl = $state<HTMLButtonElement | null>(null);
	let tabRegisterEl = $state<HTMLButtonElement | null>(null);

	let indicatorLeft = $derived(
		activeMode === 'login'
			? '0px'
			: tabLoginEl
				? `${tabLoginEl.offsetWidth + 16}px`
				: '50%'
	);
	let indicatorWidth = $derived(
		activeMode === 'login'
			? (tabLoginEl ? `${tabLoginEl.offsetWidth}px` : '50%')
			: (tabRegisterEl ? `${tabRegisterEl.offsetWidth}px` : '50%')
	);

	function switchMode(mode: AuthMode): void {
		if (mode === activeMode) return;
		activeMode = mode;
		username = '';
		password = '';
		error = '';
	}

	function getErrorMessage(err: unknown): string {
		if (err instanceof Error) return err.message;
		return 'Something went wrong. Please try again.';
	}

	async function handleSubmit(): Promise<void> {
		if (loading) return;
		error = '';
		loading = true;

		try {
			if (activeMode === 'login') {
				const req: LoginRequest = { username, password };
				const res = await authService.login(req);
				await authStore.login(res.user, res.token);
			} else {
				const req: RegisterRequest = { username, password };
				const res = await authService.register(req);
				await authStore.login(res.user, res.token);
			}

			getTelegramWebApp()?.HapticFeedback.notificationOccurred('success');
		} catch (err: unknown) {
			error = getErrorMessage(err);
			getTelegramWebApp()?.HapticFeedback.notificationOccurred('error');
		} finally {
			loading = false;
		}
	}

	let isSubmitDisabled = $derived(
		loading || username.trim().length === 0 || password.length === 0
	);
</script>

<div class="auth-root">
	<div class="hero">
		<span class="app-name">Critiqal</span>
		<span class="tagline">Share what you think. Earn what you deserve.</span>
	</div>

	<div class="auth-sheet">
		<div class="tab-switcher" role="tablist">
			<button
				bind:this={tabLoginEl}
				role="tab"
				aria-selected={activeMode === 'login'}
				class="tab-btn"
				class:tab-active={activeMode === 'login'}
				onclick={() => switchMode('login')}
			>
				Sign in
			</button>
			<button
				bind:this={tabRegisterEl}
				role="tab"
				aria-selected={activeMode === 'register'}
				class="tab-btn"
				class:tab-active={activeMode === 'register'}
				onclick={() => switchMode('register')}
			>
				Register
			</button>
			<div
				class="tab-indicator"
				style="left: {indicatorLeft}; width: {indicatorWidth};"
			></div>
		</div>

		<form onsubmit={(e) => { e.preventDefault(); handleSubmit(); }}>
			<div class="fields">
				<input
					class="field"
					type="text"
					placeholder="Username"
					autocomplete={activeMode === 'login' ? 'username' : 'username'}
					autocapitalize="none"
					spellcheck={false}
					bind:value={username}
					disabled={loading}
				/>

				<input
					class="field"
					type="password"
					placeholder="Password"
					autocomplete={activeMode === 'login' ? 'current-password' : 'new-password'}
					bind:value={password}
					disabled={loading}
				/>
			</div>

			{#if error}
				<p class="error-text" role="alert">{error}</p>
			{/if}

			<button
				type="submit"
				class="submit-btn"
				disabled={isSubmitDisabled}
			>
				{#if loading}
					{activeMode === 'login' ? 'Signing in…' : 'Creating account…'}
				{:else}
					{activeMode === 'login' ? 'Sign in' : 'Create account'}
				{/if}
			</button>
		</form>
	</div>
</div>

<style>
	.auth-root {
		position: fixed;
		inset: 0;
		z-index: 200;
		display: flex;
		flex-direction: column;
		background: #0f0f0f;
		overflow: hidden;
	}

	.hero {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		background:
			radial-gradient(ellipse at 30% 20%, rgba(224, 82, 82, 0.3), transparent 60%),
			radial-gradient(ellipse at 70% 80%, rgba(224, 82, 82, 0.15), transparent 60%);
		padding: 40px 24px 24px;
		gap: 12px;
	}

	.app-name {
		font-size: 40px;
		font-weight: 700;
		color: #f0f0f0;
		letter-spacing: -1px;
	}

	.tagline {
		font-size: 15px;
		color: rgba(240, 240, 240, 0.5);
		text-align: center;
	}

	.auth-sheet {
		background: var(--color-surface, #1a1a1a);
		border-radius: 24px 24px 0 0;
		padding: 24px 24px calc(24px + env(safe-area-inset-bottom, 0px));
		display: flex;
		flex-direction: column;
		gap: 16px;
	}

	/* Tab switcher */
	.tab-switcher {
		position: relative;
		display: flex;
		gap: 16px;
		border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.08));
		padding-bottom: 0;
	}

	.tab-btn {
		background: none;
		border: none;
		padding: 0 0 12px;
		font-size: 15px;
		font-weight: 500;
		color: rgba(240, 240, 240, 0.4);
		cursor: pointer;
		transition: color 0.2s ease;
	}

	.tab-btn.tab-active {
		color: #f0f0f0;
	}

	.tab-indicator {
		position: absolute;
		bottom: -1px;
		height: 2px;
		background: var(--tg-accent, #e05252);
		border-radius: 2px 2px 0 0;
		transition:
			left 0.25s cubic-bezier(0.4, 0, 0.2, 1),
			width 0.25s cubic-bezier(0.4, 0, 0.2, 1);
	}

	/* Form */
	form {
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.fields {
		display: flex;
		flex-direction: column;
		gap: 10px;
	}

	.field {
		width: 100%;
		height: 52px;
		border-radius: 14px;
		background: var(--color-surface-raised, #242424);
		border: 1px solid var(--color-border, rgba(255, 255, 255, 0.08));
		padding: 0 16px;
		font-size: 16px;
		color: var(--color-text-primary, #f0f0f0);
		outline: none;
		box-sizing: border-box;
		transition: border-color 0.15s ease;
	}

	.field::placeholder {
		color: rgba(240, 240, 240, 0.3);
	}

	.field:focus {
		border-color: var(--tg-accent, #e05252);
	}

	.field:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	.error-text {
		color: #e05252;
		font-size: 14px;
		margin: 0;
		line-height: 1.4;
	}

	.submit-btn {
		width: 100%;
		height: 52px;
		border-radius: 14px;
		background: var(--tg-accent, #e05252);
		color: white;
		font-weight: 600;
		font-size: 16px;
		border: none;
		cursor: pointer;
		transition: opacity 0.15s ease;
	}

	.submit-btn:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	.submit-btn:not(:disabled):active {
		opacity: 0.85;
	}
</style>
