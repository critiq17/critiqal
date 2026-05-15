<script lang="ts">
	import type { PostPhoto } from '$lib/types';
	import PhotoLightbox from '$lib/components/photo/PhotoLightbox.svelte';
	import {
		DEFAULT_RATIO,
		clampFeedRatio,
		naturalRatio,
		toAspectRatioCss,
	} from '$lib/utils/aspect';

	interface Props {
		photos: PostPhoto[];
		postId: string;
	}

	let { photos, postId }: Props = $props();

	const sorted = $derived([...photos].sort((a, b) => a.position - b.position));
	const total = $derived(sorted.length);

	// Container ratio is locked to the first photo's natural ratio (clamped) so
	// the whole carousel has a consistent height; other photos use contain.
	let containerRatio = $state(DEFAULT_RATIO);
	let ratioMeasured = $state(false);

	let scrollIndex = $state(0);
	let lightboxOpen = $state(false);
	let lightboxStartIndex = $state(0);

	function onFirstImgLoad(e: Event): void {
		if (ratioMeasured) return;
		containerRatio = clampFeedRatio(naturalRatio(e.currentTarget as HTMLImageElement));
		ratioMeasured = true;
	}

	function onStripScroll(e: Event): void {
		const el = e.currentTarget as HTMLDivElement;
		scrollIndex = Math.round(el.scrollLeft / el.clientWidth);
	}

	function openAt(index: number): void {
		lightboxStartIndex = index;
		lightboxOpen = true;
	}

	function onPhotoKey(e: KeyboardEvent, index: number): void {
		if (e.key === 'Enter' || e.key === ' ') {
			e.preventDefault();
			openAt(index);
		}
	}
</script>

{#if total > 0}
	<div
		class="gallery"
		style:aspect-ratio={toAspectRatioCss(containerRatio)}
	>
		<div
			class="photo-strip"
			role="region"
			aria-label="Post photos for post {postId}"
			onscroll={onStripScroll}
		>
			{#each sorted as photo, i (photo.id)}
				<!-- svelte-ignore a11y_no_static_element_interactions -->
				<div
					class="photo-slide"
					role="button"
					tabindex="0"
					aria-label="Open photo {i + 1} of {total}"
					onclick={() => openAt(i)}
					onkeydown={(e) => onPhotoKey(e, i)}
				>
					<img
						src={photo.url}
						alt=""
						class="photo-img"
						loading={i === 0 ? 'eager' : 'lazy'}
						decoding="async"
						draggable="false"
						onload={i === 0 ? onFirstImgLoad : undefined}
					/>
				</div>
			{/each}
		</div>

		{#if total > 1}
			<div class="dots" role="presentation">
				{#each sorted as photo, i (photo.id)}
					<div class="dot" class:dot--active={scrollIndex === i} data-photo-id={photo.id}></div>
				{/each}
			</div>

			<div class="counter" aria-hidden="true">{scrollIndex + 1}/{total}</div>
		{/if}
	</div>

	{#if lightboxOpen}
		<PhotoLightbox
			photos={sorted}
			startIndex={lightboxStartIndex}
			onClose={() => (lightboxOpen = false)}
		/>
	{/if}
{/if}

<style>
	.gallery {
		position: relative;
		width: 100%;
		/* aspect-ratio drives height; capped so very wide images don't dominate the feed. */
		max-height: min(70dvh, 640px);
		background: #000;
		border-radius: 0.75rem;
		overflow: hidden;
	}

	.photo-strip {
		display: flex;
		width: 100%;
		height: 100%;
		overflow-x: auto;
		overflow-y: hidden;
		scroll-snap-type: x mandatory;
		scroll-behavior: smooth;
		-webkit-overflow-scrolling: touch;
		scrollbar-width: none;
	}

	.photo-strip::-webkit-scrollbar {
		display: none;
	}

	.photo-slide {
		flex: 0 0 100%;
		height: 100%;
		scroll-snap-align: start;
		scroll-snap-stop: always;
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: zoom-in;
		background: #000;
		outline: none;
	}

	.photo-slide:focus-visible {
		box-shadow: inset 0 0 0 2px var(--color-text-primary);
	}

	.photo-img {
		display: block;
		width: 100%;
		height: 100%;
		/* contain = entire photo visible, letterboxed against #000 background. */
		object-fit: contain;
		user-select: none;
		-webkit-user-drag: none;
	}

	.dots {
		position: absolute;
		bottom: 0.5rem;
		left: 50%;
		transform: translateX(-50%);
		display: flex;
		gap: 0.3125rem;
		padding: 0.25rem 0.5rem;
		background: rgba(0, 0, 0, 0.32);
		border-radius: 999px;
		backdrop-filter: blur(4px);
		pointer-events: none;
	}

	.dot {
		width: 0.3125rem;
		height: 0.3125rem;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.55);
		transition: background-color 0.18s ease, width 0.18s ease;
	}

	.dot--active {
		background: #fff;
		width: 0.625rem;
		border-radius: 999px;
	}

	.counter {
		position: absolute;
		top: 0.5rem;
		right: 0.5rem;
		padding: 0.125rem 0.5rem;
		background: rgba(0, 0, 0, 0.45);
		color: #fff;
		font-size: 0.75rem;
		font-variant-numeric: tabular-nums;
		border-radius: 999px;
		pointer-events: none;
		backdrop-filter: blur(4px);
	}

	@media (prefers-reduced-motion: reduce) {
		.dot {
			transition: none;
		}

		.photo-strip {
			scroll-behavior: auto;
		}
	}
</style>
