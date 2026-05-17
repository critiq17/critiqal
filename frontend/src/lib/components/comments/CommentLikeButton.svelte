<script lang="ts">
	import { LikeButton } from '$lib/ui';
	import { UseLike } from '$lib/features/posts/useLike.svelte';
	import { postService } from '$lib/services';
	import type { Comment } from '$lib/types';

	interface Props {
		comment: Comment;
		size?: number;
	}

	let { comment, size = 14 }: Props = $props();

	// One like state per comment/reply, wired to the comment-like endpoint.
	const like = new UseLike(
		() => postService.toggleCommentLike(comment.postId, comment.id),
		comment.likedByMe,
		comment.likeCount
	);
</script>

<LikeButton {like} {size} />
