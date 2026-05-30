<script lang="ts">
	import type { UserBadge } from '$lib/types';
	import { badgeMeta, tierStyle } from '$lib/badgeMeta';

	interface Props {
		badge: UserBadge;
	}

	let { badge }: Props = $props();

	const meta = $derived(badgeMeta(badge.code));
	const style = $derived(tierStyle(meta.tier));
</script>

<span
	class="pill"
	style:--badge-accent={style.accent}
	title={badge.name}
	aria-label={badge.name}
>
	{badge.name}
</span>

<style>
	.pill {
		--badge-accent: var(--color-accent);
		display: inline-flex;
		align-items: center;
		height: 22px;
		padding: 0 8px;
		border-radius: var(--radius-full);
		border: 1px solid color-mix(in srgb, var(--badge-accent) 40%, var(--color-border));
		background: color-mix(in srgb, var(--badge-accent) 10%, var(--color-surface));
		color: color-mix(in srgb, var(--badge-accent) 80%, var(--color-text-primary));
		font-size: 0.72rem;
		font-weight: 600;
		letter-spacing: 0.01em;
		white-space: nowrap;
	}
</style>
