<script lang="ts">
	import PostPhotoGallery from './PostPhotoGallery.svelte';
	import type { Post } from '$lib/types';

	interface Props {
		post: Post;
	}

	let { post }: Props = $props();
</script>

<div class="post-body">
	<p class="post-content">{post.content}</p>
</div>
{#if post.photos && post.photos.length > 0}
	<PostPhotoGallery photos={post.photos} postId={post.id} />
{/if}

<style>
	.post-body {
		padding: 0 16px 10px;
	}

	.post-content {
		margin: 0;
		font-size: 0.9375rem;
		line-height: 1.55;
		color: var(--color-text-primary);
		white-space: pre-wrap;
		word-break: break-word;
	}
</style>
