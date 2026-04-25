<script lang="ts">
	interface Props {
		width?: string;
		height?: string;
		radius?: string;
		class?: string;
	}
	let { width = '100%', height = '16px', radius = 'var(--radius-sm)', class: cls = '' }: Props = $props();
</script>

<div
	class="skeleton {cls}"
	style="width:{width};height:{height};border-radius:{radius};"
></div>

<style>
	.skeleton {
		background: var(--color-skeleton);
		background-image: linear-gradient(
			90deg,
			var(--color-skeleton) 0%,
			var(--color-surface-raised) 50%,
			var(--color-skeleton) 100%
		);
		background-size: 200% 100%;
		animation: shimmer 1.4s ease infinite;
	}

	@keyframes shimmer {
		0% { background-position: 200% 0; }
		100% { background-position: -200% 0; }
	}

	@media (prefers-reduced-motion: reduce) { .skeleton { animation: none; } }
</style>
