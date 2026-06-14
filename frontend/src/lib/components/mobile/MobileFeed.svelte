<script lang="ts">
	import { onMount } from 'svelte';
	import { mobileFeedStore } from '$lib/stores/mobile-feed.store.svelte';
	import { openMobileComments } from '$lib/stores/mobile-comments.store';
	import { getTelegramWebApp } from '$lib/telegram';
	import { openProfile } from '$lib/stores/profile-nav.store.svelte';
	import { Post as PostComponent } from '$lib/components/post';
	import type { Post } from '$lib/types';
	import StarDraw from '$lib/ui/StarDraw.svelte';
	import { t } from '$lib/i18n';

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

	// Brand title in the transparent TG header strip frosts in on scroll.
	// Progress 0..1 over the first 90px of scroll, eased (ease-out quad) so the
	// glass + brand bloom in softly at the start and settle gently — drives the
	// full-height frosted header and the brand fade/rise.
	let headerProgress = $state(0);
	let headerTicking = false;
	function onContainerScroll(): void {
		if (headerTicking) return;
		headerTicking = true;
		requestAnimationFrame(() => {
			const top = containerEl?.scrollTop ?? 0;
			const raw = Math.min(1, Math.max(0, top / 90));
			headerProgress = 1 - (1 - raw) * (1 - raw);
			headerTicking = false;
		});
	}

	function openComments(post: Post): void {
		openMobileComments(post);
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
<div
	class="feed-header-title"
	style="--header-progress: {headerProgress}"
	aria-hidden="true"
>
	<span class="feed-header-brand">critiqal</span>
</div>

<div
	class="feed-container"
	role="region"
	aria-label={t('nav.feed')}
	bind:this={containerEl}
	onscroll={onContainerScroll}
	ontouchstart={onPullTouchStart}
	ontouchmove={onPullTouchMove}
	ontouchend={onPullTouchEnd}
>
	{#if mobileFeedStore.status === 'loading' && mobileFeedStore.posts.length === 0}
		<div class="feed-loader" aria-busy="true" aria-label={t('common.loading')}>
			<StarDraw size={52} duration={1900} title={t('common.loading')} />
		</div>
	{:else if mobileFeedStore.error && mobileFeedStore.posts.length === 0}
		<div class="feed-state error">
			<p>{mobileFeedStore.error}</p>
			<button class="retry-btn" onclick={() => fetchFeed({ force: true })}>{t('common.retry')}</button>
		</div>
	{:else}
		{#each mobileFeedStore.posts as post (post.id)}
			<PostComponent
				{post}
				variant="mobile"
				onAuthorClick={(username) => openProfile(username)}
				onOpenComments={openComments}
				onDeleted={(id) => mobileFeedStore.removePost(id)}
			/>
		{/each}

		<!-- Infinite scroll sentinel — only rendered when more pages exist -->
		{#if mobileFeedStore.hasNext}
			<div class="scroll-sentinel" use:infiniteScroll></div>
		{/if}

		{#if mobileFeedStore.isLoadingMore}
			<div class="loading-more" aria-busy="true" aria-label={t('common.loading')}>
				<StarDraw size={24} duration={1500} title={t('common.loading')} />
			</div>
		{/if}
	{/if}
</div>


<style>
	/* ── Header brand title — full-height frosted glass over the whole top ──────── */
	/* Covers the status bar + the native TG header band (and bleeds ~10px into the
	   content below), like the profile header — not just a thin 44px strip. The
	   brand sits centred in the native-header band (between the paddings). All of
	   background, blur, divider and the brand itself interpolate against
	   --header-progress, so the glass and title bloom in smoothly on scroll.
	   pointer-events: none so TG's native Close / action buttons stay tappable. */
	.feed-header-title {
		position: fixed;
		top: 0;
		left: 0;
		right: 0;
		box-sizing: border-box;
		padding-top: env(safe-area-inset-top, 0px);
		padding-bottom: 10px;
		height: calc(env(safe-area-inset-top, 0px) + 54px);
		display: flex;
		align-items: center;
		justify-content: center;
		pointer-events: none;
		z-index: 5;
		/* Noticeably more transparent (~55% fill) — leans on the blur for
		   legibility so the feed glows softly through the glass. */
		background-color: color-mix(in srgb, var(--color-bg) calc(var(--header-progress, 0) * 55%), transparent);
		backdrop-filter: blur(calc(var(--header-progress, 0) * var(--glass-blur-md))) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(calc(var(--header-progress, 0) * var(--glass-blur-md))) saturate(var(--glass-saturate));
	}

	.feed-header-title::after {
		content: '';
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		height: 1px;
		background: linear-gradient(to right, transparent, var(--glass-border), transparent);
		opacity: var(--header-progress, 0);
		pointer-events: none;
	}

	/* Brand fades in and rises 2px as the glass forms — a soft, premium reveal
	   rather than a title that's just always there. */
	.feed-header-brand {
		font-size: 17px;
		font-weight: 700;
		letter-spacing: 0.04em;
		color: var(--tg-text, #f0f0f0);
		text-transform: lowercase;
		opacity: var(--header-progress, 0);
		transform: translateY(calc((1 - var(--header-progress, 0)) * 2px));
		will-change: opacity, transform;
	}

	@media (prefers-reduced-motion: reduce) {
		.feed-header-brand {
			transform: none;
		}
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
		color: var(--color-text-secondary, var(--text-tertiary));
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
