<script lang="ts">
	// One-shot brand burst for success moments. Outline traces in, fills,
	// settles. No loop, no infinite — once it lands it stays put.
	// Respects prefers-reduced-motion (renders the final filled star).
	interface Props {
		size?: number;
		color?: string;
		duration?: number;
		title?: string;
	}
	let {
		size = 36,
		color = '#FF5A4A',
		duration = 720,
		title = 'Success'
	}: Props = $props();
</script>

<svg
	class="star-burst"
	viewBox="0 0 100 100"
	width={size}
	height={size}
	role="img"
	aria-label={title}
	style="--star-color:{color}; --star-dur:{duration}ms"
>
	<polygon
		class="star-burst-shape"
		points="50,8 60,38 96,28 66,52 78,92 50,72 22,92 34,52 4,40 40,38"
	/>
</svg>

<style>
	.star-burst {
		display: block;
		flex-shrink: 0;
	}

	.star-burst-shape {
		fill: var(--star-color);
		stroke: var(--star-color);
		stroke-width: 1.6;
		stroke-linejoin: round;
		stroke-dasharray: 900;
		transform-origin: 50% 50%;
		transform-box: fill-box;
		animation: starBurst var(--star-dur) cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
	}

	@keyframes starBurst {
		0% {
			stroke-dashoffset: 900;
			fill-opacity: 0;
			transform: scale(0.4);
			opacity: 0;
		}
		55% {
			stroke-dashoffset: 0;
			fill-opacity: 0;
			opacity: 1;
		}
		75% {
			fill-opacity: 1;
			transform: scale(1.08);
		}
		100% {
			fill-opacity: 1;
			transform: scale(1);
			opacity: 1;
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.star-burst-shape {
			animation: none;
			fill-opacity: 1;
			stroke-dashoffset: 0;
		}
	}
</style>
