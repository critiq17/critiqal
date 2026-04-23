<script lang="ts">
	import { REACTION_VISUALS } from '$lib/reactions';
	import type { ReactionType } from '$lib/types';

	interface Props {
		type: ReactionType;
		size?: number;
		active?: boolean;
	}

	let { type, size = 28, active = false }: Props = $props();

	const visual = $derived(REACTION_VISUALS[type]);
</script>

<span
	class="reaction-icon"
	class:active
	style="font-size:{size * 0.9}px;width:{size}px;height:{size}px;"
	role="img"
	aria-label={visual?.label ?? type}
>
	{#if visual?.assetPath}
		<img src={visual.assetPath} alt={visual.label} width={size} height={size} loading="lazy" />
	{:else}
		{visual?.fallbackEmoji ?? '👍'}
	{/if}
</span>

<style>
	.reaction-icon {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		border-radius: var(--radius-full);
		transition: transform var(--transition-fast);
		flex-shrink: 0;
	}
	.reaction-icon img {
		width: 100%;
		height: 100%;
		object-fit: contain;
		border-radius: var(--radius-sm);
	}
	.reaction-icon.active { transform: scale(1.15); }
	@media (prefers-reduced-motion: reduce) { .reaction-icon { transition: none; } .reaction-icon.active { transform: none; } }
</style>
