<script lang="ts">
	import type { UseLike } from '$lib/features/posts/useLike.svelte';

	interface Props {
		like: UseLike;
		size?: number;
		showCount?: boolean;
		/** Emit a coral particle burst on like — used in the post action bar. */
		burst?: boolean;
	}

	let { like, size = 18, showCount = true, burst = false }: Props = $props();
</script>

<button
	class="like-btn"
	class:liked={like.liked}
	class:bump={like.popping}
	onclick={() => like.toggle()}
	disabled={like.pending}
	aria-pressed={like.liked}
	aria-label={like.liked ? 'Unlike' : 'Like'}
	type="button"
>
	<span class="heart-wrap" style:--s="{size}px">
		{#if like.liked}
			<svg
				class="ic heart-fill"
				width={size}
				height={size}
				viewBox="0 0 24 24"
				fill="currentColor"
				aria-hidden="true"
			>
				<path
					d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
				/>
			</svg>
		{:else}
			<svg
				class="ic heart-out"
				width={size}
				height={size}
				viewBox="0 0 24 24"
				fill="none"
				stroke="currentColor"
				stroke-width="1.8"
				stroke-linecap="round"
				stroke-linejoin="round"
				aria-hidden="true"
			>
				<path
					d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
				/>
			</svg>
		{/if}
		{#if burst}
			<span class="burst" class:go={like.popping && like.liked} aria-hidden="true">
				<i></i><i></i><i></i><i></i><i></i><i></i>
			</span>
		{/if}
	</span>
	{#if showCount && like.count > 0}
		<span class="count">{like.count}</span>
	{/if}
</button>

<style>
	.like-btn {
		--spring: cubic-bezier(0.34, 1.56, 0.64, 1);
		--soft: cubic-bezier(0.2, 0.7, 0.2, 1);
		--like-color: var(--color-accent, #e05252);
		display: inline-flex;
		align-items: center;
		gap: 6px;
		height: 30px;
		padding: 0 10px;
		border-radius: 999px;
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-muted);
		font-size: 12.5px;
		font-family: inherit;
		transition:
			background 180ms var(--soft),
			color 180ms var(--soft),
			transform 480ms var(--spring);
		will-change: transform;
	}

	.like-btn:hover {
		background: var(--surface-tint-subtle);
		color: var(--color-text-primary);
	}

	.like-btn:disabled {
		cursor: default;
	}

	.like-btn:not(:disabled):active {
		transform: scale(0.86);
		transition: transform 80ms ease-out;
	}

	.like-btn.liked {
		color: var(--like-color);
	}

	.heart-wrap {
		position: relative;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: var(--s, 18px);
		height: var(--s, 18px);
	}

	.ic {
		display: block;
	}

	.like-btn.bump .ic {
		animation: heartBump 560ms var(--spring);
	}

	@keyframes heartBump {
		0% {
			transform: scale(1);
		}
		18% {
			transform: scale(0.78);
		}
		40% {
			transform: scale(1.32);
		}
		65% {
			transform: scale(0.94);
		}
		100% {
			transform: scale(1);
		}
	}

	/* coral particle burst */
	.burst {
		position: absolute;
		left: 50%;
		top: 50%;
		width: 2px;
		height: 2px;
		pointer-events: none;
	}

	.burst i {
		position: absolute;
		left: 0;
		top: 0;
		width: 4px;
		height: 4px;
		border-radius: 999px;
		background: var(--like-color);
		transform: translate(-50%, -50%) scale(0);
		opacity: 0;
	}

	.burst.go i {
		animation: burst 700ms cubic-bezier(0.22, 1, 0.36, 1) forwards;
	}

	.burst i:nth-child(1) {
		--bx: 15px;
		--by: -13px;
		animation-delay: 0ms;
	}
	.burst i:nth-child(2) {
		--bx: -13px;
		--by: -11px;
		animation-delay: 30ms;
	}
	.burst i:nth-child(3) {
		--bx: 16px;
		--by: 9px;
		animation-delay: 20ms;
	}
	.burst i:nth-child(4) {
		--bx: -15px;
		--by: 9px;
		animation-delay: 50ms;
	}
	.burst i:nth-child(5) {
		--bx: 0px;
		--by: -16px;
		animation-delay: 10ms;
	}
	.burst i:nth-child(6) {
		--bx: 0px;
		--by: 15px;
		animation-delay: 60ms;
	}

	@keyframes burst {
		0% {
			transform: translate(-50%, -50%) scale(0);
			opacity: 0;
		}
		20% {
			opacity: 1;
			transform: translate(-50%, -50%) scale(1);
		}
		100% {
			transform: translate(calc(-50% + var(--bx)), calc(-50% + var(--by))) scale(0.4);
			opacity: 0;
		}
	}

	.count {
		font-weight: 500;
		font-variant-numeric: tabular-nums;
		min-width: 0.5ch;
	}

	@media (prefers-reduced-motion: reduce) {
		.like-btn,
		.ic {
			transition: none;
		}

		.like-btn.bump .ic,
		.burst.go i {
			animation: none;
		}
	}
</style>
