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
		/**
		 * Called when the media is double-tapped. Kept as a plain callback so the
		 * gallery stays decoupled from the like state — the parent decides what a
		 * double-tap means (force-like, idempotent).
		 */
		onDoubleTapLike?: () => void;
	}

	let { photos, postId, onDoubleTapLike }: Props = $props();

	const DOUBLE_TAP_MS = 320;
	let lastTapAt = 0;
	// Suppress the single-tap lightbox open that rides along on a double-tap.
	let suppressClickUntil = 0;
	// Bumped on every double-tap so the bloom animation re-mounts and restarts.
	let bloomKey = $state(0);

	function onStripPointerDown(): void {
		const now = performance.now();
		if (now - lastTapAt < DOUBLE_TAP_MS) {
			bloomKey += 1;
			suppressClickUntil = now + 400;
			onDoubleTapLike?.();
			lastTapAt = 0;
		} else {
			lastTapAt = now;
		}
	}

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
		if (performance.now() < suppressClickUntil) return;
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
			onpointerdown={onStripPointerDown}
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

			<div class="page-pill" aria-hidden="true">{scrollIndex + 1}/{total}</div>
		{/if}

		{#key bloomKey}
			{#if bloomKey > 0}
				<div class="big-heart" aria-hidden="true">
					<svg viewBox="0 0 24 24" fill="currentColor" width="92" height="92">
						<path
							d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
						/>
					</svg>
				</div>
			{/if}
		{/key}
	</div>

	{#if lightboxOpen}
		<PhotoLightbox
			photos={sorted}
			startIndex={lightboxStartIndex}
			onClose={() => (lightboxOpen = false)}
			{onDoubleTapLike}
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
		/* touch-action left as default (auto): the browser routes a horizontal
		   swipe to this carousel and a vertical swipe to the page scroll. */
		overscroll-behavior-x: contain;
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
		bottom: 12px;
		left: 50%;
		transform: translateX(-50%);
		display: flex;
		align-items: center;
		gap: 5px;
		padding: 6px 8px;
		background: rgba(0, 0, 0, 0.4);
		border-radius: 999px;
		backdrop-filter: blur(10px);
		-webkit-backdrop-filter: blur(10px);
		box-shadow: inset 0 0 0 0.5px rgba(255, 255, 255, 0.14);
		pointer-events: none;
	}

	.dot {
		width: 5px;
		height: 5px;
		border-radius: 999px;
		background: rgba(255, 255, 255, 0.4);
		transition: all 380ms cubic-bezier(0.34, 1.56, 0.64, 1);
	}

	.dot--active {
		background: #fff;
		width: 16px;
	}

	/* page indicator pill — black glass, top-right (handoff spec) */
	.page-pill {
		position: absolute;
		top: 10px;
		right: 10px;
		height: 24px;
		padding: 0 10px;
		display: inline-flex;
		align-items: center;
		background: rgba(0, 0, 0, 0.42);
		color: #fff;
		font-size: 11.5px;
		font-variant-numeric: tabular-nums;
		letter-spacing: 0.02em;
		border-radius: 999px;
		pointer-events: none;
		backdrop-filter: blur(12px) saturate(140%);
		-webkit-backdrop-filter: blur(12px) saturate(140%);
		box-shadow: inset 0 0 0 0.5px rgba(255, 255, 255, 0.18);
	}

	/* big white heart that blooms over the photo on double-tap */
	.big-heart {
		position: absolute;
		left: 50%;
		top: 50%;
		width: 92px;
		height: 92px;
		transform: translate(-50%, -50%) scale(0);
		color: #fff;
		opacity: 0;
		pointer-events: none;
		filter: drop-shadow(0 8px 20px rgba(0, 0, 0, 0.4));
		z-index: 3;
		animation: bigHeart 900ms cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
	}

	.big-heart svg {
		display: block;
		width: 100%;
		height: 100%;
	}

	@keyframes bigHeart {
		0% {
			transform: translate(-50%, -50%) scale(0.2);
			opacity: 0;
		}
		25% {
			transform: translate(-50%, -50%) scale(1.15);
			opacity: 1;
		}
		55% {
			transform: translate(-50%, -50%) scale(0.95);
			opacity: 1;
		}
		100% {
			transform: translate(-50%, -50%) scale(1.4);
			opacity: 0;
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.dot {
			transition: none;
		}

		.photo-strip {
			scroll-behavior: auto;
		}

		.big-heart {
			animation: none;
			opacity: 0;
		}
	}
</style>
