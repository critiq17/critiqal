<script lang="ts">
	import { onMount } from 'svelte';
	import { fly, fade } from 'svelte/transition';
	import { getTelegramWebApp } from '$lib/telegram';
	import { openMobileComments } from '$lib/stores/mobile-comments.store';
	import { tabStore } from '$lib/stores/mobile-tab.store.svelte';
	import { stravaStore } from '$lib/stores/strava.store.svelte';
	import { openSettings } from '$lib/stores/settings-nav.store.svelte';
	import { UseProfile } from '$lib/features/profile/useProfile.svelte';
	import { formatCount } from '$lib/utils/formatCount';
	import ProfilePostsList from '$lib/components/profile/ProfilePostsList.svelte';
	import FollowersOverlay from '$lib/components/profile/FollowersOverlay.svelte';
	import ProfileEditOverlay from '$lib/components/profile/ProfileEditOverlay.svelte';
	import ProfileStravaWidget from '$lib/components/profile/ProfileStravaWidget.svelte';
	import ProfileShareButton from '$lib/components/profile/ProfileShareButton.svelte';
	import ProfileInlineStats from '$lib/components/profile/ProfileInlineStats.svelte';
	import ProfileTabs from '$lib/components/profile/ProfileTabs.svelte';
	import ProfileEmptyPosts from '$lib/components/profile/ProfileEmptyPosts.svelte';

	const profile = new UseProfile();

	let followersOverlayType = $state<'followers' | 'following' | null>(null);
	let stravaNotification = $state<'connected' | 'denied' | null>(null);
	let editOpen = $state(false);
	let fileInputEl = $state<HTMLInputElement | null>(null);

	const tabs = $derived([
		{ id: 'posts', label: 'Posts', count: profile.postsCount ?? profile.posts.length },
	] as const);

	const displayName = $derived(
		profile.profile?.name ?? profile.profile?.username ?? ''
	);
	const avatarInitial = $derived(
		(profile.profile?.name ?? profile.profile?.username ?? '?')
			.charAt(0)
			.toUpperCase()
	);

	function openFollowersOverlay(type: 'followers' | 'following'): void {
		followersOverlayType = type;
		if (!profile.listsLoaded && !profile.listsLoading) profile.loadFollowLists();
	}

	function closeFollowersOverlay(): void {
		followersOverlayType = null;
	}

	function openComments(postId: string): void {
		openMobileComments(postId);
	}

	async function handleAvatarChange(e: Event): Promise<void> {
		const input = e.target as HTMLInputElement;
		const file = input.files?.[0];
		if (!file) return;
		await profile.uploadAvatar(file);
		input.value = '';
	}

	function pickAvatar(): void {
		fileInputEl?.click();
	}

	function startEdit(): void {
		profile.startEdit();
		editOpen = true;
	}

	$effect(() => {
		// On the profile tab, the Telegram BackButton is owned by overlays only.
		const tg = getTelegramWebApp();
		if (tg) tg.BackButton.hide();
	});

	onMount(() => {
		profile.load();
		stravaStore.load();

		const params = new URLSearchParams(window.location.search);
		const stravaResult = params.get('strava');
		if (stravaResult === 'connected' || stravaResult === 'denied') {
			tabStore.active = 'profile';
			history.replaceState({}, '', '/');
			stravaNotification = stravaResult as 'connected' | 'denied';
			setTimeout(() => {
				stravaNotification = null;
			}, 3500);
		}
	});
</script>

<input
	bind:this={fileInputEl}
	type="file"
	accept="image/jpeg,image/png,image/webp"
	class="sr-only"
	onchange={handleAvatarChange}
	disabled={profile.isUploadingAvatar}
	aria-hidden="true"
	tabindex="-1"
/>

