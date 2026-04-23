<script lang="ts">
	import { untrack } from 'svelte';
	import { mobileExploreStore } from '$lib/stores/mobile-explore.store.svelte';
	import { UseSearch } from '$lib/features/explore/useSearch.svelte';
	import { getInitials } from '$lib/utils/getInitials';
	import { formatRelativeTime } from '$lib/utils/formatRelativeTime';
	import type { Post, User } from '$lib/types';

	interface Props {
		isActive: boolean;
	}

	let { isActive }: Props = $props();

	const search = new UseSearch();

	let query = $state(mobileExploreStore.query);
	let activeTab = $state(mobileExploreStore.tab);
	let inputEl = $state<HTMLInputElement | undefined>(undefined);

	const SKELETON_COUNT = 3;

	$effect(() => {
		mobileExploreStore.query = query;
		mobileExploreStore.tab = activeTab;
	});

	let loadedQuery: string | null = null;
	let loadedTab = activeTab;

	$effect(() => {
		const q = query;
		const tab = activeTab;

		const alreadyLoaded = untrack(() => loadedQuery === q && loadedTab === tab);
		if (alreadyLoaded) return;

		const queryChanged = untrack(() => q !== loadedQuery);
		if (queryChanged) {
			search.scheduleSearch(q, tab);
		} else {
			search.fetchResults(q, tab);
		}
		loadedQuery = q;
		loadedTab = tab;
	});

	$effect(() => {
		if (isActive) inputEl?.focus();
	});

	function infiniteScroll(el: HTMLElement): { destroy: () => void } {
		const obs = new IntersectionObserver(
			([entry]) => {
				if (entry?.isIntersecting && !search.postsLoadingMore) search.loadMorePosts();
			},
			{ threshold: 0.1 }
		);
		obs.observe(el);
		return { destroy: () => obs.disconnect() };
	}

	function handlePostTap(_post: Post): void {
		// Placeholder for post detail navigation when route is ready
	}

	function getPostInitials(user: User): string {
		return getInitials(user.name, user.username);
	}

	function truncateContent(content: string, maxLen = 120): string {
		return content.length <= maxLen ? content : `${content.slice(0, maxLen).trimEnd()}…`;
	}
</script>

