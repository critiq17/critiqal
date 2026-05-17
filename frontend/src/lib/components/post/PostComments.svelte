<script lang="ts">
	import { slide } from 'svelte/transition';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import type { UseComments } from '$lib/features/posts/useComments.svelte';
	import CommentList from '$lib/components/comments/CommentList.svelte';
	import CommentComposer from '$lib/components/comments/CommentComposer.svelte';

	interface Props {
		comments: UseComments;
	}

	let { comments }: Props = $props();
</script>

<div class="comments-panel" role="region" aria-label="Comments" transition:slide={{ duration: 200 }}>
	<CommentList {comments} />

	{#if authStore.isAuthenticated}
		<CommentComposer
			value={comments.newComment}
			submitting={comments.submitting}
			onValueChange={(v) => { comments.newComment = v; }}
			onSubmit={() => comments.submit()}
		/>
	{/if}
</div>

<style>
	.comments-panel {
		padding: 0.875rem 0 0 3.25rem;
		overflow: hidden;
		border-top: 1px solid var(--color-border);
	}
</style>
