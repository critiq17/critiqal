<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { UseReactions } from '$lib/features/posts/useReactions.svelte';
	import { UseComments } from '$lib/features/posts/useComments.svelte';
	import { UsePostMutations } from '$lib/features/posts/usePostMutations.svelte';
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
	const reactions = new UseReactions(postId);
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
</script>

{#if !mutations.deleted}
	<article class="post" class:mobile={isMobile} bind:this={articleEl}>
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

		<PostBody {post} />

		<PostFooter
			{reactions}
			viewCount={post.viewCount}
			{commentCount}
			commentsExpanded={comments.expanded}
			oncommentstoggle={handleCommentsToggle}
			onreactionshover={() => reactions.load()}
		/>

		{#if !isMobile && comments.expanded}
			<PostComments {comments} />
		{/if}
	</article>
{/if}

<style>
	.post {
		border-bottom: 1px solid var(--color-border);
		padding: 1.25rem 0.5rem;
		margin: 0 -0.5rem;
		border-radius: 0.5rem;
		transition: background-color 0.15s ease;
	}

	.post:not(.mobile):hover {
		background-color: var(--color-surface-raised);
	}

	.post:last-child {
		border-bottom: none;
	}

	.options-wrapper {
		position: relative;
	}

	@media (prefers-reduced-motion: reduce) {
		.post {
			transition: none;
		}
	}
</style>
