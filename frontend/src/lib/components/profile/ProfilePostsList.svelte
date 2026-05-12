<script lang="ts">
	import { formatRelativeTime } from '$lib/utils/formatRelativeTime';
	import { viewTracker } from '$lib/utils/viewTracker';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { REACTION_TYPES, REACTION_VISUALS } from '$lib/reactions';
	import { UseReactions } from '$lib/features/posts/useReactions.svelte';
	import type { Post } from '$lib/types';
	import type { ReactionType } from '$lib/types';

	interface Props {
		posts: Post[];
		postsLoading?: boolean;
		postsError: string | null;
		onOpenComments: (postId: string) => void;
		onRetry: () => void;
	}

	let { posts, postsLoading = false, postsError, onOpenComments, onRetry }: Props = $props();

	let reactionHooks = $state(new Map<string, UseReactions>());
	$effect(() => {
		const seen = new Set(posts.map((p) => p.id));
		let changed = false;
		for (const p of posts) {
			if (!reactionHooks.has(p.id)) {
				reactionHooks.set(p.id, new UseReactions(p.id));
				changed = true;
			}
		}
		for (const id of reactionHooks.keys()) {
			if (!seen.has(id)) {
				reactionHooks.delete(id);
				changed = true;
			}
		}
		if (changed) reactionHooks = new Map(reactionHooks);
	});

	function formatViews(n: number): string {
		if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
		if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
		return String(n);
	}

	function trackView(el: HTMLElement, postId: string): { destroy: () => void } {
		const cleanup = viewTracker.observe(el, postId, authStore.isAuthenticated);
		return { destroy: cleanup };
	}

	function handleReaction(postId: string, type: ReactionType): void {
		reactionHooks.get(postId)?.react(type);
	}
</script>

