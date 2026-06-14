<script lang="ts">
	import { fade } from 'svelte/transition';
	import { LikeButton } from '$lib/ui';
	import { sharePost } from '$lib/utils/sharePost';
	import { hapticLight } from '$lib/tma/buttons';
	import type { UseLike } from '$lib/features/posts/useLike.svelte';
	import type { Post } from '$lib/types';
	import { t } from '$lib/i18n';

	interface Props {
		post: Post;
		like: UseLike;
		viewCount: number;
		commentCount?: number;
		commentsExpanded?: boolean;
		oncommentstoggle?: () => void;
	}

	let {
		post,
		like,
		viewCount,
		commentCount = 0,
		commentsExpanded = false,
		oncommentstoggle
	}: Props = $props();

	let copiedToast = $state(false);
	let copyTimer: ReturnType<typeof setTimeout> | null = null;

	function formatViews(n: number): string {
		if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
		if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
		return String(n);
	}

	async function handleShare(): Promise<void> {
		hapticLight();
		const { copied } = await sharePost(post);
		if (copied) {
			copiedToast = true;
			if (copyTimer) clearTimeout(copyTimer);
			copyTimer = setTimeout(() => {
				copiedToast = false;
			}, 1600);
		}
	}
</script>

<div class="p-actions">
	<LikeButton {like} size={16} burst />

	{#if oncommentstoggle}
		<button
			class="act"
			class:active={commentsExpanded}
			onclick={oncommentstoggle}
			aria-label="{commentCount} {t('post.comments')}"
			aria-expanded={commentsExpanded}
			type="button"
		>
			<svg
				class="ic"
				viewBox="0 0 24 24"
				fill="none"
				stroke="currentColor"
				stroke-width="1.7"
				stroke-linecap="round"
				stroke-linejoin="round"
				aria-hidden="true"
			>
				<path d="M21 11a8 8 0 0 1-8 8H7l-4 3v-9a8 8 0 0 1 8-8h2a8 8 0 0 1 8 6Z" />
			</svg>
			{#if commentCount > 0}<span class="count">{commentCount}</span>{/if}
		</button>
	{/if}

	<button class="act" onclick={handleShare} aria-label={t('post.share')} type="button">
		<svg
			class="ic"
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			stroke-width="1.7"
			stroke-linecap="round"
			stroke-linejoin="round"
			aria-hidden="true"
		>
			<path d="M4 12v8a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1v-8" />
			<path d="M16 6 12 2 8 6" />
			<path d="M12 2v14" />
		</svg>
	</button>

	<span class="act-spacer"></span>

	<span class="act act-views" aria-label={String(viewCount)}>
		<span class="count">{formatViews(viewCount)}</span>
		<svg
			class="ic"
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			stroke-width="1.7"
			stroke-linecap="round"
			stroke-linejoin="round"
			aria-hidden="true"
		>
			<path d="M2 12s4-7 10-7 10 7 10 7-4 7-10 7-10-7-10-7Z" />
			<circle cx="12" cy="12" r="3" />
		</svg>
	</span>
</div>

{#if copiedToast}
	<div class="copied-toast" role="status" aria-live="polite" transition:fade={{ duration: 160 }}>
		{t('post.shareCopied')}
	</div>
{/if}

<style>
	.p-actions {
		--spring: cubic-bezier(0.34, 1.56, 0.64, 1);
		--soft: cubic-bezier(0.2, 0.7, 0.2, 1);
		display: flex;
		align-items: center;
		gap: 2px;
		margin: 0 -6px;
		padding-top: 2px;
	}

	.act {
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
	}

	.act:hover {
		background: var(--surface-tint-subtle);
		color: var(--color-text-primary);
	}

	.act:active {
		transform: scale(0.86);
		transition: transform 80ms ease-out;
	}

	.act.active {
		color: var(--color-text-primary);
	}

	.act .ic {
		width: 16px;
		height: 16px;
		flex: none;
	}

	.count {
		font-variant-numeric: tabular-nums;
		font-size: 11.5px;
		font-weight: 500;
	}

	.act-spacer {
		flex: 1;
	}

	.act-views {
		color: var(--color-text-muted);
		opacity: 0.75;
		pointer-events: none;
	}

	.act-views:hover {
		background: transparent;
	}

	.copied-toast {
		position: fixed;
		bottom: calc(env(safe-area-inset-bottom, 0px) + 1.5rem);
		left: 50%;
		transform: translateX(-50%);
		padding: 0.5rem 0.875rem;
		border-radius: 9999px;
		background: rgba(20, 20, 20, 0.92);
		color: #fff;
		font-size: 0.8125rem;
		font-weight: 500;
		box-shadow: 0 4px 18px rgba(0, 0, 0, 0.32);
		z-index: 60;
		pointer-events: none;
	}

	@media (prefers-reduced-motion: reduce) {
		.act {
			transition: none;
		}
	}
</style>
