<script lang="ts">
	import { ReactionIcon } from '$lib/ui';
	import { REACTION_TYPES } from '$lib/reactions';
	import type { UseReactions } from '$lib/features/posts/useReactions.svelte';
	import type { ReactionType } from '$lib/types';

	interface Props {
		reactions: UseReactions;
		onhover?: () => void;
	}

	let { reactions, onhover }: Props = $props();
</script>

<div class="reactions" role="group" aria-label="Reactions">
	{#each REACTION_TYPES as type (type)}
		{@const count = reactions.reactions[type] ?? 0}
		{@const isActive = reactions.myReaction === type}
		{@const isPopping = reactions.poppingType === type}
		<button
			class="reaction-btn"
			class:active={isActive}
			class:popping={isPopping}
			onclick={() => reactions.react(type as ReactionType)}
			onmouseenter={() => {
				onhover?.();
				if (!reactions.loaded) reactions.load();
			}}
			aria-label="{type} {count}"
			aria-pressed={isActive}
			disabled={reactions.reacting}
		>
			<ReactionIcon {type} size={24} active={isActive} />
			{#if count > 0}
				<span class="count">{count}</span>
			{/if}
		</button>
	{/each}
</div>

<style>
	.reactions {
		display: flex;
		align-items: center;
		gap: 4px;
	}

	.reaction-btn {
		display: inline-flex;
		align-items: center;
		gap: 4px;
		background: none;
		border: none;
		cursor: pointer;
		padding: 6px 8px;
		border-radius: var(--radius-md, 6px);
		color: var(--color-text-muted);
		transition:
			background 0.15s ease,
			transform 0.1s ease;
	}

	.reaction-btn:hover {
		background: var(--color-surface-raised);
	}

	.reaction-btn:disabled {
		cursor: default;
	}

	.reaction-btn:not(:disabled):active {
		transform: scale(0.92);
	}

	.reaction-btn.active {
		color: var(--color-accent);
	}

	.reaction-btn.popping {
		animation: pop 0.35s ease;
	}

	.count {
		font-size: 0.8rem;
		font-weight: 500;
	}

	@keyframes pop {
		0%,
		100% {
			transform: scale(1);
		}
		50% {
			transform: scale(1.3);
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.reaction-btn {
			transition: none;
		}

		.reaction-btn.popping {
			animation: none;
		}
	}
</style>
