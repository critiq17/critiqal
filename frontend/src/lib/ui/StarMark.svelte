<script lang="ts">
	// Static Critiqal mark — the sharp asymmetric coral star.
	interface Props {
		size?: number;
		color?: string;
		title?: string;
	}
	let { size = 22, color = '#FF5A4A', title }: Props = $props();
</script>

<svg
	class="star-mark"
	viewBox="0 0 100 100"
	width={size}
	height={size}
	role={title ? 'img' : 'presentation'}
	aria-label={title}
	aria-hidden={title ? undefined : 'true'}
>
	<polygon
		fill={color}
		points="50,8 60,38 96,28 66,52 78,92 50,72 22,92 34,52 4,40 40,38"
	/>
</svg>

<style>
	.star-mark {
		display: block;
		flex-shrink: 0;
	}
</style>
