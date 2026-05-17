<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import type { PostPhoto } from '$lib/types';
	import { pushBackButton, hapticLight } from '$lib/tma/buttons';
	import { registerSheet } from '$lib/actions/registerSheet';
	import { portal } from '$lib/actions/portal';

	interface Props {
		photos: PostPhoto[];
		startIndex?: number;
		onClose: () => void;
		/** Double-tap the photo to like the post (idempotent — set by the caller). */
		onDoubleTapLike?: () => void;
	}

	let { photos, startIndex = 0, onClose, onDoubleTapLike }: Props = $props();

	const sorted = $derived([...photos].sort((a, b) => a.position - b.position));
	const total = $derived(sorted.length);

	// svelte-ignore state_referenced_locally
	let currentIndex = $state(Math.max(0, Math.min(startIndex, photos.length - 1)));
	// Live horizontal drag offset in px (0 when not dragging).
	let dragDX = $state(0);
	let dragging = $state(false);
	// Bumped on every double-tap so the heart bloom re-mounts and replays.
	let bloomKey = $state(0);

	const hasPrev = $derived(currentIndex > 0);
	const hasNext = $derived(currentIndex < total - 1);

	let trackEl: HTMLDivElement | undefined = $state();
	let overlayEl: HTMLDivElement | undefined = $state();

	let lastBodyOverflow = '';
	let cleanupBackButton: (() => void) | null = null;

	// Pointer bookkeeping (not reactive — raw gesture math).
	let pointerId: number | null = null;
	let startX = 0;
	let startY = 0;
	let axisLocked: 'none' | 'x' | 'y' = 'none';
	let lastTapAt = 0;
	const DOUBLE_TAP_MS = 320;

	onMount(() => {
		lastBodyOverflow = document.body.style.overflow;
		document.body.style.overflow = 'hidden';
		// The ONLY way to close: the Telegram header back/chevron button.
		cleanupBackButton = pushBackButton(onClose);
		overlayEl?.focus();
	});

	onDestroy(() => {
		document.body.style.overflow = lastBodyOverflow;
		cleanupBackButton?.();
	});

	function goTo(index: number): void {
		const clamped = Math.max(0, Math.min(total - 1, index));
		if (clamped !== currentIndex) {
			currentIndex = clamped;
			hapticLight();
		}
	}

	const prev = (): void => goTo(currentIndex - 1);
	const next = (): void => goTo(currentIndex + 1);

	function onKey(e: KeyboardEvent): void {
		if (e.key === 'Escape') onClose();
		else if (e.key === 'ArrowLeft') {
			e.preventDefault();
			prev();
		} else if (e.key === 'ArrowRight') {
			e.preventDefault();
			next();
		}
	}

	function triggerLike(): void {
		bloomKey += 1;
		onDoubleTapLike?.();
	}

	// ── Gestures ────────────────────────────────────────────────────────────
	// The track owns every pointer gesture (touch-action: none) so neither the
	// browser nor Telegram can read a drag as scroll / close. A horizontal drag
	// pages photos; a short tap that repeats within 320 ms likes the post.
	// Vertical drags do nothing — closing is header-button only.

	function onPointerDown(e: PointerEvent): void {
		if (pointerId !== null) return;
		pointerId = e.pointerId;
		startX = e.clientX;
		startY = e.clientY;
		axisLocked = 'none';
		dragging = true;
		trackEl?.setPointerCapture(e.pointerId);
	}

	function onPointerMove(e: PointerEvent): void {
		if (e.pointerId !== pointerId) return;
		const dx = e.clientX - startX;
		const dy = e.clientY - startY;

		if (axisLocked === 'none') {
			if (Math.abs(dx) < 6 && Math.abs(dy) < 6) return;
			axisLocked = Math.abs(dx) > Math.abs(dy) ? 'x' : 'y';
		}
		if (axisLocked !== 'x' || total < 2) {
			dragDX = 0;
			return;
		}

		// Rubber-band when dragging past the first / last photo.
		const atEdge = (dx > 0 && !hasPrev) || (dx < 0 && !hasNext);
		dragDX = atEdge ? dx * 0.35 : dx;
	}

	function endDrag(e: PointerEvent): void {
		if (e.pointerId !== pointerId) return;
		try {
			trackEl?.releasePointerCapture(e.pointerId);
		} catch {
			/* pointer already released */
		}
		pointerId = null;
		dragging = false;

		const movedX = e.clientX - startX;
		const movedY = e.clientY - startY;

		if (axisLocked === 'x' && total > 1) {
			const width = trackEl?.clientWidth ?? 1;
			const threshold = Math.min(80, width * 0.18);
			if (dragDX <= -threshold) next();
			else if (dragDX >= threshold) prev();
		} else if (Math.abs(movedX) < 8 && Math.abs(movedY) < 8) {
			// A tap, not a drag → detect double-tap to like.
			const now = performance.now();
			if (now - lastTapAt < DOUBLE_TAP_MS) {
				triggerLike();
				lastTapAt = 0;
			} else {
				lastTapAt = now;
			}
		}

		dragDX = 0;
		axisLocked = 'none';
	}
</script>

<svelte:window onkeydown={onKey} />

<!-- svelte-ignore a11y_no_static_element_interactions -->
<div
	class="lightbox"
	use:portal
	use:registerSheet
	role="dialog"
	aria-modal="true"
	aria-label="Photo viewer"
	tabindex="-1"
	bind:this={overlayEl}