<div class="profile-container">
	{#if stravaNotification}
		<div
			class="strava-toast"
			class:strava-toast--success={stravaNotification === 'connected'}
			class:strava-toast--denied={stravaNotification === 'denied'}
			role="status"
			aria-live="polite"
			in:fly={{ y: -16, duration: 280 }}
			out:fade={{ duration: 200 }}
		>
			{#if stravaNotification === 'connected'}
				<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
					<polyline points="20 6 9 17 4 12" />
				</svg>
				Strava connected!
			{:else}
				<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
					<line x1="18" y1="6" x2="6" y2="18" />
					<line x1="6" y1="6" x2="18" y2="18" />
				</svg>
				Connection cancelled
			{/if}
		</div>
	{/if}

	<header class="top-bar">
		<div class="top-bar__title">
			<h1 class="top-bar__name">{displayName || ' '}</h1>
			{#if profile.profile}
				<span class="top-bar__sub">
					{formatCount(profile.postsCount ?? profile.posts.length)} posts
				</span>
			{/if}
		</div>
		<button
			class="icon-btn"
			type="button"
			onclick={openSettings}
			aria-label="Settings"
		>
			<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
				<circle cx="12" cy="12" r="3" />
				<path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z" />
			</svg>
		</button>
	</header>

	{#if profile.isLoading && !profile.profile}
		<div class="skeleton" aria-busy="true" aria-label="Loading profile">
			<div class="skeleton-avatar"></div>
			<div class="skeleton-info">
				<div class="skeleton-line" style="width:9rem"></div>
				<div class="skeleton-line skeleton-line--sm" style="width:5rem"></div>
				<div class="skeleton-line skeleton-line--sm" style="width:90%"></div>
			</div>
		</div>
	{:else if profile.profileError && !profile.profile}
		<div class="error-state" role="alert">
			<p class="error-text">{profile.profileError}</p>
			<button class="retry-btn" onclick={() => profile.load()}>Try again</button>
		</div>
	{:else if profile.profile}
		<section class="identity-card" aria-label="Profile info">
			<button
				class="avatar-btn"
				type="button"
				onclick={pickAvatar}
				aria-label="Change profile photo"
				disabled={profile.isUploadingAvatar}
			>
				{#if profile.profile.avatarUrl}
					<img src={profile.profile.avatarUrl} alt="" class="avatar-img" />
				{:else}
					<span class="avatar-fallback">{avatarInitial}</span>
				{/if}
				{#if profile.isUploadingAvatar}
					<span class="avatar-overlay" aria-hidden="true">
						<svg class="spinner" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
							<path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
						</svg>
					</span>
				{/if}
			</button>

			<div class="identity-body">
				<div class="identity-top">
					<div class="identity-names">
						<p class="display-name">{displayName}</p>
						<p class="username">@{profile.profile.username}</p>
					</div>
					<div class="identity-actions">
						<ProfileShareButton
							username={profile.profile.username}
							displayName={profile.profile.name}
						/>
						<button class="btn btn-outline" type="button" onclick={startEdit}>
							Edit profile
						</button>
					</div>
				</div>

				{#if profile.profile.bio}
					<p class="bio">{profile.profile.bio}</p>
				{/if}

				{#if profile.avatarError}
					<p class="avatar-error" role="alert">{profile.avatarError}</p>
				{/if}

				<ProfileInlineStats
					postsCount={profile.postsCount}
					followersCount={profile.followersCount}
					followingCount={profile.followingCount}
					onOpenFollowers={() => openFollowersOverlay('followers')}
					onOpenFollowing={() => openFollowersOverlay('following')}
				/>
			</div>
		</section>

		{#if stravaStore.connection}
			<div class="strava-slot">
				<ProfileStravaWidget isOwnProfile={true} />
			</div>
		{/if}

		<div class="tabs-wrap">
			<ProfileTabs tabs={tabs} activeId="posts" />
		</div>

		<div
			id="profile-tabpanel-posts"
			role="tabpanel"
			aria-labelledby="profile-tab-posts"
			tabindex="0"
			class="tabpanel"
		>
			{#if profile.posts.length === 0 && !profile.postsLoading}
				<ProfileEmptyPosts isOwnProfile={true} />
			{:else}
				<ProfilePostsList
					posts={profile.posts}
					postsLoading={profile.postsLoading}
					postsError={profile.profileError}
					onOpenComments={openComments}
					onRetry={() => profile.load()}
				/>
			{/if}
		</div>
	{/if}
</div>

<FollowersOverlay
	open={followersOverlayType !== null}
	type={followersOverlayType}
	list={followersOverlayType === 'followers' ? profile.followersList : profile.followingList}
	listsLoading={profile.listsLoading}
	onClose={closeFollowersOverlay}
/>

<ProfileEditOverlay
	open={editOpen}
	editName={profile.editName}
	editBio={profile.editBio}
	isSaving={profile.isSaving}
	saveError={profile.saveError}
	onSave={() => {
		profile.saveEdit().then(() => {
			editOpen = false;
		});
	}}
	onCancel={() => {
		profile.cancelEdit();
		editOpen = false;
	}}
	onNameChange={(v) => {
		profile.editName = v;
	}}
	onBioChange={(v) => {
		profile.editBio = v;
	}}
/>

<style>
	.sr-only {
		position: absolute;
		width: 1px;
		height: 1px;
		padding: 0;
		margin: -1px;
		overflow: hidden;
		clip: rect(0, 0, 0, 0);
		white-space: nowrap;
		border: 0;
	}

	.profile-container {
		height: 100%;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
		padding-bottom: var(--content-bottom-padding, 104px);
		position: relative;
	}

	.top-bar {
		position: sticky;
		top: 0;
		z-index: 10;
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.5rem;
		padding: calc(
				0.625rem +
					var(
						--tg-content-top,
						var(--tg-content-safe-area-inset-top, env(safe-area-inset-top, 0px))
					)
			) 0.5rem 0.625rem 1rem;
		background: rgba(15, 15, 15, 0.78);
		backdrop-filter: blur(14px) saturate(160%);
		-webkit-backdrop-filter: blur(14px) saturate(160%);
		border-bottom: 1px solid rgba(255, 255, 255, 0.05);
	}

	.top-bar__title {
		display: flex;
		align-items: baseline;
		gap: 0.5rem;
		min-width: 0;
	}

	.top-bar__name {
		margin: 0;
		font-size: 1rem;
		font-weight: 700;
		color: var(--tg-theme-text-color, #f0f0f0);
		letter-spacing: -0.015em;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	.top-bar__sub {
		font-size: 0.8125rem;
		color: rgba(240, 240, 240, 0.45);
		font-variant-numeric: tabular-nums;
		white-space: nowrap;
	}

	.icon-btn {
		background: none;
		border: none;
		cursor: pointer;
		color: rgba(240, 240, 240, 0.5);
		padding: 0.5rem;
		min-width: 2.5rem;
		min-height: 2.5rem;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		border-radius: 0.625rem;
		-webkit-tap-highlight-color: transparent;
		transition: color 0.15s ease;
	}

	.icon-btn svg {
		width: 1.25rem;
		height: 1.25rem;
	}

	.icon-btn:active {
		color: rgba(240, 240, 240, 0.85);
	}

	.identity-card {
		display: grid;
		grid-template-columns: auto 1fr;
		gap: 0.875rem;
		padding: 0.875rem 1rem 1rem;
		animation: fadeIn 0.2s ease-out;
	}

	.avatar-btn {
		width: 5.25rem;
		height: 5.25rem;
		padding: 0;
		border: none;
		background: rgba(255, 255, 255, 0.07);
		border-radius: 50%;
		cursor: pointer;
		position: relative;
		overflow: hidden;
		flex-shrink: 0;
		-webkit-tap-highlight-color: transparent;
	}

	.avatar-btn:disabled {
		cursor: default;
	}

	.avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
		display: block;
	}

	.avatar-fallback {
		display: flex;
		align-items: center;
		justify-content: center;
		width: 100%;
		height: 100%;
		font-size: 1.75rem;
		font-weight: 600;
		color: rgba(240, 240, 240, 0.55);
		user-select: none;
	}

	.avatar-overlay {
		position: absolute;
		inset: 0;
		display: flex;
		align-items: center;
		justify-content: center;
		background: rgba(0, 0, 0, 0.5);
		color: #fff;
		border-radius: 50%;
	}

	.spinner {
		width: 1.25rem;
		height: 1.25rem;
		animation: spin 0.9s linear infinite;
	}

	.identity-body {
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 0.625rem;
	}

	.identity-top {
		display: flex;
		flex-direction: column;
		gap: 0.625rem;
	}

	.identity-names {
		display: flex;
		flex-direction: column;
		gap: 0.125rem;
		min-width: 0;
	}

	.display-name {
		margin: 0;
		font-size: 1.0625rem;
		font-weight: 700;
		color: var(--tg-theme-text-color, #f0f0f0);
		letter-spacing: -0.015em;
		word-break: break-word;
	}

	.username {
		margin: 0;
		font-size: 0.875rem;
		color: rgba(240, 240, 240, 0.42);
	}

	.identity-actions {
		display: flex;
		flex-wrap: wrap;
		gap: 0.5rem;
		align-items: center;
	}

	.bio {
		margin: 0;
		font-size: 0.9375rem;
		color: rgba(240, 240, 240, 0.78);
		line-height: 1.5;
		white-space: pre-wrap;
		word-break: break-word;
	}

	.avatar-error {
		margin: 0;
		font-size: 0.75rem;
		color: #e05252;
	}

	.btn {
		padding: 0.4375rem 0.875rem;
		border-radius: 9999px;
		font-size: 0.8125rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition: background-color 0.15s ease, transform 0.1s ease;
		-webkit-tap-highlight-color: transparent;
	}

	.btn-outline {
		background: none;
		color: var(--tg-theme-text-color, #f0f0f0);
		border: 1px solid rgba(255, 255, 255, 0.16);
	}

	.btn-outline:active:not(:disabled) {
		background: rgba(255, 255, 255, 0.06);
		transform: scale(0.97);
	}

	.strava-slot {
		padding: 0 1rem 0.75rem;
	}

	.tabs-wrap {
		padding: 0 1rem;
		position: sticky;
		top: 3.25rem;
		background: rgba(15, 15, 15, 0.85);
		backdrop-filter: blur(12px) saturate(160%);
		-webkit-backdrop-filter: blur(12px) saturate(160%);
		z-index: 9;
	}

	.tabpanel:focus {
		outline: none;
	}

	.skeleton {
		display: flex;
		align-items: flex-start;
		gap: 0.875rem;
		padding: 0.875rem 1rem 1rem;
	}

	.skeleton-avatar {
		width: 5.25rem;
		height: 5.25rem;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.06);
		animation: shimmer 1.6s ease-in-out infinite;
		flex-shrink: 0;
	}

	.skeleton-info {
		flex: 1;
		min-width: 0;
		padding-top: 0.5rem;
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}

	.skeleton-line {
		height: 0.875rem;
		border-radius: 0.25rem;
		background: rgba(255, 255, 255, 0.06);
		animation: shimmer 1.6s ease-in-out infinite;
	}

	.skeleton-line--sm {
		height: 0.6875rem;
	}

	.error-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.75rem;
		padding: 3rem 1rem;
	}

	.error-text {
		font-size: 0.875rem;
		color: #e05252;
		text-align: center;
	}

	.retry-btn {
		padding: 0.5rem 1.25rem;
		border-radius: 0.625rem;
		border: 1px solid rgba(255, 255, 255, 0.15);
		background: none;
		color: var(--color-text-primary, #f0f0f0);
		font-size: 0.875rem;
		font-weight: 500;
		cursor: pointer;
		font-family: inherit;
	}

	.retry-btn:active {
		opacity: 0.7;
	}

	.strava-toast {
		position: fixed;
		top: max(env(safe-area-inset-top, 16px), 20px);
		left: 50%;
		transform: translateX(-50%);
		z-index: 999;
		display: flex;
		align-items: center;
		gap: 0.4375rem;
		padding: 0.625rem 1.125rem;
		border-radius: 6.25rem;
		font-size: 0.875rem;
		font-weight: 500;
		white-space: nowrap;
		box-shadow: 0 0.25rem 1.25rem rgba(0, 0, 0, 0.45);
		pointer-events: none;
	}

	.strava-toast--success {
		background: rgba(16, 185, 129, 0.95);
		color: #fff;
	}

	.strava-toast--denied {
		background: rgba(60, 60, 60, 0.95);
		color: rgba(255, 255, 255, 0.8);
	}

	@keyframes fadeIn {
		from {
			opacity: 0;
			transform: translateY(0.25rem);
		}
		to {
			opacity: 1;
			transform: translateY(0);
		}
	}

	@keyframes shimmer {
		0%,
		100% {
			opacity: 0.5;
		}
		50% {
			opacity: 1;
		}
	}

	@keyframes spin {
		to {
			transform: rotate(360deg);
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.identity-card,
		.skeleton-avatar,
		.skeleton-line {
			animation: none;
		}
	}
</style>
