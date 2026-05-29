<script lang="ts">
	import type { BadgeCode } from '$lib/types';
	import { badgeMeta, tierStyle } from './badgeMeta';
	import BadgeGlyph from './BadgeGlyph.svelte';

	type Size = 'sm' | 'md' | 'lg';

	interface Props {
		code: BadgeCode;
		// Accessible label (badge name from the backend).
		name?: string;
		size?: Size;
	}

	let { code, name, size = 'md' }: Props = $props();

	const PX: Record<Size, number> = { sm: 34, md: 50, lg: 76 };

	let meta = $derived(badgeMeta(code));
	let style = $derived(tierStyle(meta.tier));
	let px = $derived(PX[size]);
	let glyphPx = $derived(Math.round(px * 0.52));
</script>

<span
	class="medallion {size}"
	style:width="{px}px"
	style:height="{px}px"
	style:--accent={style.accent}
	style:--glow={style.glow}
	style:--face={style.gradient}
	role="img"
	aria-label={name ?? code}
	title={name ?? code}
>
	<span class="face">
		<BadgeGlyph glyph={meta.glyph} size={glyphPx} />
	</span>
</span>

<style>
	.medallion {
		position: relative;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		border-radius: var(--radius-full);
		padding: 2px;
		background: var(--accent);
		box-shadow:
			0 0 0 1px color-mix(in srgb, var(--accent) 60%, transparent),
			0 4px 14px var(--glow);
		flex-shrink: 0;
	}

	.face {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 100%;
		height: 100%;
		border-radius: var(--radius-full);
		background: var(--face);
		color: rgba(20, 18, 30, 0.62);
		/* Subtle top highlight for a glassy, minted look. */
		box-shadow:
			inset 0 1px 2px rgba(255, 255, 255, 0.55),
			inset 0 -3px 6px rgba(0, 0, 0, 0.18);
	}
</style>
