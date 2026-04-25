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
		transition: color var(--transition-fast), background var(--transition-fast);
		padding: 0;
		flex-shrink: 0;
	}
	.icon-btn:hover:not(:disabled) { color: var(--color-text-primary); background: var(--color-surface-raised); }
	.icon-btn:disabled { opacity: 0.4; cursor: not-allowed; }
</style>
