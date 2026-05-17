<script lang="ts">
	interface Tab { value: string; label: string; }
	interface Props {
		tabs: Tab[];
		active: string;
		onchange: (value: string) => void;
	}
	let { tabs, active, onchange }: Props = $props();
</script>

<div class="tabs" role="tablist">
	{#each tabs as tab}
		<button
			role="tab"
			class="tab"
			class:active={active === tab.value}
			aria-selected={active === tab.value}
			onclick={() => onchange(tab.value)}
		>
			{tab.label}
		</button>
	{/each}
</div>

<style>
	.tabs {
		display: flex;
		gap: 2px;
		background: var(--color-surface-raised);
		border-radius: var(--radius-md);
		padding: 3px;
	}
	.tab {
		flex: 1;
		padding: 6px 12px;
		border: none;
		background: transparent;
		color: var(--color-text-muted);
		border-radius: calc(var(--radius-md) - 2px);
		font-size: 0.85rem;
		font-weight: 500;
		cursor: pointer;
		transition: all var(--transition-fast);
	}
	.tab.active {
		background: var(--color-surface);
		color: var(--color-text-primary);
		box-shadow: var(--shadow-sm);
	}
	@media (prefers-reduced-motion: reduce) { .tab { transition: none; } }
</style>
