<script lang="ts">
	import { tick } from 'svelte';

	interface Props {
		open: boolean;
		onclose: () => void;
		title?: string;
		maxHeight?: string;
		children: import('svelte').Snippet;
	}

	let { open = $bindable(), onclose, title, maxHeight = '90vh', children }: Props = $props();

	let sheetEl = $state<HTMLDivElement | undefined>(undefined);
	let startY = 0;
	let currentY = 0;
	let isDragging = false;
	let translateY = $state(0);

	$effect(() => {
		if (open) {
			translateY = 0;
			tick().then(() => sheetEl?.focus());
		}
	});

	function onpointerdown(e: PointerEvent) {
		startY = e.clientY;
		currentY = e.clientY;
		isDragging = true;
		sheetEl?.setPointerCapture(e.pointerId);
	}

	function onpointermove(e: PointerEvent) {
		if (!isDragging) return;
		currentY = e.clientY;
		const delta = currentY - startY;
		if (delta > 0) translateY = delta;
	}

	function onpointerup() {
		if (!isDragging) return;
		isDragging = false;
		const delta = currentY - startY;
		if (delta > 120) {
			onclose();
		} else {
			translateY = 0;
		}
	}

	function onkeydown(e: KeyboardEvent) {
		if (e.key === 'Escape') onclose();
	}

	function onbackdropclick() {
		onclose();
	}
</script>

{#if open}
	<!-- svelte-ignore a11y_click_events_have_key_events a11y_no_static_element_interactions -->
	<div class="backdrop" onclick={onbackdropclick}></div>
	<div
		bind:this={sheetEl}
		class="sheet"
		class:open
		style="transform: translateY({translateY}px); max-height: {maxHeight};"
		role="dialog"
		aria-modal="true"
		aria-label={title}
		tabindex="-1"
		onkeydown={onkeydown}
	>
		<div
			class="drag-area"
			onpointerdown={onpointerdown}
			onpointermove={onpointermove}
			onpointerup={onpointerup}
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
		z-index: 100;
		animation: fadeIn var(--transition-base) ease;
	}

	.sheet {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		background: var(--color-surface);
		border-radius: var(--radius-xl) var(--radius-xl) 0 0;
		z-index: 101;
		display: flex;
		flex-direction: column;
		overflow: hidden;
		transition: transform var(--transition-fast);
		outline: none;
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

	@keyframes fadeIn {
		from { opacity: 0; }
		to { opacity: 1; }
	}

	@media (prefers-reduced-motion: reduce) {
		.backdrop { animation: none; }
		.sheet { transition: none; }
	}
</style>
