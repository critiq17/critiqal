<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { UseLike } from '$lib/features/posts/useLike.svelte';
	import { UseComments } from '$lib/features/posts/useComments.svelte';
	import { UsePostMutations } from '$lib/features/posts/usePostMutations.svelte';
	import { postService } from '$lib/services';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { mobilePostFocus } from '$lib/stores/mobile-post-focus.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
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
		// When true, comments render inline instead of opening the mobile sheet,
		// and long-press focus is disabled (this IS the focused instance).
		focused?: boolean;
		onDeleted?: (id: string) => void;
		onAuthorClick?: (username: string) => void;
		onOpenComments?: (postId: string) => void;
	}

	let {
		post,
		variant = 'desktop',
		focused = false,
		onDeleted,
		onAuthorClick,
		onOpenComments
	}: Props = $props();

	const isMobile = $derived(variant === 'mobile');
	// Comments live inline on desktop, and on the focused mobile instance.
	const commentsInline = $derived(!isMobile || focused);
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
		clearPressTimer();
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
		if (commentsInline) {
			comments.toggle();
		} else {
			onOpenComments?.(post.id);
		}
	}

	// ── Long-press → focus (Telegram-style) ──────────────────────────────
	// A press held past LONG_PRESS_MS that hasn't turned into a scroll lifts
	// this post above a blurred feed. Disabled on the focused instance and
	// on desktop. Movement beyond MOVE_TOLERANCE_PX = the user is scrolling,
	// so we bail and let the gesture through untouched.
	const LONG_PRESS_MS = 460;
	const MOVE_TOLERANCE_PX = 12;
	let pressTimer: ReturnType<typeof setTimeout> | null = null;
	let pressStart: { x: number; y: number } | null = null;
	let pressFired = false;
	const longPressEnabled = $derived(isMobile && !focused);

	function clearPressTimer(): void {
		if (pressTimer !== null) {
			clearTimeout(pressTimer);
			pressTimer = null;
		}
	}

	function onPressStart(e: TouchEvent): void {
		if (!longPressEnabled || e.touches.length !== 1) return;
		const t = e.touches[0];
		if (!t) return;
		pressFired = false;
		pressStart = { x: t.clientX, y: t.clientY };
		clearPressTimer();
		pressTimer = setTimeout(() => {
			pressFired = true;
			pressTimer = null;
			getTelegramWebApp()?.HapticFeedback.impactOccurred('medium');
			mobilePostFocus.open(post);
		}, LONG_PRESS_MS);
	}

	function onPressMove(e: TouchEvent): void {
		if (pressTimer === null || !pressStart) return;
		const t = e.touches[0];
		if (!t) return;
		const moved =
			Math.abs(t.clientX - pressStart.x) > MOVE_TOLERANCE_PX ||
			Math.abs(t.clientY - pressStart.y) > MOVE_TOLERANCE_PX;
		if (moved) clearPressTimer();
	}

	function onPressEnd(): void {
		clearPressTimer();
		pressStart = null;
	}

	// Swallow the click that rides along after a long-press fires so we don't
	// also open the lightbox / follow a link on release.
	function onClickCapture(e: MouseEvent): void {
		if (pressFired) {
			e.preventDefault();
			e.stopPropagation();
			pressFired = false;
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
		class:focused
		bind:this={articleEl}
		ontouchstart={onPressStart}
		ontouchmove={onPressMove}
		ontouchend={onPressEnd}
		ontouchcancel={onPressEnd}
		onclickcapture={onClickCapture}
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

			{#if commentsInline && comments.expanded}
				<PostComments {comments} />
			{/if}
		</div>
	</article>
{/if}

<style>
	/* Web: no card, no border — a post is just its content, with a soft
	   photo-colour halo behind it (.post-glow). Mini-app: same content,
	   but on a faint glass pane so posts don't visually merge in the
	   scroll feed (see .post.mobile). Content-driven, no divider lines. */
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

	/* The photo-derived ambient halo is a single blur(34px) layer; harmless
	   on desktop but it stacks expensive paint over a scrolling list on
	   mid-range Android. Mobile posts already have their own glass-pane
	   separation below — the halo isn't load-bearing there. */
	.post.mobile .post-glow {
		display: none;
	}

	/* Mini-app: each post sits on a barely-there glass pane. No border,
	   no divider line — just a faint top-down tint, a 1px top-edge light
	   cue, and a soft shadow that lifts it ~1px off the #0c0c0c base.
	   Individually almost invisible; together they make each post read
	   as its own surface. No backdrop-filter: this list scrolls, so the
	   separation is pure paint (cheap on the GPU). */
	.post.mobile {
		border-radius: 18px;
		padding: 14px 14px 10px;
		margin-bottom: 8px;
		background: linear-gradient(
			180deg,
			var(--surface-tint-faint),
			var(--surface-tint-faint)
		);
		box-shadow:
			inset 0 1px 0 var(--surface-tint-faint),
			0 1px 2px rgba(0, 0, 0, 0.22);
		transition:
			transform 380ms var(--spring),
			background 140ms var(--soft);
	}

	/* Press feedback for touch: the pane brightens a touch instead of a
	   harsh highlight, keeping the borderless feel. */
	.post.mobile:active {
		background: linear-gradient(
			180deg,
			var(--surface-tint-subtle),
			var(--surface-tint-faint)
		);
	}

	/* Suppress the OS text-selection / callout bubble so a long-press reads
	   as "focus this post", not "select text". */
	.post.mobile {
		-webkit-touch-callout: none;
		-webkit-user-select: none;
		user-select: none;
	}

	/* The lifted instance inside the focus overlay. The backdrop already
	   blurs the whole feed behind it, so the card itself is a SOLID
	   surface — no second backdrop-filter. Stacking blur-on-blur inside
	   the scrolling comment list is what made loading lag; this keeps it
	   to a single GPU blur layer. */
	.post.mobile.focused {
		margin-bottom: 0;
		border-radius: 22px;
		padding: 16px 16px 12px;
		background: var(--color-surface-raised);
		box-shadow:
			inset 0 1px 0 var(--glass-highlight),
			0 24px 64px rgba(0, 0, 0, 0.6);
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
