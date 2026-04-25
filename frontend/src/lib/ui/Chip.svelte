<script lang="ts">
	interface Props {
		active?: boolean;
		onclick?: () => void;
		children: import('svelte').Snippet;
	}
	let { active = false, onclick, children }: Props = $props();
</script>

<button class="chip" class:active {onclick}>
	{@render children()}
</button>

<style>
	.chip {
		display: inline-flex;
		align-items: center;
		padding: 4px 12px;
		border-radius: var(--radius-full);
		border: 1px solid var(--color-border);
		background: transparent;
		color: var(--color-text-muted);
		font-size: 0.8rem;
		font-weight: 500;
		cursor: pointer;
		transition: all var(--transition-fast);
		white-space: nowrap;
	}
	.chip.active { background: var(--color-accent); color: #fff; border-color: var(--color-accent); }
	.chip:hover:not(.active) { border-color: var(--color-text-muted); color: var(--color-text-primary); }
</style>
