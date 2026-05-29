<script lang="ts">
	import { tick } from 'svelte';
	import PostPhotoGallery from './PostPhotoGallery.svelte';
	import type { Post } from '$lib/types';

	interface Props {
		post: Post;
		onDoubleTapLike?: () => void;
	}

	let { post, onDoubleTapLike }: Props = $props();

	// Long posts collapse to a few lines with a "Show more" affordance, so the
	// feed stays scannable. We clamp via CSS, then measure whether the text
	// actually overflows the clamp before showing the toggle.
	const CLAMP_LINES = 8;
	let expanded = $state(false);
	let overflowing = $state(false);
	let textEl = $state<HTMLParagraphElement | null>(null);

	const content = $derived(post.content?.trim() ?? '');

	function measure(): void {
		const el = textEl;
		if (!el) return;
		// Tolerance absorbs sub-pixel line-height rounding.
		overflowing = el.scrollHeight - el.clientHeight > 4;
	}

	$effect(() => {
		// Re-measure when the text changes (collapsed state only).
		void content;
		if (expanded) return;
		let ro: ResizeObserver | null = null;
		tick().then(() => {
			measure();
			if (textEl && typeof ResizeObserver !== 'undefined') {
				ro = new ResizeObserver(() => measure());
				ro.observe(textEl);
			}
		});
		return () => ro?.disconnect();
	});

	function toggle(): void {
		expanded = !expanded;
	}
</script>

{#if content}
	<p
		class="p-text"
		class:clamped={!expanded && overflowing}
		style:--clamp-lines={CLAMP_LINES}
		bind:this={textEl}
	>{content}</p>
	{#if overflowing}
		<button class="p-more" type="button" onclick={toggle} aria-expanded={expanded}>
			{expanded ? 'Show less' : 'Show more'}
		</button>
	{/if}
{/if}
{#if post.photos && post.photos.length > 0}
	<div class="p-media-wrap">
		<PostPhotoGallery photos={post.photos} postId={post.id} {onDoubleTapLike} />
	</div>
{/if}

<style>
	.p-text {
		margin: 4px 0 12px;
		font-size: 15px;
		font-weight: 400;
		line-height: 1.55;
		color: var(--color-text-primary);
		white-space: pre-wrap;
		overflow-wrap: anywhere;
		text-wrap: pretty;
	}

	.p-text.clamped {
		display: -webkit-box;
		-webkit-box-orient: vertical;
		-webkit-line-clamp: var(--clamp-lines);
		line-clamp: var(--clamp-lines);
		overflow: hidden;
		/* Soft fade into the cut so it reads as "more below", not a hard crop. */
		-webkit-mask-image: linear-gradient(180deg, #000 70%, transparent 100%);
		mask-image: linear-gradient(180deg, #000 70%, transparent 100%);
		margin-bottom: 4px;
	}

	.p-more {
		display: inline-block;
		margin: 0 0 12px;
		padding: 0;
		background: none;
		border: 0;
		font: inherit;
		font-size: 13px;
		font-weight: 600;
		color: var(--color-text-secondary);
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
	}

	.p-more:active {
		opacity: 0.6;
	}

	.p-media-wrap {
		margin-bottom: 12px;
	}

	/* Soften the gallery corners to match the glass card radius. */
	.p-media-wrap :global(.gallery) {
		border-radius: 16px;
	}
</style>
