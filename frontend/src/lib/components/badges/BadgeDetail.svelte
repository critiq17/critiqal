<script lang="ts">
	import type { UserBadge } from '$lib/types';
	import { badgeMeta, tierStyle } from './badgeMeta';
	import BadgeMedallion from './BadgeMedallion.svelte';

	interface Props {
		badge: UserBadge;
	}

	let { badge }: Props = $props();

	let meta = $derived(badgeMeta(badge.code));
	let style = $derived(tierStyle(meta.tier));
	let awarded = $derived(formatAwarded(badge.awardedAt));

	function formatAwarded(iso: string): string {
		const d = new Date(iso);
		if (Number.isNaN(d.getTime())) return '';
		return d.toLocaleDateString(undefined, { year: 'numeric', month: 'long', day: 'numeric' });
	}
</script>

<div class="badge-detail">
	<BadgeMedallion code={badge.code} name={badge.name} size="lg" />
	<div class="badge-detail__head">
		<h3 class="badge-detail__name">{badge.name}</h3>
		<span class="badge-detail__tier" style:color={style.accent}>{style.label}</span>
	</div>
	{#if badge.description}
		<p class="badge-detail__desc">{badge.description}</p>
	{/if}
	{#if awarded}
		<p class="badge-detail__date">Earned {awarded}</p>
	{/if}
</div>

<style>
	.badge-detail {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 10px;
		width: min(100%, 280px);
		box-sizing: border-box;
		margin: 0 auto;
		text-align: center;
		padding: 6px 12px 12px;
	}

	.badge-detail__head {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 2px;
	}

	.badge-detail__name {
		margin: 0;
		font-size: 1.05rem;
		font-weight: 700;
		color: var(--color-text-primary);
	}

	.badge-detail__tier {
		font-size: 0.72rem;
		font-weight: 700;
		letter-spacing: 0.08em;
		text-transform: uppercase;
	}

	.badge-detail__desc {
		margin: 0;
		font-size: 0.86rem;
		line-height: 1.4;
		color: var(--color-text-secondary, var(--color-text-muted));
	}

	.badge-detail__date {
		margin: 0;
		font-size: 0.75rem;
		color: var(--color-text-muted);
	}

	@media (max-width: 640px) {
		.badge-detail {
			width: min(100%, 320px);
			gap: 12px;
			padding: 10px 20px calc(22px + env(safe-area-inset-bottom));
		}

		.badge-detail__name {
			font-size: 1.1rem;
		}

		.badge-detail__desc {
			max-width: 100%;
			font-size: 0.9rem;
		}
	}
</style>
