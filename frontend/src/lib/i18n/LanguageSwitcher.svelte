<script lang="ts">
	// Segmented control matching the settings page glass aesthetic.
	// The sliding pill is a layered element animated with a translateX —
	// transform-only animation means the layout never reflows mid-press.
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

<div class="lang-switch" role="radiogroup" aria-label={t('settings.language.title')}>
	<div
		class="lang-switch__pill"
		style="--idx: {activeIndex}; --count: {LOCALES.length}"
		aria-hidden="true"
	></div>
	{#each LOCALES as locale (locale)}
		{@const active = locale === i18n.locale}
		<button
			type="button"
			role="radio"
			aria-checked={active}
			class="lang-switch__btn"
			class:active
			disabled={i18n.switching}
			onclick={() => pick(locale)}
		>
			<span class="lang-switch__code">{labels[locale]}</span>
			<span class="lang-switch__name">{t(`settings.language.${locale}`)}</span>
		</button>
	{/each}
</div>

<style>
	.lang-switch {
		position: relative;
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 0;
		padding: 0.25rem;
		border-radius: 0.875rem;
		background: var(--surface-tint-subtle);
		border: 1px solid var(--divider-soft);
		overflow: hidden;
	}

	.lang-switch__pill {
		position: absolute;
		top: 0.25rem;
		left: 0.25rem;
		bottom: 0.25rem;
		width: calc((100% - 0.5rem) / var(--count));
		background: var(--color-text-primary);
		border-radius: 0.625rem;
		transform: translateX(calc(var(--idx) * 100%));
		transition: transform 380ms cubic-bezier(0.34, 1.56, 0.64, 1);
		box-shadow: 0 1px 6px rgba(0, 0, 0, 0.2);
		will-change: transform;
	}

	.lang-switch__btn {
		position: relative;
		z-index: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 0.0625rem;
		padding: 0.5rem 0.25rem;
		border: none;
		background: transparent;
		font-family: inherit;
		cursor: pointer;
		color: var(--text-secondary-2);
		border-radius: 0.625rem;
		transition: color 180ms ease;
	}

	.lang-switch__btn.active {
		color: var(--color-bg);
	}

	.lang-switch__btn:disabled {
		cursor: progress;
	}

	.lang-switch__code {
		font-size: 0.75rem;
		font-weight: 700;
		letter-spacing: 0.04em;
		line-height: 1;
	}

	.lang-switch__name {
		font-size: 0.6875rem;
		font-weight: 500;
		opacity: 0.7;
		line-height: 1.1;
	}

	@media (prefers-reduced-motion: reduce) {
		.lang-switch__pill {
			transition: none;
		}
	}
</style>
