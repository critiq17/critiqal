<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import type { Post, ReactionType, ReactionsMap } from '$lib/types';
	import { postService } from '$lib/services';
	import { mobileFeedStore } from '$lib/stores/mobile-feed.store';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import CommentSheet from './CommentSheet.svelte';
	import { openProfile } from '$lib/stores/profile-nav.store';

	// Feed state mirrored from store
	let posts = $state<Post[]>([]);
	let isLoading = $state(false);
	let isLoadingMore = $state(false);
	let page = $state(0);
	let feedError = $state<string | null>(null);

	// Per-post reaction state
	interface PostReactionState {
		reactions: ReactionsMap;
		myReaction: ReactionType | null;
		loaded: boolean;
		poppingType: ReactionType | null;
	}

	let reactionStates = $state<Map<number, PostReactionState>>(new Map());

	// Photo carousel dot tracking: postId -> active index
	let photoIndices = $state<Map<number, number>>(new Map());

	// Comment sheet
	let openCommentSheetPostId = $state<number | null>(null);

	// Pull-to-refresh
	let isPulling = $state(false);
	let pullY = $state(0);
	let pullThresholdMet = $state(false);
	let pullStartY = 0;
	let pullHapticFired = false;

	// Scroll container ref
	let containerEl = $state<HTMLElement | null>(null);

	// Sentinel for infinite scroll
	let sentinelEl = $state<HTMLElement | null>(null);
	let observer: IntersectionObserver | null = null;

	// Refresh indicator
	let isRefreshing = $state(false);

	const REACTION_IMGS: Record<ReactionType, string> = {
		GIGACHAD: '/assets/reactions/GIGACHAD.png',
		THE_ROCK: '/assets/reactions/THEROCK.png',
		DAVID: '/assets/reactions/DAVID.png'
	};

	const REACTION_TYPES: ReactionType[] = ['GIGACHAD', 'THE_ROCK', 'DAVID'];

	function formatRelativeTime(dateStr: string): string {
		const diff = Date.now() - new Date(dateStr).getTime();
		const minutes = Math.floor(diff / 60000);
		if (minutes < 1) return 'just now';
		if (minutes < 60) return `${minutes}m`;
		const hours = Math.floor(minutes / 60);
		if (hours < 24) return `${hours}h`;
		const days = Math.floor(hours / 24);
		if (days < 7) return `${days}d`;
		return new Date(dateStr).toLocaleDateString();
	}

	function getInitials(name: string | null, username: string): string {
		const src = name ?? username;
		return src.slice(0, 2).toUpperCase();
	}

	function getReactionState(postId: number): PostReactionState {
		return reactionStates.get(postId) ?? {
			reactions: { GIGACHAD: 0, THE_ROCK: 0, DAVID: 0 },
			myReaction: null,
			loaded: false,
			poppingType: null
		};
	}

	function setReactionState(postId: number, patch: Partial<PostReactionState>): void {
		const prev = getReactionState(postId);
		const next = new Map(reactionStates);
		next.set(postId, { ...prev, ...patch });
		reactionStates = next;
	}

	async function loadReactionsForPost(postId: number): Promise<void> {
		if (getReactionState(postId).loaded) return;
		try {
			const [reactions, myReaction] = await Promise.all([
				postService.getReactions(postId),
				postService.getMyReaction(postId).catch(() => undefined)
			]);
			setReactionState(postId, {
				reactions,
				myReaction: myReaction ?? null,
				loaded: true
			});
		} catch {
			// Non-critical: leave defaults
		}
	}

	async function handleReaction(post: Post, type: ReactionType): Promise<void> {
		const state = getReactionState(post.id);
		const already = state.myReaction === type;

		// Haptic
		getTelegramWebApp()?.HapticFeedback.impactOccurred('light');

		// Pop animation
		setReactionState(post.id, { poppingType: type });
		setTimeout(() => setReactionState(post.id, { poppingType: null }), 300);

		// Optimistic update
		const prev = { ...state.reactions };
		const newReactions = { ...prev };
		if (already) {
			newReactions[type] = Math.max(0, newReactions[type] - 1);
			setReactionState(post.id, { reactions: newReactions, myReaction: null });
		} else {
			if (state.myReaction) {
				newReactions[state.myReaction] = Math.max(0, newReactions[state.myReaction] - 1);
			}
			newReactions[type] = newReactions[type] + 1;
			setReactionState(post.id, { reactions: newReactions, myReaction: type });
		}

		try {
			if (already) {
				await postService.removeReaction(post.id);
			} else {
				await postService.react(post.id, type);
			}
		} catch {
			// Rollback
			setReactionState(post.id, { reactions: prev, myReaction: state.myReaction });
		}
	}

	function getPhotoIndex(postId: number): number {
		return photoIndices.get(postId) ?? 0;
	}

	function handlePhotoScroll(postId: number, e: Event): void {
		const el = e.currentTarget as HTMLElement;
		const index = Math.round(el.scrollLeft / el.clientWidth);
		const next = new Map(photoIndices);
		next.set(postId, index);
		photoIndices = next;
	}

	async function fetchFeed(resetPage = false): Promise<void> {
		isLoading = true;
		feedError = null;
		try {
			const fetched = await postService.getFeed();
			posts = fetched;
			mobileFeedStore.update((s) => ({
				...s,
				posts: fetched,
				loadedAt: Date.now(),
				page: 0,
				isLoading: false
			}));
			if (resetPage) page = 0;
			// Load reactions for all fetched posts
			fetched.forEach((p) => loadReactionsForPost(p.id));
		} catch (err) {
			feedError = err instanceof Error ? err.message : 'Failed to load feed';
		} finally {
			isLoading = false;
		}
	}

	async function loadMorePosts(): Promise<void> {
		if (isLoadingMore) return;
		isLoadingMore = true;
		try {
			const nextPage = page + 1;
			// postService.getFeed doesn't currently accept a page param;
			// if pagination is added later, pass nextPage here.
			// For now, simply mark as done to avoid infinite loop.
			page = nextPage;
			mobileFeedStore.update((s) => ({ ...s, page: nextPage }));
		} finally {
			isLoadingMore = false;
		}
	}

	// Pull-to-refresh touch handlers
	function onPullTouchStart(e: TouchEvent): void {
		const scrollTop = containerEl?.scrollTop ?? 0;
		if (scrollTop !== 0) return;
		pullStartY = e.touches[0].clientY;
		pullHapticFired = false;
	}

	function onPullTouchMove(e: TouchEvent): void {
		const scrollTop = containerEl?.scrollTop ?? 0;
		if (scrollTop !== 0) {
			isPulling = false;
			pullY = 0;
			return;
		}
		const delta = e.touches[0].clientY - pullStartY;
		if (delta <= 0) {
			isPulling = false;
			pullY = 0;
			return;
		}
		isPulling = true;
		// Rubber band: dampen the pull
		pullY = Math.min(80, delta * 0.5);
		const threshold = pullY >= 60;
		if (threshold && !pullHapticFired) {
			pullHapticFired = true;
			pullThresholdMet = true;
			getTelegramWebApp()?.HapticFeedback.impactOccurred('medium');
		} else if (!threshold) {
			pullThresholdMet = false;
		}
	}

	async function onPullTouchEnd(): Promise<void> {
		if (!isPulling) return;
		isPulling = false;
		pullY = 0;
		if (pullThresholdMet && !isRefreshing) {
			pullThresholdMet = false;
			isRefreshing = true;
			try {
				await fetchFeed(true);
			} finally {
				isRefreshing = false;
			}
		} else {
			pullThresholdMet = false;
		}
	}

	onMount(() => {
		// Subscriber only syncs local state — never triggers side-effects.
		// fetchFeed is called once below, guarded by the 30-second cache.
		let initialSync = true;
		let shouldFetch = false;

		const unsub = mobileFeedStore.subscribe((s) => {
			posts = s.posts;
			isLoading = s.isLoading;
			page = s.page;

			if (initialSync) {
				initialSync = false;
				shouldFetch = s.loadedAt === null || Date.now() - s.loadedAt >= 30000;
			}
		});

		if (shouldFetch) {
			fetchFeed();
		}

		// IntersectionObserver for infinite scroll
		if (sentinelEl) {
			observer = new IntersectionObserver(
				(entries) => {
					if (entries[0].isIntersecting && !isLoadingMore && posts.length > 0) {
						loadMorePosts();
					}
				},
				{ threshold: 0.1 }
			);
			observer.observe(sentinelEl);
		}

		return () => {
			unsub();
			observer?.disconnect();
		};
	});
