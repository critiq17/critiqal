<script lang="ts">
	import { fade, scale } from 'svelte/transition';
	import { goto } from '$app/navigation';
	import { authGate } from '$lib/stores/auth-gate.store.svelte';
	import { t } from '$lib/i18n';

	const reasonText = $derived.by(() => {
		switch (authGate.reason) {
			case 'like':    return t('auth.gate.reason.like');
			case 'follow':  return t('auth.gate.reason.follow');
			case 'comment': return t('auth.gate.reason.comment');
			case 'search':  return t('auth.gate.reason.search');
			case 'compose': return t('auth.gate.reason.compose');
			default:        return t('auth.gate.reason.generic');
		}
	});

	function goLogin(): void {
		authGate.close();
		void goto('/login');
	}

	function goRegister(): void {
		authGate.close();
		void goto('/register');
	}

	function dismiss(): void {
		authGate.close();
	}
</script>

{#if authGate.isOpen}
	<div
		class="scrim"
		role="dialog"
		aria-modal="true"
		aria-labelledby="gate-title"
		transition:fade={{ duration: 150 }}
		onclick={dismiss}
		onkeydown={(e) => { if (e.key === 'Escape') dismiss(); }}
		tabindex="-1"
	>
		<div
			class="sheet"
			role="document"
			onclick={(e) => e.stopPropagation()}
			onkeydown={(e) => e.stopPropagation()}
			tabindex="-1"
			transition:scale={{ duration: 220, start: 0.92, opacity: 0 }}
		>
			<div class="star-burst" aria-hidden="true">
				<span class="shockwave"></span>
				<span class="shockwave shockwave-2"></span>
				{#each Array(9) as _, i (i)}
					<span class="spark" style="--i:{i}"></span>
				{/each}
				<svg class="brand-star" viewBox="0 0 24 24" width="34" height="34" fill="currentColor" aria-hidden="true">
					<path d="M12 2l2.39 7.36H22l-6.18 4.49L18.21 22 12 17.27 5.79 22l2.39-8.15L2 9.36h7.61z"/>
				</svg>
			</div>

			<div class="texts">
				<p id="gate-title" class="title">{t('auth.gate.title')}</p>
				<p class="reason">{reasonText}</p>
				<p class="sub">{t('auth.gate.subtitle')}</p>
			</div>

			<div class="actions">
				<button type="button" class="btn primary" onclick={goRegister}>
					{t('auth.gate.register')}
				</button>
				<button type="button" class="btn ghost" onclick={goLogin}>
					{t('auth.gate.login')}
				</button>
			</div>

			<button type="button" class="later" onclick={dismiss}>
				{t('auth.gate.later')}
			</button>
		</div>
	</div>
{/if}

<style>
	.scrim {
		position: fixed;
		inset: 0;
		background: var(--scrim-strong, rgba(0, 0, 0, 0.7));
		backdrop-filter: blur(6px);
		-webkit-backdrop-filter: blur(6px);
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 1.25rem;
		z-index: 1000;
	}

	.sheet {
		width: 100%;
		max-width: 22rem;
		background: var(--color-surface);
		border: 1px solid var(--color-border);
		border-radius: 1rem;
		padding: 2rem 1.5rem 1.4rem;
		display: flex;
		flex-direction: column;
		gap: 1.25rem;
		box-shadow: 0 24px 60px rgba(0, 0, 0, 0.55);
		position: relative;
	}

	.star-burst {
		position: relative;
		align-self: center;
		width: 4.5rem;
		height: 4.5rem;
		display: flex;
		align-items: center;
		justify-content: center;
		color: var(--color-accent);
	}

	.brand-star {
		position: relative;
		z-index: 2;
		filter: drop-shadow(0 0 14px color-mix(in srgb, var(--color-accent) 70%, transparent));
		animation: starExplode 700ms cubic-bezier(0.22, 1.4, 0.4, 1) backwards;
	}

	.shockwave {
		position: absolute;
		left: 50%;
		top: 50%;
		width: 4.5rem;
		height: 4.5rem;
		border-radius: 50%;
		background: radial-gradient(
			circle,
			color-mix(in srgb, var(--color-accent) 55%, transparent) 0%,
			color-mix(in srgb, var(--color-accent) 18%, transparent) 45%,
			transparent 70%
		);
		transform: translate(-50%, -50%) scale(0);
		opacity: 0;
		animation: shock 850ms cubic-bezier(0.16, 0.84, 0.44, 1) forwards;
		pointer-events: none;
		z-index: 0;
	}

	.shockwave-2 {
		animation-delay: 110ms;
		background: radial-gradient(
			circle,
			color-mix(in srgb, var(--color-accent) 35%, transparent) 0%,
			transparent 60%
		);
	}

	.spark {
		position: absolute;
		left: 50%;
		top: 50%;
		width: 6px;
		height: 2px;
		border-radius: 1px;
		background: linear-gradient(
			to right,
			color-mix(in srgb, var(--color-accent) 95%, white 10%),
			color-mix(in srgb, var(--color-accent) 50%, transparent)
		);
		box-shadow: 0 0 6px color-mix(in srgb, var(--color-accent) 80%, transparent);
		opacity: 0;
		--angle: calc(var(--i) * 40deg);
		transform-origin: 0 50%;
		animation: spark 900ms cubic-bezier(0.16, 0.84, 0.44, 1) forwards;
		animation-delay: calc(var(--i) * 18ms);
		z-index: 1;
	}

	@keyframes spark {
		0%   { opacity: 0; transform: rotate(var(--angle)) translateX(2px) scaleX(0.4); }
		25%  { opacity: 1; transform: rotate(var(--angle)) translateX(14px) scaleX(1.2); }
		100% { opacity: 0; transform: rotate(var(--angle)) translateX(46px) scaleX(0.3); }
	}

	@keyframes shock {
		0%   { transform: translate(-50%, -50%) scale(0.2); opacity: 0.95; }
		60%  { opacity: 0.55; }
		100% { transform: translate(-50%, -50%) scale(2.4); opacity: 0; }
	}

	@keyframes starExplode {
		0%   { transform: scale(0); opacity: 0; }
		35%  { transform: scale(1.55); opacity: 1; }
		60%  { transform: scale(0.92); }
		80%  { transform: scale(1.08); }
		100% { transform: scale(1); opacity: 1; }
	}

	.texts {
		text-align: center;
		display: flex;
		flex-direction: column;
		gap: 0.3rem;
	}

	.title {
		font-size: 1.125rem;
		font-weight: 700;
		color: var(--color-text-primary);
		margin: 0;
		letter-spacing: -0.01em;
	}

	.reason {
		font-size: 0.9375rem;
		color: var(--color-text-secondary);
		margin: 0;
	}

	.sub {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		margin: 0;
	}

	.actions {
		display: flex;
		flex-direction: column;
		gap: 0.55rem;
	}

	.btn {
		width: 100%;
		padding: 0.75rem 1rem;
		border-radius: 0.625rem;
		border: 1px solid transparent;
		font-size: 0.9375rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition: opacity 0.15s ease, transform 0.1s ease, background 0.15s ease;
	}

	.btn.primary {
		background: var(--color-text-primary);
		color: var(--color-bg);
	}

	.btn.primary:hover { opacity: 0.9; }
	.btn.primary:active { transform: scale(0.98); }

	.btn.ghost {
		background: transparent;
		color: var(--color-text-primary);
		border-color: var(--color-border);
	}

	.btn.ghost:hover { background: var(--surface-tint-subtle); }

	.later {
		background: none;
		border: none;
		color: var(--color-text-muted);
		font-size: 0.8125rem;
		font-family: inherit;
		cursor: pointer;
		padding: 0.3rem;
		align-self: center;
		transition: color 0.15s ease;
	}

	.later:hover { color: var(--color-text-secondary); }
</style>
