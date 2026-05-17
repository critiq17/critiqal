<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { UseLike } from '$lib/features/posts/useLike.svelte';
	import { UseComments } from '$lib/features/posts/useComments.svelte';
	import { UsePostMutations } from '$lib/features/posts/usePostMutations.svelte';
	import { postService } from '$lib/services';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { viewTracker } from '$lib/utils/viewTracker';
	import PostHeader from './PostHeader.svelte';
	import PostBody from './PostBody.svelte';
	import PostFooter from './PostFooter.svelte';
	import PostOptionsMenu from './PostOptionsMenu.svelte';
	import PostComments from './PostComments.svelte';
	import type { Post as PostType } from '$lib/types';

	export type PostVariant = 'desktop' | 'mobile';

	interface Props {
		post: PostType;
		variant?: PostVariant;
		onDeleted?: (id: string) => void;
		onAuthorClick?: (username: string) => void;
		onOpenComments?: (postId: string) => void;
	}

	let {
		post,
		variant = 'desktop',
		onDeleted,
		onAuthorClick,
		onOpenComments
	}: Props = $props();

	const isMobile = $derived(variant === 'mobile');
	const isOwnPost = $derived(authStore.user?.id === post.author.id);

	const postId = $derived(post.id);
	const like = new UseLike(
		() => postService.toggleLike(post.id),
		post.likedByMe,
		post.likeCount
	);
	const comments = new UseComments(postId);
	const mutations = new UsePostMutations(postId, () => onDeleted?.(postId));

	let showOptionsMenu = $state(false);
	let articleEl: HTMLElement | undefined = $state();
	let cleanupViewTracker: (() => void) | null = null;

	onMount(() => {
		if (!isMobile && articleEl) {
			cleanupViewTracker = viewTracker.observe(articleEl, post.id, authStore.isAuthenticated);
		}
	});

	onDestroy(() => {
		cleanupViewTracker?.();
	});

	function handleAuthorClick(): void {
		if (onAuthorClick) {
			onAuthorClick(post.author.username);
		} else {
			// Default: navigate via SvelteKit
			window.location.href = `/${post.author.username}`;
		}
	}

	// Double-tap on media force-likes (idempotent): an already-liked post just
	// replays the bloom, never unlikes — matches the handoff spec.
	function handleDoubleTapLike(): void {
		if (!like.liked) like.toggle();
	}

	function handleCommentsToggle(): void {
		if (isMobile) {
			onOpenComments?.(post.id);
		} else {
			comments.toggle();
		}
	}

	function handleOptionsClick(): void {
		showOptionsMenu = !showOptionsMenu;
	}

	const commentCount = $derived(comments.loaded ? comments.comments.length : 0);

	// First photo drives the soft colour halo behind a borderless post.
	const ambientUrl = $derived(
		post.photos && post.photos.length > 0
			? [...post.photos].sort((a, b) => a.position - b.position)[0]?.url
			: undefined
	);
</script>

{#if !mutations.deleted}
	<article
		class="post"
		class:mobile={isMobile}
		bind:this={articleEl}
	>
		{#if ambientUrl}
			<div
				class="post-glow"
				aria-hidden="true"
				style:background-image="url('{ambientUrl}')"
			></div>
		{/if}

		<div class="post-inner">
			<div class="options-wrapper">
				<PostHeader
					{post}
					showOptions={isOwnPost}
					{isOwnPost}
					onauthorclick={handleAuthorClick}
					onoptionsclick={handleOptionsClick}
				/>
				{#if showOptionsMenu}
					<PostOptionsMenu
						{isOwnPost}
						deleting={mutations.deleting}
						ondelete={() => {
							showOptionsMenu = false;
							mutations.deletePost();
						}}
						onclose={() => {
							showOptionsMenu = false;
						}}
					/>
				{/if}
			</div>

			<PostBody {post} onDoubleTapLike={handleDoubleTapLike} />

			<PostFooter
				{post}
				{like}
				viewCount={post.viewCount}
				{commentCount}
				commentsExpanded={comments.expanded}
				oncommentstoggle={handleCommentsToggle}
			/>

			{#if !isMobile && comments.expanded}
				<PostComments {comments} />
			{/if}
		</div>
	</article>
{/if}

<style>
	/* No card, no border, no surface. A post is just its content; when it has
	   a photo, a single soft halo of that photo's colour bleeds out behind it
	   (.post-glow). Minimal and content-driven. One shape for web + mini-app. */
	.post {
		--spring: cubic-bezier(0.34, 1.56, 0.64, 1);
		--soft: cubic-bezier(0.2, 0.7, 0.2, 1);
		position: relative;
		border-radius: 22px;
		padding: 16px 18px 12px;
		margin-bottom: 14px;
		background: transparent;
		border: 0;
		box-shadow: none;
		transition: transform 380ms var(--spring);
	}

	/* Dynamic light from the photo: one blurred copy of the post's own image,
	   bleeding ~24px past the content as a faint coloured halo. Single layer,
	   GPU-friendly (only filter/opacity), no scrim, no overdraw. */
	.post-glow {
		position: absolute;
		inset: 6px;
		z-index: 0;
		border-radius: inherit;
		background-size: cover;
		background-position: center;
		filter: blur(34px) saturate(160%) brightness(0.55);
		opacity: 0.18;
		pointer-events: none;
		transform: translateZ(0);
		transition: opacity 220ms var(--soft);
	}

	.post-inner {
		position: relative;
		z-index: 1;
	}

	.post:not(.mobile):hover {
		transform: translateY(-1px);
	}

	.post:not(.mobile):hover .post-glow {
		opacity: 0.24;
	}

	.post.mobile {
		border-radius: 20px;
		padding: 14px 14px 10px;
		margin-bottom: 10px;
	}

	.post:last-child {
		margin-bottom: 0;
	}

	.options-wrapper {
		position: relative;
	}

	@media (prefers-reduced-motion: reduce) {
		.post {
			transition: none;
		}

		.post:not(.mobile):hover {
			transform: none;
		}
	}
</style>
