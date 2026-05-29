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
	{#if glyph === 'numeral-i'}
		<!-- Serif Roman capital I, "the first". -->
		<path
			fill="currentColor"
			stroke="none"
			d="M32 20 L68 20 L68 30 L56 30 L56 70 L68 70 L68 80 L32 80 L32 70 L44 70 L44 30 L32 30 Z"
		/>
	{:else if glyph === 'helmet'}
		<!-- Side-profile Spartan helmet traced from the supplied logo PNG. -->
		<g transform="translate(0 100) scale(0.1 -0.1)" fill="currentColor" stroke="none" fill-rule="evenodd">
			<path
				d="M345 981 c-16 -4 -29 -11 -29 -17 1 -5 5 -25 8 -44 l6 -35 -15 39 c-8 22 -17 41 -19 44 -5 5 -126 -49 -126 -56 0 -2 9 -21 21 -41 l21 -36 -29 34 c-25 28 -33 32 -50 23 -49 -27 -47 -36 28 -98 39 -33 76 -58 82 -57 7 1 38 11 69 22 123 45 266 19 345 -61 65 -66 83 -118 82 -230 0 -51 -5 -114 -9 -140 -11 -57 9 -134 42 -166 32 -29 90 -52 133 -52 l36 0 -39 38 c-38 35 -62 89 -62 139 0 11 9 50 20 85 25 79 25 75 -6 82 -15 4 -24 9 -22 12 3 2 17 0 31 -5 25 -10 27 -8 37 27 5 20 10 53 10 74 l0 36 -42 -4 c-40 -3 -35 0 20 12 21 4 23 9 16 37 -3 18 -13 49 -21 69 l-15 37 -39 -19 c-49 -25 -51 -19 -3 9 l35 21 -18 30 c-10 17 -31 43 -46 58 l-29 27 -45 -50 c-25 -27 -40 -41 -32 -30 7 11 26 35 43 52 29 32 29 33 10 48 -10 9 -41 27 -67 41 l-48 25 -20 -43 c-11 -24 -17 -34 -14 -23 21 70 21 72 -34 84 -85 18 -89 17 -94 -31 l-4 -43 -1 43 -2 42 -42 -1 c-24 -1 -56 -4 -73 -8z"
			/>
			<path
				d="M380 760 c-52 -9 -130 -39 -130 -50 0 -4 8 -14 18 -24 17 -15 22 -15 54 -2 172 72 342 -24 373 -211 9 -55 22 -55 30 1 15 101 -55 222 -152 263 -58 25 -132 33 -193 23z"
			/>
			<path
				d="M358 686 c-52 -14 -126 -59 -135 -82 -3 -8 3 -22 13 -30 37 -30 140 -74 194 -83 46 -7 49 -9 19 -10 -40 -1 -131 27 -176 55 -15 10 -51 40 -80 67 l-51 50 -12 -44 c-15 -55 -66 -316 -75 -387 -7 -52 1 -66 23 -39 5 6 22 59 37 117 15 58 32 111 38 118 21 27 137 8 137 -23 0 -19 -57 -45 -97 -45 l-28 0 -29 -160 c-16 -88 -27 -162 -24 -165 15 -15 243 109 286 155 19 21 20 25 7 51 -34 66 -11 112 59 115 41 1 46 -1 46 -20 0 -43 52 -127 96 -159 47 -33 104 -60 104 -50 0 4 -15 48 -32 99 l-33 92 18 49 c22 62 22 147 -2 198 -25 56 -90 111 -152 129 -62 19 -88 19 -151 2z m130 -248 c24 -24 8 -78 -24 -78 -17 0 -44 28 -44 45 0 17 27 45 44 45 6 0 17 -5 24 -12z"
			/>
		</g>
	{:else if glyph === 'laurel'}
		<path d="M50 88 V40" />
		<path d="M50 60 C30 58 22 46 24 30 C40 32 50 44 50 60" fill="currentColor" stroke="none" opacity="0.9" />
		<path d="M50 60 C70 58 78 46 76 30 C60 32 50 44 50 60" fill="currentColor" stroke="none" opacity="0.9" />
		<path d="M50 78 C34 76 27 66 28 52 C42 54 50 64 50 78" fill="currentColor" stroke="none" opacity="0.6" />
		<path d="M50 78 C66 76 73 66 72 52 C58 54 50 64 50 78" fill="currentColor" stroke="none" opacity="0.6" />
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
