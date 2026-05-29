<script lang="ts">
	import type { BadgeGlyph } from './badgeMeta';

	interface Props {
		glyph: BadgeGlyph;
		size?: number;
	}

	let { glyph, size = 24 }: Props = $props();
</script>

<svg
	class="glyph"
	viewBox="0 0 100 100"
	width={size}
	height={size}
	fill="none"
	stroke="currentColor"
	stroke-width="6"
	stroke-linecap="round"
	stroke-linejoin="round"
	aria-hidden="true"
>
	{#if glyph === 'star'}
		<polygon
			fill="currentColor"
			stroke="none"
			points="50,10 60,38 92,38 66,58 76,90 50,70 24,90 34,58 8,38 40,38"
		/>
	{:else if glyph === 'laurel'}
		<path d="M50 88 V40" />
		<path d="M50 60 C30 58 22 46 24 30 C40 32 50 44 50 60" fill="currentColor" stroke="none" opacity="0.9" />
		<path d="M50 60 C70 58 78 46 76 30 C60 32 50 44 50 60" fill="currentColor" stroke="none" opacity="0.9" />
		<path d="M50 78 C34 76 27 66 28 52 C42 54 50 64 50 78" fill="currentColor" stroke="none" opacity="0.6" />
		<path d="M50 78 C66 76 73 66 72 52 C58 54 50 64 50 78" fill="currentColor" stroke="none" opacity="0.6" />
	{:else if glyph === 'swords'}
		<path d="M26 22 L66 62" />
		<path d="M74 22 L34 62" />
		<path d="M22 70 L34 58" />
		<path d="M78 70 L66 58" />
		<path d="M60 66 L74 80" />
		<path d="M40 66 L26 80" />
	{:else if glyph === 'eagle'}
		<path d="M50 30 L50 72" />
		<path d="M50 38 C32 30 18 34 10 46 C26 48 38 46 50 52" fill="currentColor" stroke="none" />
		<path d="M50 38 C68 30 82 34 90 46 C74 48 62 46 50 52" fill="currentColor" stroke="none" />
		<circle cx="50" cy="26" r="6" fill="currentColor" stroke="none" />
	{:else if glyph === 'quill'}
		<path d="M24 80 C40 70 70 40 82 20 C60 28 36 50 28 72" fill="currentColor" stroke="none" opacity="0.92" />
		<path d="M24 80 L40 64" />
		<path d="M50 44 L64 40" />
		<path d="M40 56 L54 52" />
	{:else if glyph === 'column'}
		<path d="M28 26 H72" />
		<path d="M32 26 V74" />
		<path d="M50 26 V74" />
		<path d="M68 26 V74" />
		<path d="M24 74 H76" />
		<path d="M30 20 H70" />
	{:else if glyph === 'scroll'}
		<rect x="28" y="30" width="44" height="40" rx="4" fill="currentColor" stroke="none" opacity="0.9" />
		<path d="M28 30 C20 30 20 42 28 42" />
		<path d="M72 70 C80 70 80 58 72 58" />
		<path d="M38 44 H62" stroke="var(--badge-glyph-line, rgba(0,0,0,0.45))" />
		<path d="M38 54 H58" stroke="var(--badge-glyph-line, rgba(0,0,0,0.45))" />
	{/if}
</svg>

<style>
	.glyph {
		display: block;
		flex-shrink: 0;
	}
</style>
