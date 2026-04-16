<script lang="ts">
	import { onMount } from 'svelte';
	import type { User, Post } from '$lib/types';
	import { userService } from '$lib/services/user.service';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import { closeProfile } from '$lib/stores/profile-nav.store';

	interface Props {
		username: string;
	}

	let { username }: Props = $props();

	let profile = $state<User | null>(null);
	let posts = $state<Post[]>([]);
	let isLoading = $state(true);
	let error = $state<string | null>(null);

	let isFollowing = $state(false);
	let followerCount = $state(0);
	let followingCount = $state(0);
	let isTogglingFollow = $state(false);

	let isOwnProfile = $derived(authStore.user?.username === username);

	function formatCount(n: number): string {
		if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
		if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
		return String(n);
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

	function getInitial(user: User): string {
		return (user.name ?? user.username).charAt(0).toUpperCase();
	}

	async function load(): Promise<void> {
		isLoading = true;
		error = null;
		try {
			const [user, userPosts, followers, following] = await Promise.all([
				userService.getProfile(username),
				userService.getUserPosts(username),
				userService.getFollowers(0).catch(() => [] as User[]),
				userService.getFollowing(0).catch(() => [] as User[])
			]);

			profile = user;
			posts = userPosts;

			const [followersList, followingList] = await Promise.all([
				userService.getFollowers(user.id).catch(() => [] as User[]),
				userService.getFollowing(user.id).catch(() => [] as User[])
			]);

			followerCount = followersList.length;
			followingCount = followingList.length;

			if (authStore.user) {
				isFollowing = followersList.some((f) => f.id === authStore.user!.id);
			}
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load profile';
		} finally {
			isLoading = false;
		}
	}

	async function toggleFollow(): Promise<void> {
		if (!profile || isTogglingFollow) return;
		getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
		isTogglingFollow = true;
		const wasFollowing = isFollowing;
		isFollowing = !wasFollowing;
		followerCount += wasFollowing ? -1 : 1;
		try {
			if (wasFollowing) {
				await userService.unfollow(profile.id);
			} else {
				await userService.follow(profile.id);
			}
		} catch {
			isFollowing = wasFollowing;
			followerCount += wasFollowing ? 1 : -1;
		} finally {
			isTogglingFollow = false;
		}
	}

	function handleBack(): void {
		getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
		closeProfile();
	}

	// Wire Telegram BackButton
	$effect(() => {
		const tg = getTelegramWebApp();
		if (!tg) return;

		tg.BackButton.show();
		tg.BackButton.onClick(handleBack);

		return () => {
			tg.BackButton.offClick(handleBack);
			tg.BackButton.hide();
		};
	});

	onMount(() => {
		load();
	});
</script>

<div class="overlay" role="dialog" aria-label={`${username}'s profile`}>
	<!-- Header bar -->
	<div class="overlay-header">
		<button class="back-btn" onclick={handleBack} aria-label="Go back">
			<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
				<polyline points="15 18 9 12 15 6"/>
			</svg>
		</button>
		<span class="header-username">{username}</span>
		<div class="header-spacer"></div>
	</div>

	<!-- Scrollable content -->
	<div class="overlay-scroll">
		{#if isLoading}
			<div class="loading-state" aria-busy="true">
				<div class="skel skel-avatar"></div>
				<div class="skel skel-name"></div>
				<div class="skel skel-bio"></div>
			</div>

		{:else if error}
			<div class="error-state" role="alert">
				<p>{error}</p>
				<button class="retry-btn" onclick={load}>Try again</button>
			</div>

		{:else if profile}
			<!-- Profile header -->
			<section class="profile-section">
				<div class="avatar-wrap">
					{#if profile.avatarUrl}
						<img src={profile.avatarUrl} alt={profile.username} class="avatar" />
					{:else}
						<div class="avatar avatar-fallback">{getInitial(profile)}</div>
					{/if}
				</div>

				<div class="identity">
					{#if profile.name}
						<span class="display-name">{profile.name}</span>
					{/if}
					<span class="user-username">@{profile.username}</span>
					{#if profile.bio}
						<p class="bio">{profile.bio}</p>
					{/if}
				</div>

				{#if !isOwnProfile}
					<button
						class="follow-btn"
						class:following={isFollowing}
						onclick={toggleFollow}
						disabled={isTogglingFollow}
						aria-label={isFollowing ? 'Unfollow' : 'Follow'}
					>
						{isFollowing ? 'Following' : 'Follow'}
					</button>
				{/if}
			</section>

			<!-- Stats row -->
			<div class="stats-row" role="group" aria-label="Profile stats">
				<div class="stat-item">
					<span class="stat-value">{formatCount(posts.length)}</span>
					<span class="stat-label">Posts</span>
				</div>
				<div class="stat-item">
					<span class="stat-value">{formatCount(followerCount)}</span>
					<span class="stat-label">Followers</span>
				</div>
				<div class="stat-item">
					<span class="stat-value">{formatCount(followingCount)}</span>
					<span class="stat-label">Following</span>
				</div>
			</div>

			<!-- Posts -->
			{#if posts.length === 0}
				<div class="empty-posts">
					<p>No posts yet</p>
				</div>
			{:else}
				<div class="posts-list">
					{#each posts as post (post.id)}
						<article class="post-card">
							<div class="post-card-inner">
								<div class="post-meta">
									<span class="post-time">{formatRelativeTime(post.createdAt)}</span>
								</div>
								<p class="post-content">{post.content}</p>
								{#if post.photos && post.photos.length > 0}
									<div class="post-photos">
										{#each post.photos.sort((a, b) => a.position - b.position).slice(0, 1) as photo (photo.id)}
											<img src={photo.url} alt="Post photo" class="post-photo" loading="lazy" />
										{/each}
										{#if post.photos.length > 1}
											<span class="photo-count">+{post.photos.length - 1}</span>
										{/if}
									</div>
								{/if}
							</div>
						</article>
					{/each}
				</div>
			{/if}
		{/if}
	</div>
</div>

<style>
	.overlay {
		position: fixed;
		inset: 0;
		z-index: 200;
		background: var(--tg-bg, #0f0f0f);
		display: flex;
		flex-direction: column;
		animation: slideIn 0.28s cubic-bezier(0.4, 0, 0.2, 1) both;
	}

	@keyframes slideIn {
		from { transform: translateX(100%); }
		to   { transform: translateX(0); }
	}

	/* Header */
	.overlay-header {
		display: flex;
		align-items: center;
		gap: 8px;
		padding: calc(var(--tg-content-top, env(safe-area-inset-top, 0px)) + 12px) 16px 12px;
		border-bottom: 1px solid rgba(255, 255, 255, 0.06);
		background: var(--tg-bg, #0f0f0f);
	}

	.back-btn {
		background: none;
		border: none;
		padding: 4px;
		cursor: pointer;
		color: var(--tg-text, #f0f0f0);
		display: flex;
		align-items: center;
		justify-content: center;
		min-width: 36px;
		min-height: 36px;
		margin-left: -4px;
	}

	.header-username {
		font-size: 16px;
		font-weight: 600;
		color: var(--tg-text, #f0f0f0);
	}

	.header-spacer {
		flex: 1;
	}

	/* Scroll container */
	.overlay-scroll {
		flex: 1;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
		padding-bottom: 32px;
	}

	/* Profile section */
	.profile-section {
		padding: 24px 20px 16px;
		display: flex;
		flex-direction: column;
		align-items: center;
		text-align: center;
		gap: 12px;
	}

	.avatar-wrap {
		width: 80px;
		height: 80px;
		border-radius: 50%;
		overflow: hidden;
		flex-shrink: 0;
	}

	.avatar {
		width: 100%;
		height: 100%;
		object-fit: cover;
		border-radius: 50%;
	}

	.avatar-fallback {
		display: flex;
		align-items: center;
		justify-content: center;
		background: rgba(255, 255, 255, 0.08);
		color: var(--tg-text, #f0f0f0);
		font-size: 28px;
		font-weight: 600;
	}

	.identity {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 2px;
	}

	.display-name {
		font-size: 20px;
		font-weight: 700;
		color: var(--tg-text, #f0f0f0);
	}

	.user-username {
		font-size: 14px;
		color: var(--tg-hint, rgba(240, 240, 240, 0.5));
	}

	.bio {
		font-size: 14px;
		color: var(--tg-text, #f0f0f0);
		opacity: 0.8;
		margin: 4px 0 0;
		line-height: 1.4;
	}

	/* Follow button */
	.follow-btn {
		padding: 8px 32px;
		border-radius: 20px;
		border: none;
		font-size: 14px;
		font-weight: 600;
		cursor: pointer;
		transition: all 0.15s ease;
		background: var(--tg-accent, #e05252);
		color: var(--tg-btn-text, #fff);
	}

	.follow-btn.following {
		background: rgba(255, 255, 255, 0.08);
		color: var(--tg-text, #f0f0f0);
	}

	.follow-btn:disabled {
		opacity: 0.6;
		cursor: default;
	}

	/* Stats */
	.stats-row {
		display: flex;
		justify-content: center;
		gap: 32px;
		padding: 8px 20px 16px;
		border-bottom: 1px solid rgba(255, 255, 255, 0.06);
	}

	.stat-item {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 2px;
	}

	.stat-value {
		font-size: 18px;
		font-weight: 700;
		color: var(--tg-text, #f0f0f0);
	}

	.stat-label {
		font-size: 11px;
		color: var(--tg-hint, rgba(240, 240, 240, 0.5));
		text-transform: uppercase;
		letter-spacing: 0.04em;
	}

	/* Posts */
	.posts-list {
		display: flex;
		flex-direction: column;
	}

	.post-card {
		border-bottom: 1px solid rgba(255, 255, 255, 0.05);
	}

	.post-card-inner {
		padding: 14px 16px;
	}

	.post-meta {
		margin-bottom: 6px;
	}

	.post-time {
		font-size: 12px;
		color: var(--tg-hint, rgba(240, 240, 240, 0.5));
	}

	.post-content {
		font-size: 15px;
		color: var(--tg-text, #f0f0f0);
		line-height: 1.5;
		margin: 0;
		white-space: pre-wrap;
		word-break: break-word;
	}

	.post-photos {
		margin-top: 10px;
		position: relative;
		border-radius: 12px;
		overflow: hidden;
	}

	.post-photo {
		width: 100%;
		max-height: 240px;
		object-fit: cover;
		display: block;
	}

	.photo-count {
		position: absolute;
		bottom: 8px;
		right: 8px;
		background: rgba(0, 0, 0, 0.6);
		color: #fff;
		font-size: 12px;
		font-weight: 600;
		padding: 3px 8px;
		border-radius: 10px;
	}

	.empty-posts {
		padding: 40px 16px;
		text-align: center;
		color: var(--tg-hint, rgba(240, 240, 240, 0.5));
		font-size: 14px;
	}

	/* Loading skeleton */
	.loading-state {
		padding: 32px 20px;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 12px;
	}

	.skel {
		background: rgba(255, 255, 255, 0.06);
		border-radius: 8px;
		animation: pulse 1.4s ease-in-out infinite;
	}

	.skel-avatar {
		width: 80px;
		height: 80px;
		border-radius: 50%;
	}

	.skel-name {
		width: 140px;
		height: 20px;
	}

	.skel-bio {
		width: 200px;
		height: 14px;
	}

	@keyframes pulse {
		0%, 100% { opacity: 0.4; }
		50%       { opacity: 0.7; }
	}

	/* Error */
	.error-state {
		padding: 40px 20px;
		text-align: center;
		color: #e05252;
		font-size: 14px;
	}

	.retry-btn {
		margin-top: 12px;
		padding: 8px 24px;
		border-radius: 20px;
		background: rgba(255, 255, 255, 0.08);
		border: none;
		color: var(--tg-text, #f0f0f0);
		cursor: pointer;
		font-size: 14px;
	}
</style>
