<script lang="ts">
	import { onMount } from 'svelte';
	import { fly, fade } from 'svelte/transition';
	import { getTelegramWebApp } from '$lib/telegram';
	import { openMobileComments } from '$lib/stores/mobile-comments.store';
	import { tabStore } from '$lib/stores/mobile-tab.store.svelte';
	import { stravaStore } from '$lib/stores/strava.store.svelte';
	import { openSettings } from '$lib/stores/settings-nav.store.svelte';
	import { UseProfile } from '$lib/features/profile/useProfile.svelte';
	import ProfileHeader from '$lib/components/profile/ProfileHeader.svelte';
	import ProfileStickyHeader from '$lib/components/profile/ProfileStickyHeader.svelte';
	import ProfilePostsList from '$lib/components/profile/ProfilePostsList.svelte';
	import FollowersOverlay from '$lib/components/profile/FollowersOverlay.svelte';
	import ProfileEditOverlay from '$lib/components/profile/ProfileEditOverlay.svelte';

	const profile = new UseProfile();

	let showStickyHeader = $state(false);
	let sentinelEl = $state<HTMLElement | null>(null);

	let followersOverlayType = $state<'followers' | 'following' | null>(null);
	let stravaNotification = $state<'connected' | 'denied' | null>(null);
	let editOpen = $state(false);

	let fileInputEl = $state<HTMLInputElement | null>(null);

	function formatCount(n: number | null): string {
		if (n === null) return '—';
		if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
		if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
		return String(n);
	}

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

	async function retryPosts(): Promise<void> {
		if (!profile.profile) return;
		try {
			const { userService } = await import('$lib/services/user.service');
			const r = await userService.getUserPosts(profile.profile.username);
			profile.posts = r.content;
		} catch {
			// non-critical
		}
	}

	$effect(() => {
		const tg = getTelegramWebApp();

		if (tg) tg.BackButton.hide();
	});

	$effect(() => {
		if (!sentinelEl) return;
		const obs = new IntersectionObserver(
			(entries) => {
				showStickyHeader = !(entries[0]?.isIntersecting ?? true);
			},
			{ threshold: 0 }
		);
		obs.observe(sentinelEl);
		return () => obs.disconnect();
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
			setTimeout(() => { stravaNotification = null; }, 3500);
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

	<ProfileStickyHeader
		username={profile.profile?.username ?? null}
		visible={showStickyHeader}
		onSettings={openSettings}
	/>

	{#if profile.isLoading && !profile.profile}
		<div class="loading-state" aria-busy="true" aria-label="Loading profile">
			<div class="skeleton-avatar"></div>
			<div class="skeleton-name"></div>
			<div class="skeleton-bio"></div>
			<div class="skeleton-bio short"></div>
		</div>
	{:else if profile.profileError && !profile.profile}
		<div class="error-state" role="alert">
			<p class="error-text">{profile.profileError}</p>
			<button class="retry-btn" onclick={() => profile.load()}>Try again</button>
		</div>
	{:else if profile.profile}
		<ProfileHeader
			profile={profile.profile}
			isUploadingAvatar={profile.isUploadingAvatar}
			avatarError={profile.avatarError}
			onSettings={openSettings}
			onEdit={() => { profile.startEdit(); editOpen = true; }}
			onAvatarClick={() => fileInputEl?.click()}
		/>

		<div bind:this={sentinelEl} class="scroll-sentinel" aria-hidden="true"></div>

		<div class="stats-row" role="group" aria-label="Profile stats">
			<div class="stat-item">
				<span class="stat-value">{formatCount(profile.posts.length)}</span>
				<span class="stat-label">Posts</span>
			</div>
			<button class="stat-item stat-btn" onclick={() => openFollowersOverlay('followers')} aria-label="View followers">
				<span class="stat-value">{formatCount(profile.followersCount)}</span>
				<span class="stat-label">Followers</span>
			</button>
			<button class="stat-item stat-btn" onclick={() => openFollowersOverlay('following')} aria-label="View following">
				<span class="stat-value">{formatCount(profile.followingCount)}</span>
				<span class="stat-label">Following</span>
			</button>
		</div>

		<div class="posts-section-header" aria-hidden="true">
			<span>Posts</span>
		</div>

		<ProfilePostsList
			posts={profile.posts}
			postsLoading={profile.postsLoading}
			postsError={profile.profileError}
			onOpenComments={openComments}
			onRetry={() => profile.load()}
		/>
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
	onSave={() => { profile.saveEdit().then(() => { editOpen = false; }); }}
	onCancel={() => { profile.cancelEdit(); editOpen = false; }}
	onNameChange={(v) => { profile.editName = v; }}
	onBioChange={(v) => { profile.editBio = v; }}
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
		padding-top: max(
			var(--tg-content-safe-area-inset-top, 0px),
			calc(env(safe-area-inset-top, 20px) + 44px)
		);
		padding-bottom: var(--content-bottom-padding, 104px);
		position: relative;
	}

	.loading-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 32px 16px 16px;
		gap: 10px;
	}

	.skeleton-avatar {
		width: 80px;
		height: 80px;
		border-radius: 50%;
		background: var(--color-skeleton, rgba(255, 255, 255, 0.08));
		animation: shimmer 1.6s ease-in-out infinite;
	}

	.skeleton-name {
		width: 120px;
		height: 14px;
		border-radius: 4px;
		background: var(--color-skeleton, rgba(255, 255, 255, 0.08));
		animation: shimmer 1.6s ease-in-out infinite;
	}

	.skeleton-bio {
		width: 200px;
		height: 12px;
		border-radius: 4px;
		background: var(--color-skeleton, rgba(255, 255, 255, 0.08));
		animation: shimmer 1.6s ease-in-out infinite;
	}

	.skeleton-bio.short { width: 140px; }

	.error-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 12px;
		padding: 48px 16px;
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

	.retry-btn:active { opacity: 0.7; }

	.scroll-sentinel {
		height: 1px;
		width: 100%;
		pointer-events: none;
	}

	/* ── Stats row ─────────────────────────────────────────────────────────── */

	.stats-row {
		display: flex;
		align-items: center;
		padding: 0 8px 4px;
		border-bottom: 1px solid rgba(255, 255, 255, 0.06);
	}

	.stat-item {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 10px 4px;
		gap: 2px;
		background: none;
		border: none;
		font-family: inherit;
		cursor: default;
	}

	.stat-btn {
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
		border-radius: 10px;
		transition: background-color 0.12s;
	}

	.stat-btn:active { background: rgba(255, 255, 255, 0.05); }

	.stat-value {
		font-size: 17px;
		font-weight: 700;
		color: var(--tg-theme-text-color, #f0f0f0);
		letter-spacing: -0.02em;
		line-height: 1;
	}

	.stat-label {
		font-size: 10px;
		font-weight: 500;
		color: rgba(240, 240, 240, 0.35);
		text-transform: uppercase;
		letter-spacing: 0.04em;
	}

	/* ── Posts section header ──────────────────────────────────────────────── */

	.posts-section-header {
		padding: 0 20px 8px;
		font-size: 0.6875rem;
		font-weight: 500;
		color: rgba(255, 255, 255, 0.3);
		text-transform: uppercase;
		letter-spacing: 0.07em;
		border-top: 1px solid rgba(255, 255, 255, 0.06);
		padding-top: 12px;
	}

	.strava-toast {
		position: fixed;
		top: max(env(safe-area-inset-top, 16px), 20px);
		left: 50%;
		transform: translateX(-50%);
		z-index: 999;
		display: flex;
		align-items: center;
		gap: 7px;
		padding: 10px 18px;
		border-radius: 100px;
		font-size: 14px;
		font-weight: 500;
		white-space: nowrap;
		box-shadow: 0 4px 20px rgba(0, 0, 0, 0.45);
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

	@keyframes shimmer {
		0%, 100% { opacity: 0.5; }
		50% { opacity: 1; }
	}
</style>
