<script lang="ts">
	import { untrack } from 'svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import { openSheet } from '$lib/stores/sheet.store.svelte';
	import { Sheet } from '$lib/ui';
	import { UseComments } from '$lib/features/posts/useComments.svelte';
	import CommentList from '$lib/components/comments/CommentList.svelte';
	import CommentComposer from '$lib/components/comments/CommentComposer.svelte';

	interface Props {
		postId: number;
		open: boolean;
		onClose: () => void;
	}

	let { postId, open, onClose }: Props = $props();

	let comments = $state<UseComments | null>(null);

	let _closeSheet: (() => void) | null = null;

	$effect(() => {
		const tg = getTelegramWebApp();
		if (open) {
			// Create instance in local var, load WITHOUT reading $state `comments`,
			// then assign — avoids the read-write cycle that causes infinite re-runs.
			const c = new UseComments(postId);
			untrack(() => c.load());
			comments = c;
			_closeSheet = openSheet();
			if (tg) {
				tg.BackButton.show();
				tg.BackButton.onClick(onClose);
			}
		} else {
			_closeSheet?.();
			_closeSheet = null;
			if (tg) {
				tg.BackButton.hide();
				tg.BackButton.offClick(onClose);
			}
		}
	});
</script>

<Sheet {open} onclose={onClose} title="Comments" maxHeight="70vh">
	<div class="sheet-content">
		<div class="comments-area">
			{#if comments}
				<CommentList {comments} />
			{/if}
		</div>

		{#if comments}
			<CommentComposer
				value={comments.newComment}
				submitting={comments.submitting}
				onValueChange={(v) => { if (comments) comments.newComment = v; }}
				onSubmit={() => comments?.submit()}
			/>
		{/if}
	</div>
</Sheet>

<style>
	.sheet-content {
		display: flex;
		flex-direction: column;
		height: 100%;
		min-height: 0;
	}

	.comments-area {
		flex: 1;
		overflow-y: auto;
		-webkit-overflow-scrolling: touch;
		padding: 4px 0;
	}
</style>
