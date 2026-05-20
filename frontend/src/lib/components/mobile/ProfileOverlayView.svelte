<script lang="ts">
	import { onMount } from 'svelte';
	import type { User, Post } from '$lib/types';
	import { userService } from '$lib/services/user.service';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import { navStack } from '$lib/stores/nav-stack.store.svelte';
	import { openMobileComments } from '$lib/stores/mobile-comments.store';
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

	let scrollEl = $state<HTMLDivElement | undefined>(undefined);
	let scrolled = $state(false);
	function onScroll(): void {
		scrolled = (scrollEl?.scrollTop ?? 0) > 8;
	}

	let profile = $state<User | null>(null);
	let posts = $state<Post[]>([]);
	let isLoading = $state(true);
	let postsLoading = $state(false);
	let error = $state<string | null>(null);

	let isFollowing = $state(false);
	let followerCount = $state(0);
	let followingCount = $state(0);
	let isTogglingFollow = $state(false);

	const isOwnProfile = $derived(authStore.user?.username === username);

	async function load(): Promise<void> {
		isLoading = true;
		postsLoading = true;
		error = null;

		const postsPromise = userService.getUserPosts(username).catch(() => null);

		try {
			const user = await userService.getProfile(username);
			profile = user;
			isLoading = false;

			const [followersList, followingList, postsPage] = await Promise.all([
				userService.getFollowers(user.id).catch(() => [] as User[]),
				userService.getFollowing(user.id).catch(() => [] as User[]),
				postsPromise
			]);

			followerCount = followersList.length;
			followingCount = followingList.length;
			if (authStore.user) {
				isFollowing = followersList.some((f) => f.id === authStore.user!.id);
			}
			if (postsPage) posts = postsPage.content;
		} catch (err) {
			error = err instanceof Error ? err.message : 'Failed to load profile';
			isLoading = false;
		} finally {
			postsLoading = false;
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
			if (wasFollowing) await userService.unfollow(profile.id);
			else await userService.follow(profile.id);
		} catch {
			isFollowing = wasFollowing;
			followerCount += wasFollowing ? 1 : -1;
		} finally {
			isTogglingFollow = false;
		}
	}

	onMount(load);
</script>

<div
	class="overlay-scroll"
	bind:this={scrollEl}
	onscroll={onScroll}
>
	<CollapsingHeader title={profile?.name ?? username} {scrolled} />
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
		<OverlayProfileInfo
			{profile}
			{followerCount}
			{followingCount}
			postsCount={posts.length}
			{isOwnProfile}
			{isFollowing}
			{isTogglingFollow}
			onToggleFollow={toggleFollow}
			onOpenFollowers={() => navStack.pushConnections(username, 'followers')}
			onOpenFollowing={() => navStack.pushConnections(username, 'following')}
		/>
		<MobilePostList
			{posts}
			loading={postsLoading}
			onOpenComments={(postId) => openMobileComments(postId)}
			onAuthorClick={(u) => navStack.pushProfile(u)}
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
