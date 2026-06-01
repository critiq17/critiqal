<script lang="ts">
	import { onMount } from 'svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import { navStack } from '$lib/stores/nav-stack.store.svelte';
	import { openMobileComments } from '$lib/stores/mobile-comments.store';
	import { UseProfilePage } from '$lib/features/profile/useProfilePage.svelte';
	import OverlayProfileInfo from './OverlayProfileInfo.svelte';
	import CollapsingHeader from './CollapsingHeader.svelte';
	import { MobilePostList } from '$lib/components/post';

	interface Props {
		username: string;
		onBack: () => void;
	}

	// Back is handled by the native Telegram BackButton (wired in OverlayHost).
	let { username, onBack }: Props = $props();
	void onBack;

	// Shared SWR-cached profile hook (also used by the desktop /[username]
	// route). Hydrates from profileCache, revalidates on visibility return,
	// lazy-loads follower lists.
	const profile = new UseProfilePage(username);

	let scrollEl = $state<HTMLDivElement | undefined>(undefined);
	let headerProgress = $state(0);
	function onScroll(): void {
		const top = scrollEl?.scrollTop ?? 0;
		headerProgress = Math.min(1, Math.max(0, top / 80));
	}

	async function handleToggleFollow(): Promise<void> {
		getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
		await profile.toggleFollow();
	}

	onMount(() => {
		profile.loadProfile();
	});
</script>

<div class="overlay-scroll" bind:this={scrollEl} onscroll={onScroll}>
	<CollapsingHeader title={profile.profile?.name ?? username} progress={headerProgress} />
	{#if profile.profileState === 'loading' && !profile.profile}
		<div class="loading-state" aria-busy="true">
			<div class="skel skel-avatar"></div>
			<div class="skel skel-name"></div>
			<div class="skel skel-bio"></div>
		</div>
	{:else if profile.profileError && !profile.profile}
		<div class="error-state" role="alert">
			<p>{profile.profileError}</p>
			<button class="retry-btn" onclick={() => profile.loadProfile()}>Try again</button>
		</div>
	{:else if profile.profile}
		<OverlayProfileInfo
			profile={profile.profile}
			followerCount={profile.followersCount ?? 0}
			followingCount={profile.followingCount ?? 0}
			postsCount={profile.postsCount ?? profile.posts.length}
			isOwnProfile={false}
			isFollowing={profile.isFollowing}
			isTogglingFollow={profile.isFollowLoading}
			onToggleFollow={handleToggleFollow}
			onOpenFollowers={() => navStack.pushConnections(username, 'followers')}
			onOpenFollowing={() => navStack.pushConnections(username, 'following')}
		/>
		<MobilePostList
			posts={profile.posts}
			loading={profile.postsLoading}
			error={profile.postsError}
			onOpenComments={(post) => openMobileComments(post)}
			onAuthorClick={(u) => navStack.pushProfile(u)}
			onDeleted={(id) => profile.handlePostDeleted(id)}
			onRetry={() => profile.loadPosts()}
		/>
	{/if}
</div>

<style>
	.overlay-scroll {
		flex: 1;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior: contain;
		padding-bottom: calc(32px + env(safe-area-inset-bottom, 0px));
	}

	.loading-state {
		padding: 32px 20px;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 12px;
	}

	.skel {
		background: var(--surface-tint-soft);
		border-radius: 8px;
		animation: pulse 1.4s ease-in-out infinite;
	}

	.skel-avatar { width: 80px; height: 80px; border-radius: 50%; }
	.skel-name { width: 140px; height: 20px; }
	.skel-bio { width: 200px; height: 14px; }

	@keyframes pulse {
		0%, 100% { opacity: 0.4; }
		50% { opacity: 0.7; }
	}

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
		background: var(--surface-tint-medium);
		border: none;
		color: var(--tg-text, #f0f0f0);
		cursor: pointer;
		font-size: 14px;
	}

	@media (prefers-reduced-motion: reduce) {
		.skel { animation: none; }
	}
</style>
