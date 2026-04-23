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
		postsError: string | null;
		onOpenComments: (postId: number) => void;
		onRetry: () => void;
	}

	let { posts, postsError, onOpenComments, onRetry }: Props = $props();

	const reactionHooks = $derived(
		new Map(posts.map((p) => [p.id, new UseReactions(p.id)]))
	);

	function formatViews(n: number): string {
		if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
		if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
		return String(n);
	}

	function trackView(el: HTMLElement, postId: number): { destroy: () => void } {
		const cleanup = viewTracker.observe(el, postId, authStore.isAuthenticated);
		return { destroy: cleanup };
	}

	function handleReaction(postId: number, type: ReactionType): void {
		reactionHooks.get(postId)?.react(type);
	}
</script>

{#if postsError}
	<div class="posts-error" role="alert">
		<p class="error-text">{postsError}</p>
		<button class="retry-btn" onclick={onRetry}>Try again</button>
	</div>
{:else if posts.length === 0}
	<div class="posts-empty">
		<p class="posts-empty-text">No posts yet.</p>
	</div>
{:else}
	<div class="profile-feed" role="list" aria-label="Posts">
		{#each posts as post (post.id)}
			{@const rx = reactionHooks.get(post.id)}
			<article class="profile-post-card" role="listitem" use:trackView={post.id}>
				<p class="profile-post-content">{post.content}</p>

				{#if post.photos && post.photos.length > 0}
					<div class="profile-photo-strip">
						{#each post.photos.slice().sort((a, b) => a.position - b.position) as photo (photo.id)}
							<div class="profile-photo-item">
								<img src={photo.url} alt="" loading="lazy" />
							</div>
						{/each}
					</div>
				{/if}

				<div class="profile-action-row">
					{#each REACTION_TYPES as type (type)}
						{@const reactionVisual = REACTION_VISUALS[type]}
						{@const isActive = rx?.myReaction === type}
						{@const isPopping = rx?.poppingType === type}
						{@const count = rx?.reactions[type] ?? 0}
						<button
							class="p-reaction-btn"
							class:active={isActive}
							onclick={() => handleReaction(post.id, type)}
							aria-label="{type} reaction, count {count}"
						>
							{#if reactionVisual.assetPath}
								<img
									src={reactionVisual.assetPath}
									alt={reactionVisual.label}
									class="p-reaction-img"
									class:p-reaction-popping={isPopping}
									loading="lazy"
								/>
							{:else}
								<span
									class="p-reaction-emoji"
									class:p-reaction-popping={isPopping}
									aria-hidden="true"
								>
									{reactionVisual.fallbackEmoji}
								</span>
							{/if}
							{#if count > 0}
								<span class="p-reaction-count">{count}</span>
							{/if}
						</button>
					{/each}

					<span class="p-view-count" aria-label="{post.viewCount} views">
						<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
							<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
							<circle cx="12" cy="12" r="3"/>
						</svg>
						{formatViews(post.viewCount)}
					</span>

					<button
						class="p-reaction-btn p-comment-btn"
						onclick={() => onOpenComments(post.id)}
						aria-label="Comments"
					>
						<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
							<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
						</svg>
					</button>

					<span class="profile-post-time">{formatRelativeTime(post.createdAt)}</span>
				</div>
			</article>
		{/each}
	</div>
{/if}

<style>
	.posts-error,
	.posts-empty {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 10px;
		padding: 40px 16px;
	}

	.posts-empty-text {
		font-size: 14px;
		color: rgba(255, 255, 255, 0.4);
		margin: 0;
	}

	.error-text {
		font-size: 14px;
		color: #e05252;
		text-align: center;
	}

	.retry-btn {
		padding: 8px 20px;
		border-radius: 10px;
		border: 1px solid var(--color-border, rgba(255, 255, 255, 0.15));
		background: none;
		color: var(--color-text-primary, #f0f0f0);
		font-size: 14px;
		font-weight: 500;
		cursor: pointer;
		font-family: inherit;
	}

	.retry-btn:active {
		opacity: 0.7;
	}

	.profile-feed {
		display: flex;
		flex-direction: column;
	}

	.profile-post-card {
		padding: 14px 16px 10px;
		border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.08));
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
	}

	.profile-post-card:active {
		background: rgba(255, 255, 255, 0.03);
	}

	.profile-post-content {
		font-size: 15px;
		line-height: 1.5;
		color: var(--color-text-primary, #f0f0f0);
		margin: 0 0 10px;
		word-break: break-word;
	}

	.profile-photo-strip {
		display: flex;
		overflow-x: auto;
		scroll-snap-type: x mandatory;
		scroll-behavior: smooth;
		-webkit-overflow-scrolling: touch;
		scrollbar-width: none;
		border-radius: 12px;
		overflow: hidden;
		margin-bottom: 8px;
	}

	.profile-photo-strip::-webkit-scrollbar {
		display: none;
	}

	.profile-photo-item {
		flex-shrink: 0;
		width: 100%;
		aspect-ratio: 4 / 3;
		scroll-snap-align: start;
	}

	.profile-photo-item img {
		width: 100%;
		height: 100%;
		object-fit: cover;
		display: block;
	}

	.profile-action-row {
		display: flex;
		align-items: center;
		gap: 2px;
		margin-top: 6px;
	}

	.p-reaction-btn {
		min-width: 44px;
		min-height: 36px;
		background: none;
		border: none;
		cursor: pointer;
		display: flex;
		align-items: center;
		gap: 4px;
		padding: 5px 8px;
		border-radius: 8px;
		transition: background 0.15s ease;
		-webkit-tap-highlight-color: transparent;
	}

	.p-reaction-btn.active {
		background: rgba(224, 82, 82, 0.18);
	}

	.p-reaction-btn:active {
		background: rgba(255, 255, 255, 0.06);
	}

	.p-reaction-img {
		width: 20px;
		height: 20px;
		object-fit: contain;
	}

	.p-reaction-emoji {
		width: 20px;
		height: 20px;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		font-size: 17px;
		line-height: 1;
	}

	.p-reaction-count {
		font-size: 12px;
		font-weight: 500;
		color: rgba(240, 240, 240, 0.6);
	}

	.p-reaction-btn.active .p-reaction-count {
		color: #e05252;
	}

	.p-view-count {
		display: flex;
		align-items: center;
		gap: 4px;
		margin-left: auto;
		font-size: 12px;
		color: rgba(240, 240, 240, 0.38);
		user-select: none;
	}

	.p-comment-btn {
		color: rgba(240, 240, 240, 0.4);
	}

	.profile-post-time {
		font-size: 12px;
		color: rgba(240, 240, 240, 0.3);
		margin-left: 4px;
	}

	@keyframes profileReactionPop {
		0% { transform: scale(1); }
		50% { transform: scale(1.4); }
		100% { transform: scale(1); }
	}

	:global(.p-reaction-popping) {
		animation: profileReactionPop 300ms ease-out;
	}
</style>
