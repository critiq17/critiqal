<script lang="ts">
	import type { BadgeGlyph } from './badgeMeta';
	import BadgeGlyphSvg from './BadgeGlyph.svelte';

	interface Props {
		glyph: BadgeGlyph;
		size?: number;
	}

	let { glyph, size = 24 }: Props = $props();

	const ASSET_GLYPHS: Partial<Record<BadgeGlyph, string>> = {
		centurion: '/assets/badges/centurion.png',
		legatus: '/assets/badges/legatus.png',
		scribe: '/assets/badges/scribe.png',
		orator: '/assets/badges/orator.png',
		scroll: '/assets/badges/scroll.png',
	};

	let assetSrc = $derived(ASSET_GLYPHS[glyph]);
</script>

{#if assetSrc}
	<span
		class="asset-glyph"
		style:width="{size}px"
		style:height="{size}px"
		aria-hidden="true"
	>
		<img src={assetSrc} alt="" loading="lazy" decoding="async" />
	</span>
{:else}
	<BadgeGlyphSvg {glyph} {size} />
{/if}

<style>
	.asset-glyph {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.asset-glyph img {
		display: block;
		width: 100%;
		height: 100%;
		object-fit: contain;
		mix-blend-mode: multiply;
		filter: contrast(1.18);
		pointer-events: none;
		user-select: none;
	}
</style>