<div class="explore-container">
	<div class="search-bar">
		<div class="search-wrapper">
			<svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
				<circle cx="11" cy="11" r="8" />
				<line x1="21" y1="21" x2="16.65" y2="16.65" />
			</svg>
			<input
				bind:this={inputEl}
				bind:value={query}
				class="search-input"
				type="search"
				placeholder="Search posts or people…"
				aria-label="Search"
				autocomplete="off"
				spellcheck="false"
			/>
			{#if query}
				<button class="search-clear" onclick={() => (query = '')} aria-label="Clear search" type="button">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
						<line x1="18" y1="6" x2="6" y2="18" />
						<line x1="6" y1="6" x2="18" y2="18" />
					</svg>
				</button>
			{/if}
		</div>

		<div class="tab-bar" role="tablist" aria-label="Content type">
			<button
				class="tab-btn"
				class:active={activeTab === 'posts'}
				role="tab"
				aria-selected={activeTab === 'posts'}
				type="button"
				onclick={() => (activeTab = 'posts')}
			>Posts</button>
			<button
				class="tab-btn"
				class:active={activeTab === 'people'}
				role="tab"
				aria-selected={activeTab === 'people'}
				type="button"
				onclick={() => (activeTab = 'people')}
			>People</button>
			<span
				class="tab-indicator"
				style:left={activeTab === 'posts' ? '0%' : '50%'}
				style:width="50%"
				aria-hidden="true"
			></span>
		</div>
	</div>

	<div class="content-area" role="tabpanel" aria-label={activeTab === 'posts' ? 'Posts results' : 'People results'}>
		{#if activeTab === 'posts'}
			{#if search.postsLoading}
				<div class="skeleton-list" aria-busy="true" aria-label="Loading posts">
					{#each { length: SKELETON_COUNT } as _, i (i)}
						<div class="post-skeleton" style:animation-delay="{i * 60}ms">
							<div class="post-skeleton-header">
								<div class="skeleton-avatar"></div>
								<div class="skeleton-meta">
									<div class="skeleton-line skeleton-name"></div>
									<div class="skeleton-line skeleton-time"></div>
								</div>
							</div>
							<div class="skeleton-line skeleton-body"></div>
							<div class="skeleton-line skeleton-body-short"></div>
						</div>
					{/each}
				</div>
			{:else if search.postsError}
				<div class="empty-state" role="alert">
					<div class="empty-icon-wrap">
						<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
							<circle cx="12" cy="12" r="10" />
							<line x1="12" y1="8" x2="12" y2="12" />
							<line x1="12" y1="16" x2="12.01" y2="16" />
						</svg>
					</div>
					<p class="empty-title">Something went wrong</p>
					<p class="empty-subtitle">{search.postsError}</p>
					<button class="retry-btn" type="button" onclick={() => search.fetchResults(query, activeTab)}>Try again</button>
				</div>
			{:else if search.posts.length === 0}
				<div class="empty-state">
					<div class="empty-icon-wrap empty-icon-dashed">
						<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
							<circle cx="11" cy="11" r="8" />
							<line x1="21" y1="21" x2="16.65" y2="16.65" />
						</svg>
					</div>
					<p class="empty-title">Nothing found</p>
					<p class="empty-subtitle">Try a different search</p>
				</div>
			{:else}
				<ul class="results-list" aria-label="Post results">
					{#each search.posts as post, i (post.id)}
						<li
							class="result-item post-item"
							style:animation-delay="{i * 30}ms"
							onclick={() => handlePostTap(post)}
							onkeydown={(e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); handlePostTap(post); } }}
							role="button"
							tabindex="0"
							aria-label="Post by {post.author.name ?? post.author.username}"
						>
							<div class="post-meta">
								<div class="post-avatar" aria-hidden="true">
									{#if post.author.avatarUrl}
										<img src={post.author.avatarUrl} alt={post.author.username} class="avatar-img" />
									{:else}
										<span class="avatar-initial">{getPostInitials(post.author)}</span>
									{/if}
								</div>
								<div class="post-author-info">
									<span class="author-name">{post.author.name ?? post.author.username}</span>
									<span class="post-time">{formatRelativeTime(post.createdAt)}</span>
								</div>
							</div>
							<p class="post-excerpt">{truncateContent(post.content)}</p>
						</li>
					{/each}
				</ul>

				{#if search.postsHasNext}
					<div class="explore-sentinel" use:infiniteScroll></div>
				{/if}
				{#if search.postsLoadingMore}
					<div class="loading-more-indicator">Loading…</div>
				{/if}
			{/if}

		{:else}
			<!-- People tab -->
			{#if search.usersState === 'loading'}
				<div class="skeleton-list" aria-busy="true" aria-label="Loading people">
					{#each { length: SKELETON_COUNT } as _, i (i)}
						<div class="user-skeleton" style:animation-delay="{i * 60}ms">
							<div class="skeleton-avatar"></div>
							<div class="skeleton-meta">
								<div class="skeleton-line skeleton-name"></div>
								<div class="skeleton-line skeleton-time"></div>
							</div>
						</div>
					{/each}
				</div>
			{:else if search.usersState === 'error'}
				<div class="empty-state" role="alert">
					<div class="empty-icon-wrap">
						<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
							<circle cx="12" cy="12" r="10" />
							<line x1="12" y1="8" x2="12" y2="12" />
							<line x1="12" y1="16" x2="12.01" y2="16" />
						</svg>
					</div>
					<p class="empty-title">Something went wrong</p>
					<p class="empty-subtitle">{search.usersError}</p>
					<button class="retry-btn" type="button" onclick={() => search.fetchResults(query, activeTab)}>Try again</button>
				</div>
			{:else if search.users.length === 0 && search.usersState === 'loaded'}
				<div class="empty-state">
					<div class="empty-icon-wrap empty-icon-dashed">
						<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
							<circle cx="11" cy="11" r="8" />
							<line x1="21" y1="21" x2="16.65" y2="16.65" />
						</svg>
					</div>
					<p class="empty-title">Nothing found</p>
					<p class="empty-subtitle">Try a different search</p>
				</div>
			{:else if search.users.length > 0}
				<ul class="results-list" aria-label="People results">
					{#each search.users as user, i (user.id)}
						<li class="result-item user-item" style:animation-delay="{i * 30}ms">
							<div class="user-avatar-wrap" aria-hidden="true">
								{#if user.avatarUrl}
									<img src={user.avatarUrl} alt={user.username} class="avatar-img" />
								{:else}
									<span class="avatar-initial">{getInitials(user.name, user.username)}</span>
								{/if}
							</div>
							<div class="user-info">
								<span class="user-display-name">{user.name ?? user.username}</span>
								<span class="user-handle">@{user.username}</span>
							</div>
							<button
								class="follow-btn"
								class:following={search.followStates.get(user.id) ?? false}
								type="button"
								onclick={() => search.toggleFollow(user)}
								aria-label="{(search.followStates.get(user.id) ?? false) ? 'Unfollow' : 'Follow'} {user.name ?? user.username}"
							>
								{(search.followStates.get(user.id) ?? false) ? 'Unfollow' : 'Follow'}
							</button>
						</li>
					{/each}
				</ul>
			{/if}
		{/if}
	</div>
</div>

<style>
	.explore-container {
		height: 100%;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
		padding-top: var(--tg-content-top, var(--tg-content-safe-area-inset-top, env(safe-area-inset-top, 0px)));
		padding-bottom: var(--content-bottom-padding, 104px);
	}

	.search-bar {
		position: sticky;
		top: 0;
		background: var(--color-bg, #0f0f0f);
		padding: 12px 16px 0;
		z-index: 10;
		backdrop-filter: blur(12px);
		-webkit-backdrop-filter: blur(12px);
	}

	.search-wrapper {
		position: relative;
		display: flex;
		align-items: center;
		margin-bottom: 10px;
	}

	.search-icon {
		position: absolute;
		left: 14px;
		top: 50%;
		transform: translateY(-50%);
		width: 16px;
		height: 16px;
		color: rgba(255, 255, 255, 0.4);
		pointer-events: none;
		flex-shrink: 0;
	}

	.search-input {
		width: 100%;
		height: 44px;
		border-radius: 22px;
		background: var(--color-surface-raised, #242424);
		border: 1px solid transparent;
		padding: 0 44px 0 44px;
		font-size: 15px;
		font-family: inherit;
		color: var(--color-text-primary, #f0f0f0);
		outline: none;
		box-sizing: border-box;
		transition: border-color 0.18s ease, box-shadow 0.18s ease;
		-webkit-appearance: none;
	}

	.search-input::placeholder { color: rgba(255, 255, 255, 0.35); }

	.search-input:focus {
		border-color: var(--color-accent, #e05252);
		box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-accent, #e05252) 15%, transparent);
	}

	.search-input::-webkit-search-cancel-button { display: none; }

	.search-clear {
		position: absolute;
		right: 10px;
		top: 50%;
		transform: translateY(-50%);
		background: none;
		border: none;
		cursor: pointer;
		color: rgba(255, 255, 255, 0.4);
		padding: 4px;
		display: flex;
		align-items: center;
		border-radius: 50%;
		transition: color 0.15s ease, background-color 0.15s ease;
	}

	.search-clear svg { width: 14px; height: 14px; }

	.search-clear:hover {
		color: var(--color-text-primary, #f0f0f0);
		background-color: rgba(255, 255, 255, 0.1);
	}

	.tab-bar {
		display: flex;
		position: relative;
		border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.08));
	}

	.tab-btn {
		flex: 1;
		text-align: center;
		padding: 10px 0;
		background: none;
		border: none;
		cursor: pointer;
		font-size: 14px;
		font-weight: 500;
		font-family: inherit;
		color: rgba(255, 255, 255, 0.5);
		transition: color 0.2s ease;
		position: relative;
		z-index: 1;
	}

	.tab-btn:hover { color: var(--color-text-primary, #f0f0f0); }
	.tab-btn.active { color: var(--color-text-primary, #f0f0f0); font-weight: 600; }

	.tab-indicator {
		position: absolute;
		bottom: -1px;
		height: 2px;
		background: var(--tg-accent, #e05252);
		border-radius: 1px 1px 0 0;
		transition: left 0.2s ease, width 0.2s ease;
	}

	.content-area { padding-top: 4px; }

	.results-list {
		list-style: none;
		margin: 0;
		padding: 0;
		display: flex;
		flex-direction: column;
	}

	.result-item { animation: fadeSlideUp 0.25s ease both; }

	.post-item {
		padding: 14px 16px;
		border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.06));
		cursor: pointer;
		outline: none;
		transition: background-color 0.15s ease;
	}

	.post-item:hover,
	.post-item:focus-visible { background-color: var(--color-surface-raised, #1a1a1a); }

	.post-item:focus-visible { box-shadow: inset 0 0 0 2px var(--color-accent, #e05252); }

	.post-meta {
		display: flex;
		align-items: center;
		gap: 10px;
		margin-bottom: 8px;
	}

	.post-author-info {
		display: flex;
		align-items: baseline;
		gap: 6px;
		min-width: 0;
	}

	.author-name {
		font-size: 14px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.post-time {
		font-size: 12px;
		color: rgba(255, 255, 255, 0.4);
		flex-shrink: 0;
	}

	.post-excerpt {
		font-size: 14px;
		line-height: 1.5;
		color: rgba(255, 255, 255, 0.75);
		margin: 0;
		display: -webkit-box;
		-webkit-line-clamp: 2;
		line-clamp: 2;
		-webkit-box-orient: vertical;
		overflow: hidden;
	}

	.user-item {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 12px 16px;
		min-height: 48px;
		border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.06));
	}

	.user-avatar-wrap {
		width: 40px;
		height: 40px;
		border-radius: 50%;
		background: var(--color-surface-raised, #242424);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.post-avatar {
		width: 32px;
		height: 32px;
		border-radius: 50%;
		background: var(--color-surface-raised, #242424);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.avatar-img { width: 100%; height: 100%; object-fit: cover; }

	.avatar-initial {
		font-size: 13px;
		font-weight: 600;
		color: rgba(255, 255, 255, 0.5);
		user-select: none;
	}

	.user-info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	.user-display-name {
		font-size: 14px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.user-handle { font-size: 12px; color: rgba(255, 255, 255, 0.4); }

	.follow-btn {
		background: var(--tg-accent, #e05252);
		color: white;
		border: none;
		border-radius: 16px;
		padding: 6px 14px;
		font-size: 13px;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		flex-shrink: 0;
		transition: opacity 0.15s ease, background-color 0.2s ease;
		white-space: nowrap;
	}

	.follow-btn:hover { opacity: 0.85; }

	.follow-btn.following {
		background: rgba(255, 255, 255, 0.08);
		color: rgba(255, 255, 255, 0.7);
	}

	.follow-btn.following:hover { background: rgba(255, 255, 255, 0.12); opacity: 1; }

	.skeleton-list { display: flex; flex-direction: column; padding-top: 4px; }

	.post-skeleton {
		padding: 14px 16px;
		border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.06));
		animation: fadeSlideUp 0.22s ease both;
	}

	.post-skeleton-header {
		display: flex;
		align-items: center;
		gap: 10px;
		margin-bottom: 10px;
	}

	.user-skeleton {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 12px 16px;
		border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.06));
		animation: fadeSlideUp 0.22s ease both;
	}

	.skeleton-avatar {
		width: 32px;
		height: 32px;
		border-radius: 50%;
		background: var(--color-surface-raised, #242424);
		flex-shrink: 0;
		animation: shimmer 1.4s ease-in-out infinite;
	}

	.user-skeleton .skeleton-avatar { width: 40px; height: 40px; }

	.skeleton-meta { flex: 1; display: flex; flex-direction: column; gap: 6px; }

	.skeleton-line {
		border-radius: 4px;
		background: var(--color-surface-raised, #242424);
		animation: shimmer 1.4s ease-in-out infinite;
	}

	.skeleton-name { height: 13px; width: 40%; }
	.skeleton-time { height: 11px; width: 20%; animation-delay: 0.1s; }
	.skeleton-body { height: 13px; width: 90%; animation-delay: 0.05s; }
	.skeleton-body-short { height: 13px; width: 65%; margin-top: 6px; animation-delay: 0.15s; }

	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 60px 24px;
		gap: 16px;
		color: rgba(255, 255, 255, 0.4);
		text-align: center;
	}

	.empty-icon-wrap {
		width: 48px;
		height: 48px;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		background: rgba(255, 255, 255, 0.04);
	}

	.empty-icon-wrap svg { width: 22px; height: 22px; color: rgba(255, 255, 255, 0.3); }
	.empty-icon-dashed { border: 1.5px dashed rgba(255, 255, 255, 0.2); background: none; }
	.empty-title { font-size: 15px; font-weight: 600; color: var(--color-text-primary, #f0f0f0); margin: 0; }
	.empty-subtitle { font-size: 13px; color: rgba(255, 255, 255, 0.4); margin: 0; margin-top: -8px; }

	.retry-btn {
		margin-top: 4px;
		padding: 8px 20px;
		border-radius: 8px;
		border: 1px solid var(--color-border, rgba(255, 255, 255, 0.12));
		background: none;
		color: var(--color-text-primary, #f0f0f0);
		font-size: 14px;
		font-weight: 500;
		font-family: inherit;
		cursor: pointer;
		transition: background-color 0.15s ease, transform 0.1s ease;
	}

	.retry-btn:hover { background-color: var(--color-surface-raised, #242424); }
	.retry-btn:active { transform: scale(0.97); }

	.explore-sentinel { height: 1px; }

	.loading-more-indicator {
		padding: 16px;
		text-align: center;
		font-size: 13px;
		color: rgba(255, 255, 255, 0.5);
	}

	@keyframes fadeSlideUp {
		from { opacity: 0; transform: translateY(12px); }
		to { opacity: 1; transform: translateY(0); }
	}

	@keyframes shimmer {
		0%, 100% { opacity: 0.5; }
		50% { opacity: 1; }
	}
</style>
