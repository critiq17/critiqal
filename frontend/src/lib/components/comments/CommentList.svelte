<script lang="ts">
	import type { UseComments } from '$lib/features/posts/useComments.svelte';
	import CommentItem from './CommentItem.svelte';

	interface Props {
		comments: UseComments;
	}

	let { comments }: Props = $props();
</script>

{#if comments.loaded === false}
	<div class="comments-loading" aria-busy="true" aria-label="Loading comments">
		<span class="loading-dot"></span>
		<span class="loading-dot"></span>
		<span class="loading-dot"></span>
	</div>
{:else if comments.comments.length > 0}
	<ul class="comments-list">
		{#each comments.comments as comment (comment.id)}
			<CommentItem {comment} {comments} />
		{/each}
	</ul>
{:else}
	<p class="no-comments">No comments yet.</p>
{/if}

<style>
	.comments-loading {
		display: flex;
		gap: 0.25rem;
		align-items: center;
		padding: 0.5rem 0;
	}

	.loading-dot {
		width: 0.3125rem;
		height: 0.3125rem;
		border-radius: 50%;
		background: var(--color-text-muted);
		animation: blink 1.2s ease-in-out infinite;
	}

	.loading-dot:nth-child(2) {
		animation-delay: 0.2s;
	}

	.loading-dot:nth-child(3) {
		animation-delay: 0.4s;
	}

	@keyframes blink {
		0%,
		100% {
			opacity: 0.3;
		}
		50% {
			opacity: 1;
		}
	}

	.comments-list {
		list-style: none;
		padding: 0;
		margin: 0 0 0.875rem;
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}

	.no-comments {
		font-size: 0.875rem;
		color: var(--color-text-muted);
		margin: 0 0 0.875rem;
	}
</style>
