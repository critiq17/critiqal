<script lang="ts">
	import PostPhotoGallery from './PostPhotoGallery.svelte';
	import type { Post } from '$lib/types';

	interface Props {
		post: Post;
		onDoubleTapLike?: () => void;
	}

	let { post, onDoubleTapLike }: Props = $props();
</script>

{#if post.content?.trim()}
	<p class="p-text">{post.content}</p>
{/if}
{#if post.photos && post.photos.length > 0}
	<div class="p-media-wrap">
		<PostPhotoGallery photos={post.photos} postId={post.id} {onDoubleTapLike} />
	</div>
{/if}

<style>
	.p-text {
		margin: 4px 0 12px;
		font-size: 14.5px;
		line-height: 1.5;
		color: var(--color-text-primary);
		letter-spacing: -0.003em;
		white-space: pre-wrap;
		overflow-wrap: anywhere;
		text-wrap: pretty;
	}

	.p-media-wrap {
		margin-bottom: 12px;
	}

	/* Soften the gallery corners to match the glass card radius. */
	.p-media-wrap :global(.gallery) {
		border-radius: 16px;
	}
</style>
