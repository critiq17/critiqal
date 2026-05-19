<script lang="ts">
	// The Critiqal mark drawing itself: outline strokes on, fills, holds, then
	// gently restarts. Used as a quiet brand loader (feed) and as the posting
	// indicator. Tunable speed; respects prefers-reduced-motion.
	interface Props {
		size?: number;
		color?: string;
		duration?: number;
		title?: string;
	}
	let {
		size = 44,
		color = '#FF5A4A',
		duration = 1600,
		title = 'Loading'
	}: Props = $props();
</script>

<svg
	class="star-draw"
	viewBox="0 0 100 100"
	width={size}
	height={size}
	role="img"
	aria-label={title}
	style="--star-color:{color}; --star-dur:{duration}ms"
>
	<polygon
		class="star-draw-shape"
		points="50,8 60,38 96,28 66,52 78,92 50,72 22,92 34,52 4,40 40,38"
	/>
</svg>

<style>
	.star-draw {
		display: block;
		flex-shrink: 0;
	}

	.star-draw-shape {
		fill: var(--star-color);
		stroke: var(--star-color);
		stroke-width: 1.6;
		stroke-linejoin: round;
		stroke-dasharray: 900;
		transform-origin: 50% 50%;
		transform-box: fill-box;
		animation: starDraw var(--star-dur) ease-in-out infinite;
	}

	/* outline draws on → fills → holds → quietly fades and redraws */
	@keyframes starDraw {
		0% {
			stroke-dashoffset: 900;
			fill-opacity: 0;
			opacity: 1;
		}
		45% {
			stroke-dashoffset: 0;
			fill-opacity: 0;
		}
		60% {
			stroke-dashoffset: 0;
			fill-opacity: 1;
		}
		82% {
			fill-opacity: 1;
			opacity: 1;
		}
		100% {
			fill-opacity: 1;
			opacity: 0;
			stroke-dashoffset: 900;
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.star-draw-shape {
			animation: none;
			fill-opacity: 1;
			stroke-dashoffset: 0;
		}
	}
</style>
