<script lang="ts">
	import PostReactions from './PostReactions.svelte';
	import type { UseReactions } from '$lib/features/posts/useReactions.svelte';

	interface Props {
		reactions: UseReactions;
		viewCount: number;
		commentCount?: number;
		commentsExpanded?: boolean;
		oncommentstoggle?: () => void;
		onreactionshover?: () => void;
	}

	let {
		reactions,
		viewCount,
		commentCount = 0,
		commentsExpanded = false,
		oncommentstoggle,
		onreactionshover
	}: Props = $props();

	function formatViews(n: number): string {
		if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
		if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
		return String(n);
	}
</script>

<div class="post-footer">
	<div class="footer-left">
		<PostReactions {reactions} onhover={onreactionshover} />
	</div>

	<div class="footer-right">
		{#if oncommentstoggle}
			<button
				class="footer-btn"
				class:active={commentsExpanded}
				onclick={oncommentstoggle}
				aria-label="{commentCount} comments"
				aria-expanded={commentsExpanded}
			>
				<svg
					width="16"
					height="16"
					viewBox="0 0 24 24"
					fill="none"
					stroke="currentColor"
					stroke-width="2"
					aria-hidden="true"
				>
					<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z" />
				</svg>
				{#if commentCount > 0}<span class="count">{commentCount}</span>{/if}
			</button>
		{/if}

		<span class="view-count" aria-label="{viewCount} views">
			<svg
				width="14"
				height="14"
				viewBox="0 0 24 24"
				fill="none"
				stroke="currentColor"
				stroke-width="2"
				aria-hidden="true"
			>
				<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
				<circle cx="12" cy="12" r="3" />
			</svg>
			{formatViews(viewCount)}
		</span>
	</div>
</div>

<style>
	.post-footer {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 4px 12px 12px;
		border-top: 1px solid var(--color-border);
		margin-top: 4px;
	}

	.footer-left {
		flex: 1;
	}

	.footer-right {
		display: flex;
		align-items: center;
		gap: 8px;
	}

	.footer-btn {
		display: inline-flex;
		align-items: center;
		gap: 4px;
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-muted);
		font-size: 0.8rem;
		padding: 4px 8px;
		border-radius: var(--radius-md, 6px);
		transition: color 0.15s ease, background-color 0.15s ease;
	}

	.footer-btn:hover,
	.footer-btn.active {
		color: var(--color-text-primary);
		background: var(--color-surface-raised);
	}

	.count {
		font-weight: 500;
	}

	.view-count {
		display: flex;
		align-items: center;
		gap: 3px;
		font-size: 0.75rem;
		color: var(--color-text-muted);
		user-select: none;
	}
</style>
