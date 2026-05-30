<script lang="ts">
	import type { BadgeCode } from '$lib/types';
	import { badgeMeta, tierStyle } from './badgeMeta';
	import BadgeIcon from './BadgeIcon.svelte';

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
	let glyphPx = $derived(Math.round(px * (meta.glyph === 'helmet' ? 0.58 : 0.56)));
</script>

<span
	class="medallion {size}"
	class:surface-glass={meta.surface === 'glass'}
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
		<BadgeIcon glyph={meta.glyph} size={glyphPx} />
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
		position: relative;
		display: flex;
		align-items: center;
		justify-content: center;
		width: 100%;
		height: 100%;
		border-radius: var(--radius-full);
		background: var(--face);
		overflow: hidden;
		/* Engraved glyph: dark and low-key; reads as struck metal, not print. */
		color: rgba(16, 14, 22, 0.82);
		/* Rim light + inner shadow for a minted, recessed face with depth. */
		box-shadow:
			inset 0 1px 1px rgba(255, 255, 255, 0.4),
			inset 0 -4px 9px rgba(0, 0, 0, 0.32),
			inset 0 0 0 1px rgba(0, 0, 0, 0.14);
	}

	/* Specular highlight (the "blik"): a glossy top-left sheen on every medallion,
	   so each tier reads as polished struck metal, not flat print. */
	.face::after {
		content: '';
		position: absolute;
		inset: 0;
		border-radius: inherit;
		background:
			radial-gradient(circle at 30% 20%, rgba(255, 255, 255, 0.6), rgba(255, 255, 255, 0) 42%),
			linear-gradient(158deg, rgba(255, 255, 255, 0.28) 0%, rgba(255, 255, 255, 0) 40%);
		mix-blend-mode: screen;
		pointer-events: none;
	}

	.surface-glass {
		background: linear-gradient(
			145deg,
			rgba(255, 255, 255, 0.52),
			color-mix(in srgb, var(--accent) 70%, rgba(255, 255, 255, 0.2)) 52%,
			rgba(34, 40, 52, 0.36)
		);
		box-shadow:
			0 0 0 1px rgba(255, 255, 255, 0.16),
			0 8px 24px var(--glow),
			inset 0 1px 0 rgba(255, 255, 255, 0.55);
	}

	/* Glass reuses the shared .face::after blik; only its frosted base differs. */
	.surface-glass .face {
		background:
			linear-gradient(150deg, rgba(245, 248, 255, 0.48) 0%, rgba(156, 170, 190, 0.32) 48%, rgba(45, 52, 66, 0.34) 100%);
		backdrop-filter: blur(12px) saturate(160%);
		-webkit-backdrop-filter: blur(12px) saturate(160%);
		color: rgba(10, 12, 18, 0.88);
		box-shadow:
			inset 0 1px 1px rgba(255, 255, 255, 0.62),
			inset 0 -6px 10px rgba(18, 22, 30, 0.24),
			inset 0 0 0 1px rgba(255, 255, 255, 0.16);
	}
</style>
