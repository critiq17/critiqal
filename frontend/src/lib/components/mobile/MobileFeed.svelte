<script lang="ts">
	import { onMount } from 'svelte';
	import { mobileFeedStore } from '$lib/stores/mobile-feed.store.svelte';
	import { openMobileComments } from '$lib/stores/mobile-comments.store';
	import { getTelegramWebApp } from '$lib/telegram';
	import { openProfile } from '$lib/stores/profile-nav.store.svelte';
	import { Post as PostComponent } from '$lib/components/post';
	import StarDraw from '$lib/ui/StarDraw.svelte';

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

	function openComments(postId: string): void {
		openMobileComments(postId);
	}

	async function fetchFeed(options: { force?: boolean } = {}): Promise<void> {
		const { force = false } = options;
		try {
			await mobileFeedStore.load({ force });
		} catch {
			// Non-critical — store manages its own error state
		}
	}

	async function loadMorePosts(): Promise<void> {
		if (mobileFeedStore.isLoadingMore) return;
		await mobileFeedStore.loadMore();
	}

	function infiniteScroll(el: HTMLElement): { destroy: () => void } {
		const obs = new IntersectionObserver(
			([entry]) => {
				if (entry?.isIntersecting && !mobileFeedStore.isLoadingMore) loadMorePosts();
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
		pullStartY = e.touches[0]?.clientY ?? 0;
		pullHapticFired = false;
	}

	function onPullTouchMove(e: TouchEvent): void {
		const scrollTop = containerEl?.scrollTop ?? 0;
		if (scrollTop !== 0) {
			isPulling = false;
			pullY = 0;
			return;
		}
		const delta = (e.touches[0]?.clientY ?? pullStartY) - pullStartY;
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
				await fetchFeed({ force: true });
			} finally {
				isRefreshing = false;
			}
		} else {
			pullThresholdMet = false;
		}
	}

	onMount(() => {
		// ensureLoaded is stale-while-revalidate: returns cached posts instantly
		// if fresh, otherwise refetches without resetting visible state.
		mobileFeedStore.ensureLoaded();
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
	{#if mobileFeedStore.status === 'loading' && mobileFeedStore.posts.length === 0}
		<div class="feed-loader" aria-busy="true" aria-label="Loading feed">
			<StarDraw size={52} duration={1900} title="Loading feed" />
		</div>
	{:else if mobileFeedStore.error && mobileFeedStore.posts.length === 0}
		<div class="feed-state error">
			<p>{mobileFeedStore.error}</p>
			<button class="retry-btn" onclick={() => fetchFeed({ force: true })}>Retry</button>
		</div>
	{:else}
		{#each mobileFeedStore.posts as post (post.id)}
			<PostComponent
				{post}
				variant="mobile"
				onAuthorClick={(username) => openProfile(username)}
				onOpenComments={(postId) => openComments(postId)}
			/>
		{/each}

		<!-- Infinite scroll sentinel — only rendered when more pages exist -->
		{#if mobileFeedStore.hasNext}
			<div class="scroll-sentinel" use:infiniteScroll></div>
		{/if}

		{#if mobileFeedStore.isLoadingMore}
			<div class="loading-more" aria-busy="true" aria-label="Loading more posts">
				<StarDraw size={24} duration={1500} title="Loading more posts" />
			</div>
		{/if}
	{/if}
</div>


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
		padding-top: var(--tg-top-clearance);
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

	/* Quiet brand loader — centered, calm, no skeleton noise */
	.feed-loader {
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 22vh 16px;
		opacity: 0.9;
	}

	/* Infinite scroll */
	.scroll-sentinel {
		height: 1px;
	}

	.loading-more {
		display: flex;
		align-items: center;
		justify-content: center;
		padding: 16px;
		opacity: 0.7;
	}

</style>
