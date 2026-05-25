<script lang="ts">
	// Glass segmented control. The active pill is a layered absolutely-positioned
	// element that animates with translateX — no layout reflow per press.
	import { i18n } from './store.svelte';
	import { LOCALES, type Locale } from './types';
	import { t } from './translate';

	const labels: Record<Locale, string> = {
		ru: 'RU',
		uk: 'UK',
		en: 'EN'
	};

	let activeIndex = $derived(LOCALES.indexOf(i18n.locale));

	function pick(locale: Locale): void {
		if (locale === i18n.locale || i18n.switching) return;
		void i18n.setLocale(locale);
	}
</script>

<div class="lang" role="radiogroup" aria-label={t('settings.language.title')}>
	<div
		class="lang__pill"
		style="--idx: {activeIndex}; --count: {LOCALES.length}"
		aria-hidden="true"
	></div>
	{#each LOCALES as locale (locale)}
		{@const active = locale === i18n.locale}
		<button
			type="button"
			role="radio"
			aria-checked={active}
			class="lang__btn"
			class:active
			disabled={i18n.switching}
			onclick={() => pick(locale)}
		>
			<span class="lang__code">{labels[locale]}</span>
			<span class="lang__name">{t(`settings.language.${locale}`)}</span>
		</button>
	{/each}
</div>

<style>
	.lang {
		position: relative;
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 0;
		padding: 0.3125rem;
		border-radius: 1rem;
		background: var(--glass-bg-soft);
		backdrop-filter: blur(calc(var(--glass-blur) + 6px)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(calc(var(--glass-blur) + 6px)) saturate(var(--glass-saturate));
		border: 1px solid var(--glass-border);
		box-shadow:
			inset 0 1px 0 var(--glass-highlight),
			0 2px 8px rgba(0, 0, 0, 0.18);
		overflow: hidden;
		isolation: isolate;
	}

	@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
		.lang {
			background: var(--surface-tint-soft);
		}
	}

	/* A faint highlight sweep gives the surface a "liquid glass" feel. */
	.lang::before {
		content: '';
		position: absolute;
		inset: 0;
		pointer-events: none;
		background: linear-gradient(
			135deg,
			rgba(255, 255, 255, 0.04) 0%,
			rgba(255, 255, 255, 0) 40%,
			rgba(255, 255, 255, 0) 100%
		);
		border-radius: inherit;
		z-index: 0;
	}

	.lang__pill {
		position: absolute;
		top: 0.3125rem;
		left: 0.3125rem;
		bottom: 0.3125rem;
		width: calc((100% - 0.625rem) / var(--count));
		background:
			linear-gradient(180deg,
				rgba(255, 255, 255, 0.06) 0%,
				rgba(255, 255, 255, 0) 100%
			),
			var(--color-text-primary);
		border-radius: 0.75rem;
		transform: translateX(calc(var(--idx) * 100%));
		transition: transform 460ms cubic-bezier(0.32, 1.6, 0.54, 1);
		box-shadow:
			0 1px 0 rgba(255, 255, 255, 0.18) inset,
			0 6px 14px rgba(0, 0, 0, 0.22),
			0 1px 2px rgba(0, 0, 0, 0.18);
		will-change: transform;
		z-index: 1;
	}

	.lang__btn {
		position: relative;
		z-index: 2;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 0.125rem;
		padding: 0.5625rem 0.25rem;
		border: none;
		background: transparent;
		font-family: inherit;
		cursor: pointer;
		color: var(--text-secondary-2);
		border-radius: 0.75rem;
		transition: color 200ms ease, transform 200ms ease;
		-webkit-tap-highlight-color: transparent;
	}

	.lang__btn:hover:not(:disabled):not(.active) {
		color: var(--text-strong);
	}

	.lang__btn:active:not(:disabled) {
		transform: scale(0.97);
	}

	.lang__btn.active {
		color: var(--color-bg);
	}

	.lang__btn:disabled {
		cursor: progress;
	}

	.lang__code {
		font-size: 0.75rem;
		font-weight: 700;
		letter-spacing: 0.06em;
		line-height: 1;
	}

	.lang__name {
		font-size: 0.6875rem;
		font-weight: 500;
		opacity: 0.78;
		line-height: 1.1;
	}

	@media (prefers-reduced-motion: reduce) {
		.lang__pill {
			transition: none;
		}
		.lang__btn:active:not(:disabled) {
			transform: none;
		}
	}
</style>
