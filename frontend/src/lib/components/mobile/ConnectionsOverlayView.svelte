<script lang="ts">
	import { onMount } from 'svelte';
	import type { User } from '$lib/types';
	import { userService } from '$lib/services/user.service';
	import { navStack } from '$lib/stores/nav-stack.store.svelte';
	import { getInitials } from '$lib/utils/getInitials';
	import OverlayBackHeader from './OverlayBackHeader.svelte';

	interface Props {
		username: string;
		tab: 'followers' | 'following';
		onBack: () => void;
	}

	let { username, tab, onBack }: Props = $props();

	let list = $state<User[]>([]);
	let loading = $state(true);
	let error = $state<string | null>(null);

	async function load(): Promise<void> {
		loading = true;
		error = null;
		try {
			const user = await userService.getProfile(username);
			list =
				tab === 'followers'
					? await userService.getFollowers(user.id)
					: await userService.getFollowing(user.id);
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load';
		} finally {
			loading = false;
		}
	}

	onMount(load);
</script>

<OverlayBackHeader title={tab === 'followers' ? 'Followers' : 'Following'} {onBack} />

<div class="scroll-area mobile-scroll-container">
	{#if loading}
		{#each { length: 6 } as _, i (i)}
			<div class="user-skeleton">
				<div class="skeleton-avatar"></div>
				<div class="skeleton-lines">
					<div class="skeleton-line" style="width:110px"></div>
					<div class="skeleton-line" style="width:72px;opacity:0.6"></div>
				</div>
			</div>
		{/each}
	{:else if error}
		<div class="empty-state" role="alert">
			<p class="empty-text">{error}</p>
			<button class="retry-btn" type="button" onclick={load}>Try again</button>
		</div>
	{:else if list.length === 0}
		<div class="empty-state">
			<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor"
				stroke-width="1.25" stroke-linecap="round" stroke-linejoin="round"
				style="color:rgba(255,255,255,0.18)" aria-hidden="true">
				<path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
				<circle cx="9" cy="7" r="4"/>
				<path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/>
			</svg>
			<p class="empty-text">
				{tab === 'followers' ? 'No followers yet' : 'Not following anyone'}
			</p>
		</div>
	{:else}
		<ul class="user-list" role="list">
			{#each list as user (user.id)}
				<li role="listitem">
					<button class="user-row" type="button" onclick={() => navStack.pushProfile(user.username)}>
						<div class="user-avatar">
							{#if user.avatarUrl}
								<img src={user.avatarUrl} alt="" class="avatar-img" />
							{:else}
								<span class="avatar-initial">{getInitials(user.name, user.username)}</span>
							{/if}
						</div>
						<div class="user-info">
							<span class="user-name">{user.name ?? user.username}</span>
							<span class="user-handle">@{user.username}</span>
						</div>
						<svg class="row-chevron" width="14" height="14" viewBox="0 0 24 24" fill="none"
							stroke="currentColor" stroke-width="2" aria-hidden="true">
							<polyline points="9 18 15 12 9 6" />
						</svg>
					</button>
				</li>
			{/each}
		</ul>
	{/if}
	<div class="safe-bottom" aria-hidden="true"></div>
</div>

<style>
	.scroll-area {
		flex: 1;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior: contain;
	}

	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 12px;
		padding: 60px 16px;
	}

	.empty-text {
		font-size: 14px;
		color: var(--text-faint);
		margin: 0;
	}

	.retry-btn {
		padding: 8px 20px;
		border-radius: 10px;
		border: 1px solid var(--surface-tint-medium);
		background: none;
		color: var(--text-secondary-2);
		font-size: 14px;
		cursor: pointer;
		font-family: inherit;
	}

	.user-list {
		list-style: none;
		padding: 0;
		margin: 0;
	}

	.user-row {
		display: flex;
		align-items: center;
		gap: 12px;
		width: 100%;
		padding: 12px 16px;
		background: none;
		border: none;
		border-bottom: 1px solid var(--surface-tint-subtle);
		text-align: left;
		font-family: inherit;
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
		transition: background-color 0.12s;
	}

	.user-row:active { background: var(--surface-tint-subtle); }

	.user-avatar {
		width: 42px;
		height: 42px;
		border-radius: 50%;
		background: var(--surface-tint-soft);
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
		flex-shrink: 0;
	}

	.avatar-img { width: 100%; height: 100%; object-fit: cover; display: block; }

	.avatar-initial {
		font-size: 15px;
		font-weight: 600;
		color: var(--text-tertiary);
		user-select: none;
	}

	.user-info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	.user-name {
		font-size: 15px;
		font-weight: 500;
		color: var(--tg-text, #f0f0f0);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.user-handle {
		font-size: 12px;
		color: var(--text-quaternary);
	}

	.row-chevron {
		color: var(--text-ghost);
		flex-shrink: 0;
	}

	.user-skeleton {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 12px 16px;
		border-bottom: 1px solid var(--surface-tint-subtle);
	}

	.skeleton-avatar {
		width: 42px;
		height: 42px;
		border-radius: 50%;
		background: var(--surface-tint-soft);
		flex-shrink: 0;
		animation: pulse 1.4s ease-in-out infinite;
	}

	.skeleton-lines {
		display: flex;
		flex-direction: column;
		gap: 7px;
		flex: 1;
	}

	.skeleton-line {
		height: 12px;
		border-radius: 4px;
		background: var(--surface-tint-soft);
		animation: pulse 1.4s ease-in-out infinite;
	}

	@keyframes pulse {
		0%, 100% { opacity: 1; }
		50% { opacity: 0.4; }
	}

	.safe-bottom {
		height: calc(16px + env(safe-area-inset-bottom, 0px));
	}

	@media (prefers-reduced-motion: reduce) {
		.skeleton-avatar,
		.skeleton-line { animation: none; }
	}
</style>
