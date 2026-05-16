<script lang="ts">
	import { onMount, onDestroy, tick } from 'svelte';
	import type { PostPhoto } from '$lib/types';
	import { pushBackButton, hapticLight } from '$lib/tma/buttons';
	import { registerSheet } from '$lib/actions/registerSheet';

	interface Props {
		photos: PostPhoto[];
		startIndex?: number;
		onClose: () => void;
	}

	let { photos, startIndex = 0, onClose }: Props = $props();

	// Snapshot the initial start index — the lightbox owns navigation after mount.
	// svelte-ignore state_referenced_locally
	let currentIndex = $state(startIndex);
	let trackEl: HTMLDivElement | undefined = $state();
	let overlayEl: HTMLDivElement | undefined = $state();
	let zoom = $state(1);

	const total = $derived(photos.length);
	const hasPrev = $derived(currentIndex > 0);
	const hasNext = $derived(currentIndex < total - 1);

	let lastBodyOverflow = '';
	let cleanupBackButton: (() => void) | null = null;

	onMount(async () => {
		lastBodyOverflow = document.body.style.overflow;
		document.body.style.overflow = 'hidden';
		// Telegram Mini App: route hardware back / chevron through onClose,
		// preserving any back button that an outer overlay may have shown.
		cleanupBackButton = pushBackButton(onClose);
		await tick();
		// Position track on the requested photo without animation.
		if (trackEl) {
			trackEl.scrollLeft = trackEl.clientWidth * startIndex;
		}
		overlayEl?.focus();
	});

	onDestroy(() => {
		document.body.style.overflow = lastBodyOverflow;
		cleanupBackButton?.();
	});

	function syncIndexFromScroll(e: Event): void {
		const el = e.currentTarget as HTMLDivElement;
		const next = Math.round(el.scrollLeft / el.clientWidth);
		if (next !== currentIndex) {
			currentIndex = next;
			resetZoom();
			hapticLight();
		}
	}

	function goTo(index: number, smooth = true): void {
		if (!trackEl) return;
		const clamped = Math.max(0, Math.min(total - 1, index));
		trackEl.scrollTo({
			left: trackEl.clientWidth * clamped,
			behavior: smooth ? 'smooth' : 'auto',
		});
	}

	function prev(): void {
		if (hasPrev) goTo(currentIndex - 1);
	}

	function next(): void {
		if (hasNext) goTo(currentIndex + 1);
	}

	function resetZoom(): void {
		zoom = 1;
	}

	function onKey(e: KeyboardEvent): void {
		switch (e.key) {
			case 'Escape':
				onClose();
				break;
			case 'ArrowLeft':
				e.preventDefault();
				prev();
				break;
			case 'ArrowRight':
				e.preventDefault();
				next();
				break;
		}
	}

	function onOverlayClick(e: MouseEvent): void {
		// Close only when clicking the dark backdrop, not a child (image, button).
		if (e.target === e.currentTarget) {
			onClose();
		}
	}

	// Double-tap / double-click toggles 2× zoom for the active image.
	function onImageDoubleClick(): void {
		zoom = zoom > 1 ? 1 : 2;
	}

	const sorted = $derived([...photos].sort((a, b) => a.position - b.position));
</script>

<svelte:window onkeydown={onKey} />

<!-- svelte-ignore a11y_no_static_element_interactions -->
<!-- svelte-ignore a11y_click_events_have_key_events -->
<div
	class="lightbox"
	use:registerSheet
	role="dialog"
	aria-modal="true"
	aria-label="Photo viewer"
	tabindex="-1"
	bind:this={overlayEl}
	onclick={onOverlayClick}
