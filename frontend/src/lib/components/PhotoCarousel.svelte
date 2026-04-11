<script lang="ts">
	import type { PostPhoto } from '$lib/types';

	interface Props {
		photos: PostPhoto[];
	}

	let { photos }: Props = $props();

	let currentIndex = $state(0);

	const sorted = $derived([...photos].sort((a, b) => a.position - b.position));
	const total = $derived(sorted.length);
	const hasPrev = $derived(currentIndex > 0);
	const hasNext = $derived(currentIndex < total - 1);

	function prev(): void {
		if (hasPrev) currentIndex -= 1;
	}

	function next(): void {
		if (hasNext) currentIndex += 1;
	}

	function goTo(index: number): void {
		currentIndex = index;
	}


</script>

{#if total > 0}
	<div
		class="carousel"
		role="region"
		aria-label="Post photos"
	>
		<div class="carousel-track" style:transform="translateX({-currentIndex * 100}%)">
			{#each sorted as photo (photo.id)}
				<div class="carousel-slide">
					<img
						src={photo.thumbnailUrl ?? photo.url}
						alt=""
						class="carousel-img"
						loading="lazy"
						decoding="async"
					/>
				</div>
			{/each}
		</div>

		{#if total > 1}
			{#if hasPrev}
				<button
					class="carousel-arrow carousel-arrow--prev"
					onclick={prev}
					aria-label="Previous photo"
					type="button"
				>
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
						<polyline points="15 18 9 12 15 6" />
					</svg>
				</button>
			{/if}

			{#if hasNext}
				<button
					class="carousel-arrow carousel-arrow--next"
					onclick={next}
					aria-label="Next photo"
					type="button"
				>
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
						<polyline points="9 18 15 12 9 6" />
					</svg>
				</button>
			{/if}

			<div class="carousel-dots" role="tablist" aria-label="Photo navigation">
				{#each sorted as _, i (i)}
					<button
						class="carousel-dot"
						class:carousel-dot--active={i === currentIndex}
						role="tab"
						aria-selected={i === currentIndex}
						aria-label="Photo {i + 1} of {total}"
						onclick={() => goTo(i)}
						type="button"
					></button>
				{/each}
			</div>
		{/if}
	</div>
{/if}

<style>
	.carousel {
		position: relative;
		overflow: hidden;
		border-radius: 0.75rem;
		background: var(--color-surface-raised);
		aspect-ratio: 4 / 3;
	}

	.carousel-track {
		display: flex;
		height: 100%;
		transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
		will-change: transform;
	}

	.carousel-slide {
		flex: 0 0 100%;
		height: 100%;
	}

	.carousel-img {
		display: block;
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.carousel-arrow {
		position: absolute;
		top: 50%;
		transform: translateY(-50%);
		background: rgba(0, 0, 0, 0.45);
		color: #fff;
		border: none;
		border-radius: 50%;
		width: 2rem;
		height: 2rem;
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		transition: background-color 0.15s ease, opacity 0.15s ease;
		opacity: 0;
		z-index: 2;
	}

	.carousel-arrow svg {
		width: 1rem;
		height: 1rem;
	}

	.carousel:hover .carousel-arrow {
		opacity: 1;
	}

	.carousel-arrow:hover {
		background: rgba(0, 0, 0, 0.7);
	}

	.carousel-arrow--prev {
		left: 0.5rem;
	}

	.carousel-arrow--next {
		right: 0.5rem;
	}

	.carousel-dots {
		position: absolute;
		bottom: 0.625rem;
		left: 50%;
		transform: translateX(-50%);
		display: flex;
		gap: 0.375rem;
		z-index: 2;
	}

	.carousel-dot {
		width: 0.375rem;
		height: 0.375rem;
		border-radius: 50%;
		border: none;
		background: rgba(255, 255, 255, 0.5);
		cursor: pointer;
		padding: 0;
		transition: background-color 0.18s ease, transform 0.18s ease;
	}

	.carousel-dot--active {
		background: #fff;
		transform: scale(1.3);
	}
</style>
