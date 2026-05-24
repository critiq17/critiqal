<script lang="ts">
	// Fullscreen scrim shown during a language switch. Uses the brand
	// StarBurst as the focal point so the swap feels intentional, not glitchy.
	// Mounted once at the layout root; visibility driven entirely by i18n.switching.
	import { fade } from 'svelte/transition';
	import StarBurst from '$lib/ui/StarBurst.svelte';
	import { i18n } from './store.svelte';
	import { t } from './translate';
</script>

{#if i18n.switching}
	<div
		class="lang-overlay"
		role="status"
		aria-live="polite"
		transition:fade={{ duration: 180 }}
	>
		<div class="lang-overlay__inner">
			<StarBurst size={72} duration={720} />
			<span class="lang-overlay__label">{t('settings.language.applying')}</span>
		</div>
	</div>
{/if}

<style>
	.lang-overlay {
		position: fixed;
		inset: 0;
		z-index: 9999;
		display: grid;
		place-items: center;
		background: var(--scrim-strong, rgba(0, 0, 0, 0.7));
		backdrop-filter: blur(18px) saturate(160%);
		-webkit-backdrop-filter: blur(18px) saturate(160%);
		pointer-events: all;
	}

	.lang-overlay__inner {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.875rem;
	}

	.lang-overlay__label {
		font-size: 0.875rem;
		color: var(--text-strong, rgba(255, 255, 255, 0.82));
		letter-spacing: 0.01em;
	}
</style>
