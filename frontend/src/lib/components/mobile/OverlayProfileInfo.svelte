<script lang="ts">
	import type { User } from '$lib/types';

	interface Props {
		profile: User;
		followerCount: number;
		followingCount: number;
		postsCount: number;
		isOwnProfile: boolean;
		isFollowing: boolean;
		isTogglingFollow: boolean;
		onToggleFollow: () => void;
	}

	let {
		profile,
		followerCount,
		followingCount,
		postsCount,
		isOwnProfile,
		isFollowing,
		isTogglingFollow,
		onToggleFollow
	}: Props = $props();

	function formatCount(n: number): string {
		if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
		if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
		return String(n);
	}

	function getInitial(user: User): string {
		return (user.name ?? user.username).charAt(0).toUpperCase();
	}
</script>

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
			onclick={onToggleFollow}
			disabled={isTogglingFollow}
			aria-label={isFollowing ? 'Unfollow' : 'Follow'}
		>
			{isFollowing ? 'Following' : 'Follow'}
		</button>
	{/if}
</section>

<div class="stats-row" role="group" aria-label="Profile stats">
	<div class="stat-item">
		<span class="stat-value">{formatCount(postsCount)}</span>
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

<style>
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

	.follow-btn:disabled { opacity: 0.6; cursor: default; }

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
</style>