{#if postsError}
	<div class="state-box" role="alert">
		<p class="state-text err">{postsError}</p>
		<button class="retry-btn" onclick={onRetry}>Try again</button>
	</div>
{:else if postsLoading && posts.length === 0}
	<div class="skeleton-list" aria-busy="true" aria-label="Loading posts">
		{#each [80, 56, 96] as h}
			<div class="skeleton-post" style="--h:{h}px"></div>
		{/each}
	</div>
{:else if posts.length === 0}
	<div class="state-box">
		<svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor"
			stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"
			style="color:rgba(255,255,255,0.2)" aria-hidden="true">
			<path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/>
			<polyline points="14 2 14 8 20 8"/>
		</svg>
		<p class="state-text">No posts yet</p>
	</div>
{:else}
	<ul class="feed" role="list" aria-label="Posts">
		{#each posts as post (post.id)}
			{@const rx = reactionHooks.get(post.id)}
			<li class="post-card" role="listitem" use:trackView={post.id}>
				<p class="post-content">{post.content}</p>

				{#if post.photos && post.photos.length > 0}
					<div class="photo-strip">
						{#each post.photos.slice().sort((a, b) => a.position - b.position) as photo (photo.id)}
							<div class="photo-item">
								<img src={photo.url} alt="" loading="lazy" />
							</div>
						{/each}
					</div>
				{/if}

				<div class="action-row">
					<!-- Reactions -->
					<div class="reactions">
						{#each REACTION_TYPES as type (type)}
							{@const rv = REACTION_VISUALS[type]}
							{@const isActive = rx?.myReaction === type}
							{@const isPopping = rx?.poppingType === type}
							{@const count = rx?.reactions[type] ?? 0}
							<button
								class="react-btn"
								class:active={isActive}
								onclick={() => handleReaction(post.id, type)}
								aria-label="{type}, {count}"
							>
								{#if rv.assetPath}
									<img src={rv.assetPath} alt={rv.label}
										class="react-img" class:popping={isPopping} loading="lazy" />
								{:else}
									<span class="react-emoji" class:popping={isPopping} aria-hidden="true">
										{rv.fallbackEmoji}
									</span>
								{/if}
								{#if count > 0}
									<span class="react-count">{count}</span>
								{/if}
							</button>
						{/each}
					</div>

					<!-- Right side: comments + views + time -->
					<div class="post-meta">
						<button
							class="comment-btn"
							onclick={() => onOpenComments(post.id)}
							aria-label="Comments"
						>
							<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor"
								stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
								<path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/>
							</svg>
						</button>
						<span class="meta-dot" aria-hidden="true">·</span>
						<span class="view-count" aria-label="{post.viewCount} views">
							{formatViews(post.viewCount)}
						</span>
						<span class="meta-dot" aria-hidden="true">·</span>
						<time class="post-time" datetime={post.createdAt}>
							{formatRelativeTime(post.createdAt)}
						</time>
					</div>
				</div>
			</li>
		{/each}
	</ul>
{/if}

<style>
	/* ── States ────────────────────────────────────────────────────────────── */

	.state-box {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 10px;
		padding: 48px 16px;
	}

	.state-text {
		font-size: 14px;
		color: rgba(255, 255, 255, 0.35);
		margin: 0;
		text-align: center;
	}

	.state-text.err { color: #e05252; }

	.retry-btn {
		padding: 8px 20px;
		border-radius: 10px;
		border: 1px solid rgba(255, 255, 255, 0.1);
		background: none;
		color: rgba(255, 255, 255, 0.7);
		font-size: 14px;
		font-weight: 500;
		cursor: pointer;
		font-family: inherit;
		-webkit-tap-highlight-color: transparent;
	}

	.retry-btn:active { opacity: 0.7; }

	/* ── Skeleton ──────────────────────────────────────────────────────────── */

	.skeleton-list {
		display: flex;
		flex-direction: column;
		gap: 1px;
	}

	.skeleton-post {
		height: var(--h, 80px);
		background: rgba(255, 255, 255, 0.04);
		animation: pulse 1.4s ease-in-out infinite;
	}

	@keyframes pulse {
		0%, 100% { opacity: 1; }
		50% { opacity: 0.4; }
	}

	/* ── Feed ──────────────────────────────────────────────────────────────── */

	.feed {
		display: flex;
		flex-direction: column;
		list-style: none;
		padding: 0;
		margin: 0;
	}

	.post-card {
		padding: 14px 16px 10px;
		border-bottom: 1px solid rgba(255, 255, 255, 0.06);
		-webkit-tap-highlight-color: transparent;
	}

	/* ── Content ───────────────────────────────────────────────────────────── */

	.post-content {
		font-size: 15px;
		line-height: 1.5;
		color: var(--tg-theme-text-color, #f0f0f0);
		margin: 0 0 10px;
		word-break: break-word;
	}

	/* ── Photos ────────────────────────────────────────────────────────────── */

	.photo-strip {
		display: flex;
		overflow-x: auto;
		scroll-snap-type: x mandatory;
		-webkit-overflow-scrolling: touch;
		scrollbar-width: none;
		border-radius: 10px;
		overflow: hidden;
		margin-bottom: 10px;
	}

	.photo-strip::-webkit-scrollbar { display: none; }

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

	/* ── Action row ────────────────────────────────────────────────────────── */

	.action-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 4px;
	}

	/* ── Reactions ─────────────────────────────────────────────────────────── */

	.reactions {
		display: flex;
		align-items: center;
		gap: 0;
	}

	.react-btn {
		min-width: 40px;
		min-height: 34px;
		background: none;
		border: none;
		cursor: pointer;
		display: flex;
		align-items: center;
		gap: 3px;
		padding: 4px 6px;
		border-radius: 8px;
		-webkit-tap-highlight-color: transparent;
		transition: background-color 0.12s;
	}

	.react-btn:active { background: rgba(255, 255, 255, 0.05); }
	.react-btn.active { background: rgba(224, 82, 82, 0.12); }

	.react-img {
		width: 18px;
		height: 18px;
		object-fit: contain;
	}

	.react-emoji {
		font-size: 16px;
		line-height: 1;
		width: 18px;
		display: inline-flex;
		align-items: center;
		justify-content: center;
	}

	.react-count {
		font-size: 12px;
		font-weight: 500;
		color: rgba(240, 240, 240, 0.5);
	}

	.react-btn.active .react-count { color: #e05252; }

	/* ── Post meta (right side) ────────────────────────────────────────────── */

	.post-meta {
		display: flex;
		align-items: center;
		gap: 5px;
		flex-shrink: 0;
	}

	.comment-btn {
		background: none;
		border: none;
		cursor: pointer;
		color: rgba(240, 240, 240, 0.35);
		padding: 4px;
		display: flex;
		align-items: center;
		-webkit-tap-highlight-color: transparent;
		border-radius: 6px;
		transition: color 0.12s;
	}

	.comment-btn:active { color: rgba(240, 240, 240, 0.7); }

	.meta-dot {
		font-size: 10px;
		color: rgba(255, 255, 255, 0.2);
	}

	.view-count {
		font-size: 11px;
		color: rgba(240, 240, 240, 0.3);
	}

	.post-time {
		font-size: 11px;
		color: rgba(240, 240, 240, 0.28);
	}

	/* ── Animation ─────────────────────────────────────────────────────────── */

	@keyframes reactionPop {
		0% { transform: scale(1); }
		50% { transform: scale(1.4); }
		100% { transform: scale(1); }
	}

	:global(.popping) {
		animation: reactionPop 300ms ease-out;
	}
</style>