>
	<button class="lightbox-close" type="button" aria-label="Close" onclick={onClose}>
		<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
			<line x1="6" y1="6" x2="18" y2="18" />
			<line x1="18" y1="6" x2="6" y2="18" />
		</svg>
	</button>

	{#if total > 1}
		<div class="lightbox-counter" aria-live="polite">{currentIndex + 1} / {total}</div>
	{/if}

	<div
		class="lightbox-track"
		bind:this={trackEl}
		onscroll={syncIndexFromScroll}
		role="region"
		aria-label="Photo carousel"
	>
		{#each sorted as photo, i (photo.id)}
			<div class="lightbox-slide">
				<!-- svelte-ignore a11y_click_events_have_key_events -->
				<img
					src={photo.url}
					alt=""
					class="lightbox-img"
					class:lightbox-img--zoomed={i === currentIndex && zoom > 1}
					style:--zoom={i === currentIndex ? zoom : 1}
					ondblclick={i === currentIndex ? onImageDoubleClick : undefined}
					decoding="async"
					loading={Math.abs(i - currentIndex) <= 1 ? 'eager' : 'lazy'}
					draggable="false"
				/>
			</div>
		{/each}
	</div>

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
		/* Prefer Telegram-reported viewport (fullscreen WebApp), then dvh, then vh. */
		height: var(--tg-viewport-height, 100dvh);
		min-height: 100dvh;
		background: rgba(0, 0, 0, 0.96);
		z-index: 1000;
		display: flex;
		align-items: center;
		justify-content: center;
		outline: none;
		/* Avoid OS pull-to-refresh / overscroll fighting our scroll-snap track. */
		overscroll-behavior: contain;
	}

	.lightbox-track {
		width: 100%;
		height: 100%;
		display: flex;
		overflow-x: auto;
		overflow-y: hidden;
		scroll-snap-type: x mandatory;
		scrollbar-width: none;
		-webkit-overflow-scrolling: touch;
		/* Explicit horizontal paging; no vertical gestures (close = header button). */
		touch-action: pan-x;
	}

	.lightbox-track::-webkit-scrollbar {
		display: none;
	}

	.lightbox-slide {
		flex: 0 0 100%;
		height: 100%;
		display: flex;
		align-items: center;
		justify-content: center;
		scroll-snap-align: center;
		scroll-snap-stop: always;
		padding: 1rem;
		/* Horizontal swipe between photos + pinch-zoom. */
		touch-action: pan-x pinch-zoom;
	}

	.lightbox-img {
		max-width: 100%;
		max-height: 100%;
		width: auto;
		height: auto;
		/* contain = full photo always visible, never cropped. */
		object-fit: contain;
		user-select: none;
		-webkit-user-drag: none;
		transform: scale(var(--zoom, 1));
		transition: transform 0.25s ease;
		cursor: zoom-in;
	}

	.lightbox-img--zoomed {
		cursor: zoom-out;
	}

	.lightbox-close {
		position: absolute;
		/* Full clearance (with the 44px floor) so it never hides under the
		   native Telegram Close / ⋯ buttons. */
		top: var(--tg-top-clearance);
		right: 0.75rem;
		width: 2.4rem;
		height: 2.4rem;
		border-radius: 9999px;
		background: var(--glass-bg-soft, rgba(255, 255, 255, 0.12));
		backdrop-filter: blur(calc(var(--glass-blur, 24px) + 8px)) saturate(var(--glass-saturate, 180%));
		-webkit-backdrop-filter: blur(calc(var(--glass-blur, 24px) + 8px)) saturate(var(--glass-saturate, 180%));
		border: 1px solid var(--glass-border, rgba(255, 255, 255, 0.12));
		box-shadow: inset 0 1px 0 var(--glass-highlight, rgba(255, 255, 255, 0.1));
		color: #fff;
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		z-index: 4;
		transition:
			transform 0.34s cubic-bezier(0.34, 1.56, 0.64, 1),
			background-color 0.15s ease;
	}

	.lightbox-close:active {
		transform: scale(0.88);
		transition-duration: 0.07s;
		background: rgba(255, 255, 255, 0.22);
	}

	.lightbox-close svg {
		width: 1.25rem;
		height: 1.25rem;
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
		transition: background-color 0.15s ease, opacity 0.15s ease;
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

	/* Hide arrows on touch devices — swipe is the primary affordance there. */
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
		.lightbox-img,
		.lightbox-dot,
		.lightbox-arrow,
		.lightbox-close {
			transition: none;
		}
		.lightbox-close:active {
			transform: none;
		}
	}
</style>
