<script lang="ts">
	import type { UserBadge } from '$lib/types';
	import BadgePill from './BadgePill.svelte';

	interface Props {
		badges: UserBadge[];
		max?: number;
	}

	let { badges, max }: Props = $props();

	let shown = $derived(max ? badges.slice(0, max) : badges);
	let extra = $derived(max && badges.length > max ? badges.length - max : 0);
</script>

{#if badges.length > 0}
	<div class="badge-pill-row" role="list" aria-label="Badges">
		{#each shown as badge (badge.id)}
			<div role="listitem">
				<BadgePill {badge} />
			</div>
		{/each}
		{#if extra > 0}
			<span class="badge-pill-more">+{extra}</span>
		{/if}
	</div>
{/if}

<style>
	.badge-pill-row {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 6px;
		min-width: 0;
	}

	.badge-pill-more {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		min-width: 28px;
		height: 28px;
		padding: 0 8px;
		border-radius: var(--radius-full);
		border: 1px solid var(--glass-border);
		background: var(--glass-bg-soft);
		color: var(--color-text-muted);
		font-size: 0.76rem;
		font-weight: 650;
	}
</style>
