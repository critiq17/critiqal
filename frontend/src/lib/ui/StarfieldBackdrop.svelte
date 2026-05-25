<script lang="ts">
	// Decorative drifting starfield for auth pages.
	// Pure CSS animation, no JS per-frame work. Stars sit at z-index 0 behind
	// the card; positions and timings are hand-tuned for an even distribution.
	interface Star {
		top: string;
		left: string;
		size: number;
		opacity: number;
		duration: number;
		delay: number;
		drift: number;
		rotate: number;
	}

	const stars: Star[] = [
		{ top: '8%',  left: '12%', size: 22, opacity: 0.32, duration: 22, delay: 0,    drift: 22, rotate: -8 },
		{ top: '18%', left: '78%', size: 28, opacity: 0.28, duration: 28, delay: -6,   drift: 28, rotate: 12 },
		{ top: '32%', left: '6%',  size: 18, opacity: 0.26, duration: 24, delay: -12,  drift: 18, rotate: 20 },
		{ top: '46%', left: '88%', size: 24, opacity: 0.30, duration: 26, delay: -3,   drift: 24, rotate: -14 },
		{ top: '58%', left: '22%', size: 16, opacity: 0.22, duration: 32, delay: -18,  drift: 16, rotate: 8 },
		{ top: '70%', left: '70%', size: 26, opacity: 0.28, duration: 25, delay: -9,   drift: 26, rotate: -6 },
		{ top: '82%', left: '14%', size: 20, opacity: 0.24, duration: 30, delay: -15,  drift: 20, rotate: 16 },
		{ top: '88%', left: '82%', size: 17, opacity: 0.22, duration: 27, delay: -21,  drift: 18, rotate: -10 },
		{ top: '4%',  left: '48%', size: 14, opacity: 0.20, duration: 33, delay: -7,   drift: 14, rotate: 4 },
		{ top: '52%', left: '50%', size: 32, opacity: 0.18, duration: 36, delay: -14,  drift: 30, rotate: -18 }
	];
</script>

<div class="starfield" aria-hidden="true">
	{#each stars as s, i (i)}
		<svg
			class="star"
			style="top: {s.top}; left: {s.left}; width: {s.size}px; height: {s.size}px; opacity: {s.opacity}; --dur: {s.duration}s; --delay: {s.delay}s; --drift: {s.drift}px; --rot: {s.rotate}deg;"
			viewBox="0 0 100 100"
		>
			<polygon
				fill="#FF5A4A"
				points="50,8 60,38 96,28 66,52 78,92 50,72 22,92 34,52 4,40 40,38"
			/>
		</svg>
	{/each}
</div>

<style>
	.starfield {
		position: fixed;
		inset: 0;
		pointer-events: none;
		overflow: hidden;
		z-index: 0;
	}

	.star {
		position: absolute;
		filter:
			drop-shadow(0 0 8px rgba(255, 90, 74, 0.75))
			drop-shadow(0 0 18px rgba(224, 82, 82, 0.45));
		will-change: transform, opacity;
		animation:
			drift var(--dur) ease-in-out infinite,
			twinkle calc(var(--dur) * 0.5) ease-in-out infinite;
		animation-delay: var(--delay);
		transform-origin: center;
	}

	@keyframes drift {
		0%   { transform: translate3d(0, 0, 0) rotate(0deg); }
		25%  { transform: translate3d(calc(var(--drift) * 0.6), calc(var(--drift) * -0.4), 0) rotate(calc(var(--rot) * 0.5)); }
		50%  { transform: translate3d(calc(var(--drift) * -0.3), calc(var(--drift) * -0.8), 0) rotate(var(--rot)); }
		75%  { transform: translate3d(calc(var(--drift) * -0.7), calc(var(--drift) * 0.3), 0) rotate(calc(var(--rot) * 0.4)); }
		100% { transform: translate3d(0, 0, 0) rotate(0deg); }
	}

	@keyframes twinkle {
		0%, 100% { filter: drop-shadow(0 0 8px rgba(255, 90, 74, 0.75)) drop-shadow(0 0 18px rgba(224, 82, 82, 0.45)); }
		50%      { filter: drop-shadow(0 0 14px rgba(255, 90, 74, 1)) drop-shadow(0 0 28px rgba(224, 82, 82, 0.65)); }
	}

	@media (prefers-reduced-motion: reduce) {
		.star {
			animation: none;
		}
	}

	/* Light theme — push the tint stronger so stars stay visible on white. */
	:global([data-theme='light']) .star {
		filter:
			drop-shadow(0 0 6px rgba(210, 58, 58, 0.45))
			drop-shadow(0 0 14px rgba(210, 58, 58, 0.22));
	}
</style>
