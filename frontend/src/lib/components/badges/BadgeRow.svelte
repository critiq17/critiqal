<script lang="ts">
	import type { UserBadge } from '$lib/types';
	import Badge from './Badge.svelte';

	type Size = 'sm' | 'md' | 'lg';

	interface Props {
		badges: UserBadge[];
		size?: Size;
		// Cap the number of medallions shown; the rest collapse into a "+N".
		max?: number;
	}

	let { badges, size = 'md', max }: Props = $props();

	let shown = $derived(max ? badges.slice(0, max) : badges);
	let extra = $derived(max && badges.length > max ? badges.length - max : 0);
</script>

{#if badges.length > 0}
	<div class="badge-row" role="list" aria-label="Badges">
		{#each shown as badge (badge.id)}
			<div role="listitem">
				<Badge {badge} {size} />
			</div>
		{/each}
		{#if extra > 0}
			<span class="badge-row__more">+{extra}</span>
		{/if}
	</div>
{/if}

<style>
	.badge-row {
		display: flex;
		flex-wrap: wrap;
		align-items: center;
		gap: 8px;
	}

	.badge-row__more {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		min-width: 28px;
		height: 28px;
		padding: 0 8px;
		border-radius: var(--radius-full);
		border: 1px solid var(--color-border);
		color: var(--color-text-muted);
		font-size: 0.78rem;
		font-weight: 600;
	}
</style>
