<script lang="ts">
	import type { Post, ReactionsMap, ReactionType } from '$lib/types';
	import { postService } from '$lib/services';
	import { authStore } from '$lib/stores/auth.store.svelte';

	interface Props {
		post: Post;
	}

	let { post }: Props = $props();

	let reactions = $state<ReactionsMap>({ GIGACHAD: 0, THE_ROCK: 0, DAVID: 0 });
	let commentCount = $state(0);
	let isLiked = $state(false);
	let isLiking = $state(false);
	let isExpanded = $state(false);
	let reactionsLoaded = $state(false);

	const HEART_REACTION: ReactionType = 'GIGACHAD';

	$effect(() => {
		loadReactions();
	});

	async function loadReactions(): Promise<void> {
		try {
			const data = await postService.getReactions(post.id);
			reactions = data;
			reactionsLoaded = true;
		} catch {
			reactionsLoaded = true;
		}
	}

	async function handleReaction(): Promise<void> {
		if (!authStore.isAuthenticated || isLiking) return;

		const previousLiked = isLiked;
		const previousCount = reactions[HEART_REACTION];

		isLiked = !isLiked;
		reactions = {
			...reactions,
			[HEART_REACTION]: isLiked ? previousCount + 1 : Math.max(0, previousCount - 1)
		};

		isLiking = true;
		try {
			if (isLiked) {
				await postService.react(post.id, HEART_REACTION);
			} else {
				await postService.removeReaction(post.id);
			}
		} catch {
			isLiked = previousLiked;
			reactions = { ...reactions, [HEART_REACTION]: previousCount };
		} finally {
			isLiking = false;
		}
	}

	function toggleComments(): void {
		isExpanded = !isExpanded;
	}

	function formatTimestamp(isoString: string): string {
		const date = new Date(isoString);
		const now = new Date();
		const diffMs = now.getTime() - date.getTime();
		const diffMins = Math.floor(diffMs / 60_000);
		const diffHours = Math.floor(diffMins / 60);
		const diffDays = Math.floor(diffHours / 24);

		if (diffMins < 1) return 'just now';
		if (diffMins < 60) return `${diffMins}m`;
		if (diffHours < 24) return `${diffHours}h`;
		if (diffDays < 7) return `${diffDays}d`;
		return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
	}

	function getAvatarInitial(user: Post['author']): string {
		return (user.name ?? user.username).charAt(0).toUpperCase();
	}

	const totalLikes = $derived(reactions[HEART_REACTION]);
</script>

<article class="post-card">
	<div class="post-header">
		<div class="avatar" aria-hidden="true">
			{#if post.author.avatarUrl}
				<img src={post.author.avatarUrl} alt={post.author.username} class="avatar-img" />
			{:else}
				<span class="avatar-initial">{getAvatarInitial(post.author)}</span>
			{/if}
		</div>
		<div class="author-meta">
			<span class="author-name">{post.author.name ?? post.author.username}</span>
			<span class="author-username">@{post.author.username}</span>
		</div>
	</div>

	<div class="post-body">
		<p class="post-content">{post.content}</p>
		{#if post.photoUrl}
			<img src={post.photoUrl} alt="Post attachment" class="post-image" />
		{/if}
	</div>

	<div class="post-footer">
		<button
			class="action-btn reaction-btn"
			class:liked={isLiked}
			class:animating={isLiking}
			onclick={handleReaction}
			disabled={!authStore.isAuthenticated}
			aria-label={isLiked ? 'Unlike post' : 'Like post'}
			aria-pressed={isLiked}
		>
			<svg
				class="icon heart-icon"
				viewBox="0 0 24 24"
				fill={isLiked ? 'currentColor' : 'none'}
				stroke="currentColor"
				stroke-width="2"
				aria-hidden="true"
			>
				<path
					d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"
				/>
			</svg>
			{#if reactionsLoaded}
				<span class="action-count">{totalLikes > 0 ? totalLikes : ''}</span>
			{/if}
		</button>

		<button class="action-btn comment-btn" onclick={toggleComments} aria-label="Toggle comments">
			<svg
				class="icon"
				viewBox="0 0 24 24"
				fill="none"
				stroke="currentColor"
				stroke-width="2"
				aria-hidden="true"
			>
				<path
					d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"
				/>
			</svg>
			{#if commentCount > 0}
				<span class="action-count">{commentCount}</span>
			{/if}
		</button>

		<time class="post-timestamp" datetime={post.createdAt}>
			{formatTimestamp(post.createdAt)}
		</time>
	</div>

	{#if isExpanded}
		<div class="comments-panel" role="region" aria-label="Comments">
			<p class="comments-hint">Comments coming soon.</p>
		</div>
	{/if}
</article>

<style>
	.post-card {
		padding: 1.25rem 0;
		border-bottom: 1px solid var(--color-border);
		transition: background-color 0.15s ease;
	}

	.post-card:last-child {
		border-bottom: none;
	}

	.post-header {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		margin-bottom: 0.75rem;
	}

	.avatar {
		width: 2.5rem;
		height: 2.5rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.avatar-initial {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--color-text-muted);
		user-select: none;
	}

	.author-meta {
		display: flex;
		flex-direction: column;
		gap: 0.125rem;
	}

	.author-name {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text-primary);
		line-height: 1.2;
	}

	.author-username {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		line-height: 1.2;
	}

	.post-body {
		padding-left: 3.25rem;
		margin-bottom: 0.875rem;
	}

	.post-content {
		font-size: 0.9375rem;
		line-height: 1.55;
		color: var(--color-text-primary);
		margin: 0;
		white-space: pre-wrap;
		word-break: break-word;
	}

	.post-image {
		margin-top: 0.75rem;
		width: 100%;
		border-radius: 0.5rem;
		max-height: 28rem;
		object-fit: cover;
		display: block;
	}

	.post-footer {
		display: flex;
		align-items: center;
		gap: 1.25rem;
		padding-left: 3.25rem;
	}

	.action-btn {
		display: flex;
		align-items: center;
		gap: 0.3125rem;
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-muted);
		font-size: 0.8125rem;
		padding: 0.25rem;
		border-radius: 0.25rem;
		transition:
			color 0.15s ease,
			transform 0.1s ease;
	}

	.action-btn:hover:not(:disabled) {
		color: var(--color-text-primary);
	}

	.action-btn:disabled {
		cursor: default;
	}

	.action-btn:active:not(:disabled) {
		transform: scale(0.92);
	}

	.reaction-btn.liked {
		color: var(--color-accent);
	}

	.reaction-btn:hover:not(:disabled):not(.liked) {
		color: var(--color-accent);
	}

	@keyframes heartPop {
		0% {
			transform: scale(1);
		}
		40% {
			transform: scale(1.35);
		}
		100% {
			transform: scale(1);
		}
	}

	.reaction-btn.animating .heart-icon {
		animation: heartPop 0.25s ease-out;
	}

	.icon {
		width: 1rem;
		height: 1rem;
		flex-shrink: 0;
		transition: transform 0.15s ease;
	}

	.action-count {
		min-width: 0.75rem;
		text-align: left;
	}

	.post-timestamp {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		margin-left: auto;
	}

	.comments-panel {
		padding: 0.75rem 0 0 3.25rem;
		animation: fadeSlideDown 0.18s ease-out;
	}

	.comments-hint {
		font-size: 0.875rem;
		color: var(--color-text-muted);
		margin: 0;
	}

	@keyframes fadeSlideDown {
		from {
			opacity: 0;
			transform: translateY(-0.25rem);
		}
		to {
			opacity: 1;
			transform: translateY(0);
		}
	}
</style>
