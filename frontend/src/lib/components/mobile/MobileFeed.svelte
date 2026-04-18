<script lang="ts">
	import { onMount } from 'svelte';
	import type { Post, ReactionType, ReactionsMap } from '$lib/types';
	import { postService } from '$lib/services';
	import { mobileFeedStore } from '$lib/stores/mobile-feed.store';
	import { getTelegramWebApp } from '$lib/telegram';
	import { DEFAULT_REACTIONS, REACTION_TYPES, REACTION_VISUALS } from '$lib/reactions';
	import { viewTracker } from '$lib/utils/viewTracker';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import CommentSheet from './CommentSheet.svelte';
	import { openProfile } from '$lib/stores/profile-nav.store';

	const INITIAL_REACTION_PREFETCH_COUNT = 5;

	// Feed state mirrored from store
	let posts = $state<Post[]>([]);
	let isLoading = $state(false);
	let isLoadingMore = $state(false);
	let hasNext = $state(false);
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

	// Refresh indicator
	let isRefreshing = $state(false);

	function formatViews(n: number): string {
		if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
		if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
		return String(n);
	}

	function trackView(el: HTMLElement, postId: number): { destroy: () => void } {
		const cleanup = viewTracker.observe(el, postId, authStore.isAuthenticated);
		return { destroy: cleanup };
	}

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
			reactions: { ...DEFAULT_REACTIONS },
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
				reactions: { ...DEFAULT_REACTIONS, ...reactions },
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

	function prefetchReactions(postsToPrefetch: Post[]): void {
		postsToPrefetch.slice(0, INITIAL_REACTION_PREFETCH_COUNT).forEach((post) => {
			loadReactionsForPost(post.id);
		});
	}

	async function fetchFeed(options: { resetPage?: boolean; force?: boolean } = {}): Promise<void> {
		const { resetPage = false, force = false } = options;
		try {
			const fetched = await mobileFeedStore.load({ force, resetPage });
			prefetchReactions(fetched);
		} catch (err) {
			console.error('[MobileFeed] fetchFeed error:', err);
		}
	}

	async function loadMorePosts(): Promise<void> {
		if (isLoadingMore) return;
		await mobileFeedStore.loadMore();
	}

	function infiniteScroll(el: HTMLElement): { destroy: () => void } {
		const obs = new IntersectionObserver(
			([entry]) => {
				if (entry.isIntersecting && !isLoadingMore) loadMorePosts();
			},
			{ threshold: 0.1 }
		);
		obs.observe(el);
		return { destroy: () => obs.disconnect() };
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
				await fetchFeed({ resetPage: true, force: true });
			} finally {
				isRefreshing = false;
			}
		} else {
			pullThresholdMet = false;
		}
	}

	onMount(() => {
		let initialized = false;

		const unsub = mobileFeedStore.subscribe((s) => {
			posts = s.posts;
			isLoading = s.status === 'loading';
			isLoadingMore = s.isLoadingMore;
			hasNext = s.hasNext;
			feedError = s.error;

			// Trigger fetch on first store update if posts are empty
			if (!initialized) {
				initialized = true;
				if (posts.length === 0) {
					mobileFeedStore.reset();
					fetchFeed({ force: true });
				} else {
					prefetchReactions(posts);
				}
			}
		});

		return () => {
			unsub();
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

<!-- Brand title sits inside the transparent TG header — centered between Close and action buttons -->
<div class="feed-header-title" aria-hidden="true">
	<span class="feed-header-brand">critiqal</span>
</div>

<div
	class="feed-container"
	role="region"
	aria-label="Feed"
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
			<button class="retry-btn" onclick={() => fetchFeed({ force: true, resetPage: true })}>Retry</button>
		</div>
	{:else}
		{#each posts as post (post.id)}
			<article class="post-card" use:trackView={post.id}>
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
						{#each [...post.photos].sort((a, b) => a.position - b.position) as photo (photo.id)}
							<div class="photo-item">
								<img src={photo.url} alt="" loading="lazy" />
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
						{@const reactionVisual = REACTION_VISUALS[type]}
						{@const isActive = rs.myReaction === type}
						{@const isPopping = rs.poppingType === type}
						<button
							class="reaction-btn"
							class:active={isActive}
							onclick={() => handleReaction(post, type)}
							aria-label="{type} reaction, count {rs.reactions[type]}"
							onmouseenter={() => { if (!rs.loaded) loadReactionsForPost(post.id); }}
						>
							{#if reactionVisual.assetPath}
								<img
									src={reactionVisual.assetPath}
									alt={reactionVisual.label}
									class="reaction-img"
									class:reaction-popping={isPopping}
									loading="lazy"
								/>
							{:else}
								<span
									class="reaction-emoji"
									class:reaction-popping={isPopping}
									aria-hidden="true"
								>
									{reactionVisual.fallbackEmoji}
								</span>
							{/if}
							{#if rs.reactions[type] > 0}
								<span class="reaction-count">{rs.reactions[type]}</span>
							{/if}
						</button>
					{/each}

					<!-- View count -->
					<span class="view-count" aria-label="{post.viewCount} views">
						<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
							<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
							<circle cx="12" cy="12" r="3"/>
						</svg>
						{formatViews(post.viewCount)}
					</span>

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

		<!-- Infinite scroll sentinel — only rendered when more pages exist -->
		{#if hasNext}
			<div class="scroll-sentinel" use:infiniteScroll></div>
		{/if}

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
	/* ── Header brand title — lives in the transparent TG header strip ─────────── */
	/* top = device status bar height; height = TG header bar height (~44px).
	   pointer-events: none so TG's native Close / action buttons remain tappable. */
	.feed-header-title {
		position: fixed;
		top: env(safe-area-inset-top, 0px);
		left: 0;
		right: 0;
		height: 44px;
		display: flex;
		align-items: center;
		justify-content: center;
		pointer-events: none;
		z-index: 5;
	}

	.feed-header-brand {
		font-size: 17px;
		font-weight: 700;
		letter-spacing: 0.04em;
		color: var(--tg-text, #f0f0f0);
		text-transform: lowercase;
	}

	.feed-container {
		height: 100%;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
		scrollbar-width: none;
		padding-top: max(
			var(--tg-content-safe-area-inset-top, 0px),
			calc(env(safe-area-inset-top, 20px) + 44px)
		);
		padding-bottom: var(--content-bottom-padding, 104px);
		position: relative;
	}

	.feed-container::-webkit-scrollbar {
		display: none;
	}

	/* Pull-to-refresh indicator */
	.refresh-indicator {
		position: absolute;
		top: max(
			calc(var(--tg-content-safe-area-inset-top, 0px) + 12px),
			calc(env(safe-area-inset-top, 20px) + 44px + 12px)
		);
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

	.reaction-emoji {
		width: 22px;
		height: 22px;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		font-size: 18px;
		line-height: 1;
	}

	.reaction-count {
		font-size: 13px;
		font-weight: 500;
		color: rgba(240, 240, 240, 0.65);
	}

	.reaction-btn.active .reaction-count {
		color: var(--tg-accent, #e05252);
	}

	.view-count {
		display: flex;
		align-items: center;
		gap: 4px;
		margin-left: auto;
		font-size: 12px;
		color: rgba(240, 240, 240, 0.38);
		user-select: none;
	}

	.comment-btn {
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
