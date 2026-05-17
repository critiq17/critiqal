<script lang="ts">
	interface Props {
		size?: number;
		label: string;
		disabled?: boolean;
		onclick?: (e: MouseEvent) => void;
		children: import('svelte').Snippet;
	}

	let { size = 36, label, disabled = false, onclick, children }: Props = $props();
</script>

<button
	class="icon-btn"
	style="width:{size}px;height:{size}px;"
	aria-label={label}
	{disabled}
	{onclick}
>
	{@render children()}
</button>

<style>
	.icon-btn {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		border: none;
		background: transparent;
		border-radius: var(--radius-full);
		cursor: pointer;
		color: var(--color-text-muted);
		transition:
			color var(--transition-fast),
			background var(--transition-fast),
			transform 0.34s cubic-bezier(0.34, 1.56, 0.64, 1);
		padding: 0;
		flex-shrink: 0;
	}
	.icon-btn:hover:not(:disabled) { color: var(--color-text-primary); background: var(--color-surface-raised); }
	.icon-btn:active:not(:disabled) { transform: scale(0.9); transition-duration: 0.07s; }
	.icon-btn:disabled { opacity: 0.4; cursor: not-allowed; }

	@media (prefers-reduced-motion: reduce) {
		.icon-btn { transition: color var(--transition-fast), background var(--transition-fast); }
		.icon-btn:active:not(:disabled) { transform: none; }
	}
</style>
