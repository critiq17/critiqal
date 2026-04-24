<script lang="ts">
	import type { UseSearch } from '$lib/features/explore/useSearch.svelte';
	import { getInitials } from '$lib/utils/getInitials';
	import ExploreEmptyState from './ExploreEmptyState.svelte';

	const SKELETON_COUNT = 3;

	interface Props {
		search: UseSearch;
		onRetry: () => void;
	}

	let { search, onRetry }: Props = $props();
</script>

{#if search.usersState === 'loading'}
	<div class="skeleton-list" aria-busy="true" aria-label="Loading people">
		{#each { length: SKELETON_COUNT } as _, i (i)}
			<div class="user-skeleton" style:animation-delay="{i * 60}ms">
				<div class="skeleton-avatar"></div>
				<div class="skeleton-meta">
					<div class="skeleton-line skeleton-name"></div>
					<div class="skeleton-line skeleton-handle"></div>
				</div>
			</div>
		{/each}
	</div>
{:else if search.usersState === 'error'}
	<ExploreEmptyState variant="error" message={search.usersError} onRetry={onRetry} />
{:else if search.users.length === 0 && search.usersState === 'loaded'}
	<ExploreEmptyState variant="empty" message="Try a different search" />
{:else if search.users.length > 0}
	<ul class="results-list" aria-label="People results">
		{#each search.users as user, i (user.id)}
			<li class="user-item" style:animation-delay="{i * 30}ms">
				<div class="user-avatar" aria-hidden="true">
					{#if user.avatarUrl}
						<img src={user.avatarUrl} alt={user.username} class="avatar-img" />
					{:else}
						<span class="avatar-initial">{getInitials(user.name, user.username)}</span>
					{/if}
				</div>
				<div class="user-info">
					<span class="display-name">{user.name ?? user.username}</span>
					<span class="handle">@{user.username}</span>
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

<style>
	.results-list {
		list-style: none;
		margin: 0;
		padding: 0;
		display: flex;
		flex-direction: column;
	}

	.user-item {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 12px 16px;
		min-height: 48px;
		border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.06));
		animation: fadeSlideUp 0.25s ease both;
	}

	.user-avatar {
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

	.display-name {
		font-size: 14px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.handle { font-size: 12px; color: rgba(255, 255, 255, 0.4); }

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

	.user-skeleton {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 12px 16px;
		border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.06));
		animation: fadeSlideUp 0.22s ease both;
	}

	.skeleton-avatar {
		width: 40px;
		height: 40px;
		border-radius: 50%;
		background: var(--color-surface-raised, #242424);
		flex-shrink: 0;
		animation: shimmer 1.4s ease-in-out infinite;
	}

	.skeleton-meta { flex: 1; display: flex; flex-direction: column; gap: 6px; }

	.skeleton-line {
		border-radius: 4px;
		background: var(--color-surface-raised, #242424);
		animation: shimmer 1.4s ease-in-out infinite;
	}

	.skeleton-name { height: 13px; width: 40%; }
	.skeleton-handle { height: 11px; width: 25%; animation-delay: 0.1s; }

	@keyframes fadeSlideUp {
		from { opacity: 0; transform: translateY(12px); }
		to { opacity: 1; transform: translateY(0); }
	}

	@keyframes shimmer {
		0%, 100% { opacity: 0.5; }
		50% { opacity: 1; }
	}
</style>