</script>

<!-- Pull-to-refresh indicator -->
{#if isRefreshing || (isPulling && pullY > 20)}
	<div class="refresh-indicator" style="opacity: {isPulling ? pullY / 60 : 1}">
		<svg
			class="refresh-icon"
			class:spinning={isRefreshing}
			width="20"
			height="20"
			viewBox="0 0 24 24"
			fill="none"
			stroke="currentColor"
			stroke-width="2"
			stroke-linecap="round"
			stroke-linejoin="round"
		>
			<path d="M21 2v6h-6" />
			<path d="M3 12a9 9 0 0 1 15-6.7L21 8" />
			<path d="M3 22v-6h6" />
			<path d="M21 12a9 9 0 0 1-15 6.7L3 16" />
		</svg>
	</div>
{/if}

<div
	class="feed-container"
	bind:this={containerEl}
	ontouchstart={onPullTouchStart}
	ontouchmove={onPullTouchMove}
	ontouchend={onPullTouchEnd}
>
	{#if isLoading && posts.length === 0}
		<div class="feed-state">
			<div class="skeleton-card"></div>
			<div class="skeleton-card"></div>
			<div class="skeleton-card"></div>
		</div>
	{:else if feedError && posts.length === 0}
		<div class="feed-state error">
			<p>{feedError}</p>
			<button class="retry-btn" onclick={() => fetchFeed()}>Retry</button>
		</div>
	{:else}
		{#each posts as post (post.id)}
			<article class="post-card">
				<div class="post-card-inner">
					<!-- Author row -->
					<div class="author-row">
						<button
							class="author-tap"
							onclick={() => openProfile(post.author.username)}
							aria-label="View {post.author.username}'s profile"
						>
							<div class="author-avatar">
								{#if post.author.avatarUrl}
									<img
										src={post.author.avatarUrl}
										alt={post.author.username}
										class="avatar-img"
									/>
								{:else}
									<div class="avatar-initials">
										{getInitials(post.author.name, post.author.username)}
									</div>
								{/if}
							</div>
							<div class="author-info">
								<span class="author-display">
									{post.author.name ?? post.author.username}
								</span>
								<span class="author-username">@{post.author.username}</span>
							</div>
						</button>
						<span class="post-time">{formatRelativeTime(post.createdAt)}</span>
					</div>

					<!-- Content -->
					<p class="post-content">{post.content}</p>
				</div>

				<!-- Photos -->
				{#if post.photos && post.photos.length > 0}
					<div
						class="photo-strip"
						role="region"
						aria-label="Post photos"
						onscroll={(e) => handlePhotoScroll(post.id, e)}
					>
						{#each post.photos.sort((a, b) => a.position - b.position) as photo (photo.id)}
							<div class="photo-item">
								<img src={photo.url} alt="Post photo" loading="lazy" />
							</div>
						{/each}
					</div>

					{#if post.photos.length > 1}
						<div class="photo-dots" role="presentation">
							{#each post.photos as _photo, i (i)}
								<div
									class="photo-dot"
									class:active={getPhotoIndex(post.id) === i}
								></div>
							{/each}
						</div>
					{/if}
				{/if}

				<!-- Reaction + comment row -->
				<div class="post-card-inner action-row">
					<!-- Reactions -->
					{#each REACTION_TYPES as type (type)}
						{@const rs = getReactionState(post.id)}
						{@const isActive = rs.myReaction === type}
						{@const isPopping = rs.poppingType === type}
						<button
							class="reaction-btn"
							class:active={isActive}
							onclick={() => handleReaction(post, type)}
							aria-label="{type} reaction, count {rs.reactions[type]}"
							onmouseenter={() => { if (!rs.loaded) loadReactionsForPost(post.id); }}
						>
							<img
								src={REACTION_IMGS[type]}
								alt={type}
								class="reaction-img"
								class:reaction-popping={isPopping}
								loading="lazy"
							/>
							{#if rs.reactions[type] > 0}
								<span class="reaction-count">{rs.reactions[type]}</span>
							{/if}
						</button>
					{/each}

					<!-- Comment button -->
					<button
						class="reaction-btn comment-btn"
						onclick={() => {
							openCommentSheetPostId = post.id;
						}}
						aria-label="Comments"
					>
						<svg
							width="20"
							height="20"
							viewBox="0 0 24 24"
							fill="none"
							stroke="currentColor"
							stroke-width="2"
							stroke-linecap="round"
							stroke-linejoin="round"
						>
							<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"></path>
						</svg>
					</button>
				</div>
			</article>
		{/each}

		<!-- Infinite scroll sentinel -->
		<div class="scroll-sentinel" bind:this={sentinelEl}></div>

		{#if isLoadingMore}
			<div class="loading-more">Loading…</div>
		{/if}
	{/if}
</div>

<!-- Comment sheet -->
<CommentSheet
	postId={openCommentSheetPostId ?? 0}
	open={openCommentSheetPostId !== null}
	onClose={() => (openCommentSheetPostId = null)}
/>


<style>
	.feed-container {
		height: 100%;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
		/* In fullscreen mode --tg-content-top = transparent Telegram header height.
		   Pushes the compose prompt and feed below the transparent header overlay. */
		padding-top: var(--tg-content-top, var(--tg-content-safe-area-inset-top, env(safe-area-inset-top, 0px)));
		padding-bottom: var(--content-bottom-padding, 104px);
		position: relative;
	}

	/* Pull-to-refresh indicator */
	.refresh-indicator {
		position: absolute;
		top: 12px;
		left: 50%;
		transform: translateX(-50%);
		z-index: 10;
		pointer-events: none;
		color: var(--color-text-secondary, rgba(240, 240, 240, 0.6));
		transition: opacity 0.15s ease;
	}

	.refresh-icon {
		display: block;
	}

	@keyframes spinRefresh {
		from { transform: rotate(0deg); }
		to { transform: rotate(360deg); }
	}

	:global(.refresh-icon.spinning) {
		animation: spinRefresh 0.8s linear infinite;
	}

	/* Feed states */
	.feed-state {
		padding: 24px 16px;
		display: flex;
		flex-direction: column;
		gap: 12px;
	}

	.feed-state.error {
		align-items: center;
		color: #e05252;
		font-size: 14px;
	}

	.retry-btn {
		margin-top: 8px;
		padding: 8px 20px;
		border-radius: 20px;
		background: var(--tg-accent, #e05252);
		color: #fff;
		border: none;
		cursor: pointer;
		font-size: 14px;
	}

	.skeleton-card {
		height: 120px;
		border-radius: 12px;
		background: var(--color-surface-raised, #242424);
		animation: pulse 1.4s ease-in-out infinite;
	}

	@keyframes pulse {
		0%, 100% { opacity: 1; }
		50% { opacity: 0.4; }
	}

	/* Post card */
	.post-card {
		border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.08));
		padding: 0;
	}

	.post-card-inner {
		padding: 16px;
	}

	/* Author row */
	.author-row {
		display: flex;
		align-items: center;
		gap: 10px;
		margin-bottom: 4px;
	}

	.author-tap {
		display: flex;
		align-items: center;
		gap: 10px;
		flex: 1;
		min-width: 0;
		background: none;
		border: none;
		padding: 0;
		cursor: pointer;
		text-align: left;
		-webkit-tap-highlight-color: transparent;
	}

	.author-avatar {
		flex-shrink: 0;
	}

	.avatar-img {
		border-radius: 50%;
		width: 36px;
		height: 36px;
		object-fit: cover;
		display: block;
	}

	.avatar-initials {
		width: 36px;
		height: 36px;
		border-radius: 50%;
		background: var(--tg-accent, #e05252);
		color: #fff;
		font-size: 13px;
		font-weight: 600;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.author-info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
	}

	.author-display {
		font-size: 14px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.author-username {
		font-size: 12px;
		color: var(--color-text-secondary, rgba(240, 240, 240, 0.5));
	}

	.post-time {
		flex-shrink: 0;
		font-size: 12px;
		color: var(--color-text-secondary, rgba(240, 240, 240, 0.5));
	}

	/* Content */
	.post-content {
		font-size: 15px;
		line-height: 1.5;
		color: var(--color-text-primary, #f0f0f0);
		margin: 8px 0 0;
		word-break: break-word;
	}

	/* Photos */
	.photo-strip {
		display: flex;
		overflow-x: auto;
		scroll-snap-type: x mandatory;
		scroll-behavior: smooth;
		-webkit-overflow-scrolling: touch;
		scrollbar-width: none;
	}

	.photo-strip::-webkit-scrollbar {
		display: none;
	}

	.photo-item {
		flex-shrink: 0;
		width: 100%;
		aspect-ratio: 4 / 3;
		scroll-snap-align: start;
	}

	.photo-item img {
		width: 100%;
		height: 100%;
		object-fit: cover;
		display: block;
	}

	/* Photo dots */
	.photo-dots {
		display: flex;
		justify-content: center;
		gap: 5px;
		padding: 8px 0 4px;
	}

	.photo-dot {
		height: 6px;
		border-radius: 3px;
		background: rgba(255, 255, 255, 0.3);
		width: 6px;
		transition: width 0.2s ease, background 0.2s ease;
	}

	.photo-dot.active {
		width: 12px;
		background: #ffffff;
	}

	/* Action row */
	.action-row {
		display: flex;
		align-items: center;
		gap: 4px;
		padding-top: 4px;
	}

	/* Reactions */
	.reaction-btn {
		min-width: 52px;
		min-height: 44px;
		background: none;
		border: none;
		cursor: pointer;
		display: flex;
		align-items: center;
		gap: 5px;
		padding: 6px 10px;
		border-radius: 8px;
		transition: background 0.15s ease;
	}

	.reaction-btn.active {
		background: rgba(224, 82, 82, 0.18);
	}

	.reaction-btn:active {
		background: rgba(255, 255, 255, 0.06);
	}

	.reaction-img {
		width: 22px;
		height: 22px;
		object-fit: contain;
	}

	.reaction-count {
		font-size: 13px;
		font-weight: 500;
		color: rgba(240, 240, 240, 0.65);
	}

	.reaction-btn.active .reaction-count {
		color: var(--tg-accent, #e05252);
	}

	.comment-btn {
		margin-left: auto;
		color: var(--color-text-secondary, rgba(240, 240, 240, 0.6));
	}

	@keyframes reactionPop {
		0% { transform: scale(1); }
		50% { transform: scale(1.4); }
		100% { transform: scale(1); }
	}

	:global(.reaction-popping) {
		animation: reactionPop 300ms ease-out;
	}

	/* Infinite scroll */
	.scroll-sentinel {
		height: 1px;
	}

	.loading-more {
		padding: 16px;
		text-align: center;
		font-size: 13px;
		color: var(--color-text-secondary, rgba(240, 240, 240, 0.5));
	}

</style>