>
	{#if total > 1}
		<div class="lightbox-counter" aria-live="polite">{currentIndex + 1} / {total}</div>
	{/if}

	<div
		class="lightbox-track"
		class:dragging
		bind:this={trackEl}
		style:transform="translate3d(calc({-currentIndex * 100}% + {dragDX}px), 0, 0)"
		onpointerdown={onPointerDown}
		onpointermove={onPointerMove}
		onpointerup={endDrag}
		onpointercancel={endDrag}
		role="region"
		aria-label="Photo carousel"
	>
		{#each sorted as photo, i (photo.id)}
			<div class="lightbox-slide">
				<img
					src={photo.url}
					alt=""
					class="lightbox-img"
					decoding="async"
					loading={Math.abs(i - currentIndex) <= 1 ? 'eager' : 'lazy'}
					draggable="false"
				/>
			</div>
		{/each}
	</div>

	{#key bloomKey}
		{#if bloomKey > 0}
			<div class="lightbox-bloom" aria-hidden="true">
				<svg viewBox="0 0 24 24" fill="currentColor" width="104" height="104">
					<path
						d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
					/>
				</svg>
			</div>
		{/if}
	{/key}

	{#if total > 1}
		{#if hasPrev}
			<button
				class="lightbox-arrow lightbox-arrow--prev"
				type="button"
				aria-label="Previous photo"
				onclick={prev}
			>
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
					<polyline points="15 18 9 12 15 6" />
				</svg>
			</button>
		{/if}
		{#if hasNext}
			<button
				class="lightbox-arrow lightbox-arrow--next"
				type="button"
				aria-label="Next photo"
				onclick={next}
			>
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
					<polyline points="9 18 15 12 9 6" />
				</svg>
			</button>
		{/if}

		<div class="lightbox-dots" role="tablist" aria-label="Photo navigation">
			{#each sorted as photo, i (photo.id)}
				<button
					type="button"
					class="lightbox-dot"
					class:lightbox-dot--active={i === currentIndex}
					role="tab"
					aria-selected={i === currentIndex}
					aria-label="Photo {i + 1} of {total}"
					data-photo-id={photo.id}
					onclick={() => goTo(i)}
				></button>
			{/each}
		</div>
	{/if}
</div>

<style>
	.lightbox {
		position: fixed;
		inset: 0;
		width: 100vw;
		height: var(--tg-viewport-height, 100dvh);
		min-height: 100dvh;
		background: #000;
		z-index: 1000;
		overflow: hidden;
		outline: none;
	}

	.lightbox-track {
		display: flex;
		width: 100%;
		height: 100%;
		will-change: transform;
		/* Own every gesture: the browser/Telegram never see scroll or a
		   close-drag. Only our horizontal pager moves the track. */
		touch-action: none;
		transition: transform 0.34s cubic-bezier(0.22, 1, 0.36, 1);
	}

	.lightbox-track.dragging {
		transition: none;
		cursor: grabbing;
	}

	.lightbox-slide {
		flex: 0 0 100%;
		width: 100%;
		height: 100%;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 1rem;
	}

	.lightbox-img {
		max-width: 100%;
		max-height: 100%;
		width: auto;
		height: auto;
		object-fit: contain;
		user-select: none;
		-webkit-user-drag: none;
		pointer-events: none;
	}

	/* Big heart that blooms on double-tap, mirroring the in-feed like. */
	.lightbox-bloom {
		position: absolute;
		left: 50%;
		top: 50%;
		width: 104px;
		height: 104px;
		transform: translate(-50%, -50%) scale(0);
		color: #fff;
		opacity: 0;
		pointer-events: none;
		z-index: 3;
		filter: drop-shadow(0 8px 22px rgba(0, 0, 0, 0.5));
		animation: lightboxBloom 900ms cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
	}

	.lightbox-bloom svg {
		display: block;
		width: 100%;
		height: 100%;
	}

	@keyframes lightboxBloom {
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

	.lightbox-counter {
		position: absolute;
		top: calc(var(--tg-top-clearance) + 0.4rem);
		left: 50%;
		transform: translateX(-50%);
		color: rgba(255, 255, 255, 0.85);
		font-size: 0.875rem;
		font-variant-numeric: tabular-nums;
		z-index: 2;
		pointer-events: none;
	}

	.lightbox-arrow {
		position: absolute;
		top: 50%;
		transform: translateY(-50%);
		width: 2.5rem;
		height: 2.5rem;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.12);
		color: #fff;
		border: none;
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		z-index: 2;
		transition: background-color 0.15s ease;
	}

	.lightbox-arrow:hover {
		background: rgba(255, 255, 255, 0.22);
	}

	.lightbox-arrow svg {
		width: 1.25rem;
		height: 1.25rem;
	}

	.lightbox-arrow--prev {
		left: 0.75rem;
	}

	.lightbox-arrow--next {
		right: 0.75rem;
	}

	/* Touch devices page by swipe — hide the arrows there. */
	@media (hover: none) {
		.lightbox-arrow {
			display: none;
		}
	}

	.lightbox-dots {
		position: absolute;
		bottom: calc(
			1rem +
				var(
					--tg-content-bottom,
					var(--tg-content-safe-area-inset-bottom, env(safe-area-inset-bottom, 0px))
				)
		);
		left: 50%;
		transform: translateX(-50%);
		display: flex;
		gap: 0.375rem;
		z-index: 2;
	}

	.lightbox-dot {
		width: 0.4375rem;
		height: 0.4375rem;
		border-radius: 50%;
		border: none;
		background: rgba(255, 255, 255, 0.4);
		cursor: pointer;
		padding: 0;
		transition: background-color 0.18s ease, transform 0.18s ease;
	}

	.lightbox-dot--active {
		background: #fff;
		transform: scale(1.3);
	}

	@media (prefers-reduced-motion: reduce) {
		.lightbox-track,
		.lightbox-dot {
			transition: none;
		}
		.lightbox-bloom {
			animation: none;
			opacity: 0;
		}
	}
</style>
