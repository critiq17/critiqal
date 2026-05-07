<script lang="ts">
	import type { PostPhoto } from '$lib/types';

	interface Props {
		photos: PostPhoto[];
		postId: string;
	}

	let { photos, postId }: Props = $props();

	let scrollIndex = $state(0);

	function onscroll(e: Event): void {
		const el = e.currentTarget as HTMLDivElement;
		scrollIndex = Math.round(el.scrollLeft / el.clientWidth);
	}

	const sorted = $derived([...photos].sort((a, b) => a.position - b.position));
</script>

{#if sorted.length > 0}
	<div class="gallery">
		<!-- svelte-ignore a11y_no_redundant_roles -->
		<div
			class="photo-strip"
			role="region"
			aria-label="Post photos for post {postId}"
			{onscroll}
		>
			{#each sorted as photo (photo.id)}
				<div class="photo-item">
					<img src={photo.url} alt="" loading="lazy" />
				</div>
			{/each}
		</div>
		{#if sorted.length > 1}
			<div class="dots" role="presentation">
				{#each sorted as _p, i (i)}
					<div class="dot" class:active={scrollIndex === i}></div>
				{/each}
			</div>
		{/if}
	</div>
{/if}

<style>
	.gallery {
		width: 100%;
	}

	.photo-strip {
		display: flex;
		overflow-x: auto;
		scroll-snap-type: x mandatory;
		scroll-behavior: smooth;
		-webkit-overflow-scrolling: touch;
		scrollbar-width: none;
		gap: 2px;
	}

	.photo-strip::-webkit-scrollbar {
		display: none;
	}

	.photo-item {
		flex-shrink: 0;
		width: 100%;
		scroll-snap-align: start;
	}

	.photo-item img {
		width: 100%;
		max-height: 360px;
		object-fit: cover;
		display: block;
	}

	.dots {
		display: flex;
		justify-content: center;
		gap: 4px;
		padding: 6px 0;
	}

	.dot {
		width: 5px;
		height: 5px;
		border-radius: 50%;
		background: var(--color-border);
		transition: background 0.2s ease, width 0.2s ease;
	}

	.dot.active {
		background: var(--color-text-primary);
		width: 10px;
		border-radius: 3px;
	}

	@media (prefers-reduced-motion: reduce) {
		.dot {
			transition: none;
		}
	}
</style>
