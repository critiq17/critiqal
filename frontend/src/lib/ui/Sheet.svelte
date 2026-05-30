<script lang="ts">
	import { tick } from 'svelte';
	import { cubicIn, backOut } from 'svelte/easing';
	import type { TransitionConfig } from 'svelte/transition';
	import { elasticDrag } from '$lib/actions/elasticDrag';
	import { reducedMotion } from '$lib/ui/reducedMotion.svelte';

	interface Props {
		open: boolean;
		onclose: () => void;
		title?: string;
		maxHeight?: string;
		children: import('svelte').Snippet;
	}

	let { open = $bindable(), onclose, title, maxHeight = '90vh', children }: Props = $props();

	let sheetEl = $state<HTMLDivElement | undefined>(undefined);
	let backdropEl = $state<HTMLDivElement | undefined>(undefined);

	$effect(() => {
		if (open) tick().then(() => sheetEl?.focus());
	});

	// Sheet entry: spring slide-up from below.
	function sheetIn(_node: HTMLElement): TransitionConfig {
		if (reducedMotion.value) return { duration: 0 };
		return {
			duration: 440,
			easing: backOut,
			css: (t, u) => `transform: translateY(${u * 100}%); opacity: ${Math.min(t * 3, 1)}`,
		};
	}

	// Sheet exit: reads current drag offset and slides the rest of the way out.
	function sheetOut(node: HTMLElement): TransitionConfig {
		if (reducedMotion.value) return { duration: 0 };
		const matrix = new DOMMatrix(window.getComputedStyle(node).transform);
		const startY = matrix.m42; // current drag offset in px
		const height = node.offsetHeight;
		const remaining = Math.max(height - startY, 60); // distance left to off-screen
		// Capture starting position to avoid flash on first tick
		node.style.transform = `translateY(${startY}px)`;
		return {
			duration: Math.round(280 * (remaining / height) + 60),
			easing: cubicIn,
			tick(t) {
				node.style.transform = `translateY(${startY + remaining * (1 - t)}px)`;
			},
		};
	}

	// Backdrop entry: simple fade.
	function backdropIn(_node: HTMLElement): TransitionConfig {
		if (reducedMotion.value) return { duration: 0 };
		return { duration: 240, css: (t) => `opacity: ${t}` };
	}

	// Backdrop exit: fade from current opacity (respects drag-dimmed state).
	function backdropOut(node: HTMLElement): TransitionConfig {
		if (reducedMotion.value) return { duration: 0 };
		const from = parseFloat(node.style.opacity || '1');
		return { duration: 220, easing: cubicIn, css: (t) => `opacity: ${t * from}` };
	}

	function onkeydown(e: KeyboardEvent) {
		if (e.key === 'Escape') onclose();
	}
</script>

{#if open}
	<!-- svelte-ignore a11y_click_events_have_key_events a11y_no_static_element_interactions -->
	<div
		class="backdrop"
		bind:this={backdropEl}
		in:backdropIn
		out:backdropOut
		onclick={onclose}
	></div>
	<div
		bind:this={sheetEl}
		class="sheet glass"
		style:max-height={maxHeight}
		role="dialog"
		aria-modal="true"
		aria-label={title}
		tabindex="-1"
		onkeydown={onkeydown}
		in:sheetIn
		out:sheetOut
	>
		<div
			class="drag-area"
			use:elasticDrag={{
				target: () => sheetEl,
				dismissDistance: 120,
				dismissVelocity: 0.5,
				onDismiss: onclose,
				onProgress: (p) => {
					if (!backdropEl) return;
					if (p <= 0) {
						backdropEl.style.removeProperty('opacity');
					} else {
						backdropEl.style.opacity = String(1 - p);
					}
				},
				stretch: 0.04,
				stretchOrigin: 'bottom center',
			}}
		>
			<div class="drag-handle"></div>
			{#if title}
				<h2 class="sheet-title">{title}</h2>
			{/if}
		</div>
		<div class="sheet-content">
			{@render children()}
		</div>
	</div>
{/if}

<style>
	.backdrop {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.6);
		backdrop-filter: blur(4px);
		-webkit-backdrop-filter: blur(4px);
		z-index: 100;
		will-change: opacity;
	}

	.sheet {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		border-radius: var(--radius-xl) var(--radius-xl) 0 0;
		z-index: 101;
		display: flex;
		flex-direction: column;
		overflow: hidden;
		outline: none;
		will-change: transform;
	}

	.drag-area {
		padding: var(--spacing-sm) var(--spacing-md) var(--spacing-xs);
		display: flex;
		flex-direction: column;
		align-items: center;
		cursor: grab;
		user-select: none;
		touch-action: none;
	}

	.drag-handle {
		width: 36px;
		height: 4px;
		background: var(--color-border);
		border-radius: var(--radius-full);
		margin-bottom: var(--spacing-xs);
	}

	.sheet-title {
		font-size: 0.9rem;
		font-weight: 600;
		color: var(--color-text-primary);
		margin: 0;
		padding-bottom: var(--spacing-xs);
	}

	.sheet-content {
		flex: 1;
		overflow-y: auto;
		overscroll-behavior: contain;
	}
</style>
