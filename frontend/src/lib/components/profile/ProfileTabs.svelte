<script lang="ts">
	import { formatCount } from '$lib/utils/formatCount';

	export interface ProfileTab {
		readonly id: string;
		readonly label: string;
		readonly count: number | null;
	}

	interface Props {
		tabs: readonly ProfileTab[];
		activeId: string;
		onSelect?: (id: string) => void;
	}

	let { tabs, activeId, onSelect }: Props = $props();

	function handleClick(id: string): void {
		if (id !== activeId) onSelect?.(id);
	}
</script>

<div class="tabs" role="tablist" aria-label="Profile sections">
	{#each tabs as tab (tab.id)}
		<button
			type="button"
			role="tab"
			class="tab"
			class:tab--active={tab.id === activeId}
			aria-selected={tab.id === activeId}
			aria-controls={`profile-tabpanel-${tab.id}`}
			id={`profile-tab-${tab.id}`}
			tabindex={tab.id === activeId ? 0 : -1}
			onclick={() => handleClick(tab.id)}
		>
			<span class="label">{tab.label}</span>
			<span class="count">{formatCount(tab.count)}</span>
		</button>
	{/each}
</div>

<style>
	.tabs {
		display: flex;
		gap: 0.25rem;
		border-bottom: 1px solid rgba(255, 255, 255, 0.05);
		padding: 0;
	}

	.tab {
		position: relative;
		display: inline-flex;
		align-items: baseline;
		gap: 0.375rem;
		padding: 0.625rem 0.875rem;
		background: none;
		border: none;
		color: var(--color-text-muted);
		font-family: inherit;
		font-size: 0.9375rem;
		font-weight: 600;
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
		transition: color 0.15s ease;
	}

	.tab:hover {
		color: var(--color-text-primary);
	}

	.tab--active {
		color: var(--color-text-primary);
	}

	.tab--active::after {
		content: '';
		position: absolute;
		left: 0.5rem;
		right: 0.5rem;
		bottom: -1px;
		height: 2px;
		background: var(--color-text-primary);
		border-radius: 2px 2px 0 0;
	}

	.count {
		font-size: 0.8125rem;
		font-weight: 500;
		color: var(--color-text-muted);
		font-variant-numeric: tabular-nums;
	}

	.tab--active .count {
		color: var(--color-text-muted);
	}

	@media (prefers-reduced-motion: reduce) {
		.tab {
			transition: none;
		}
	}
</style>
