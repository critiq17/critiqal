<script lang="ts">
	import { onMount } from 'svelte';
	import type { User, Post } from '$lib/types';
	import { userService } from '$lib/services/user.service';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import { closeProfile, openProfile } from '$lib/stores/profile-nav.store.svelte';
	import { openMobileComments } from '$lib/stores/mobile-comments.store';
	import { notifyOverlaySwipe } from '$lib/overlay-swipe';
	import { registerSheet } from '$lib/actions/registerSheet';
	import OverlayProfileInfo from './OverlayProfileInfo.svelte';
	import { MobilePostList } from '$lib/components/post';

	// ── Swipe-to-dismiss (direct DOM at 60fps) ────────────────────────────────
	// CSS animations with fill-mode override inline style.transform, so we
	// drive the slide-in imperatively in onMount instead.

	let overlayEl: HTMLElement | null = null;

	let _touchStartX = 0;
	let _touchStartY = 0;
	let _dirLocked: 'h' | 'v' | null = null;
	let _lastX = 0;
	let _lastT = 0;
	let _velocity = 0;
	let _currentX = 0;

	const DISMISS_RATIO = 0.35;
	const VELOCITY_PX_MS = 0.45;

	function _applyTransform(x: number, transition: string): void {
		if (!overlayEl) return;
		overlayEl.style.transition = transition;
		overlayEl.style.transform = `translateX(${x}px)`;
	}

	function onSwipeTouchStart(): void {
		_applyTransform(_currentX, 'none');
		_touchStartX = 0;
		_dirLocked = null;
		_velocity = 0;
	}

	function onSwipeTouchMove(e: TouchEvent): void {
		const t = e.touches[0];
		if (!t) return;

		if (!_dirLocked && _touchStartX === 0) {
			_touchStartX = t.clientX;
			_touchStartY = t.clientY;
			_lastX = t.clientX;
			_lastT = Date.now();
		}

		const dx = t.clientX - _touchStartX;
		const dy = t.clientY - _touchStartY;

		if (!_dirLocked && (Math.abs(dx) > 5 || Math.abs(dy) > 5)) {
			_dirLocked = Math.abs(dx) >= Math.abs(dy) ? 'h' : 'v';
		}

		if (_dirLocked !== 'h' || dx < 0) return;

		const now = Date.now();
		const dt = now - _lastT;
		if (dt > 0) _velocity = (t.clientX - _lastX) / dt;
		_lastX = t.clientX;
		_lastT = now;
		_currentX = dx;

		if (overlayEl) overlayEl.style.transform = `translateX(${dx}px)`;
		notifyOverlaySwipe(dx, window.innerWidth, 'drag');
	}

	function onSwipeTouchEnd(): void {
		if (_currentX <= 0) return;

		const sw = window.innerWidth;
		const threshold = sw * DISMISS_RATIO;

		if (_currentX > threshold || _velocity > VELOCITY_PX_MS) {
			getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
			_applyTransform(sw, 'transform 0.24s cubic-bezier(0.4, 0, 1, 1)');
			notifyOverlaySwipe(sw, sw, 'dismiss');
			setTimeout(() => closeProfile(), 260);
		} else {
			_applyTransform(0, 'transform 0.38s cubic-bezier(0.34, 1.56, 0.64, 1)');
			notifyOverlaySwipe(0, sw, 'cancel');
		}
		_currentX = 0;
	}

	interface Props {
		username: string;
	}

	let { username }: Props = $props();

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

		// Fire posts fetch immediately — username is enough, no userId needed.
		const postsPromise = userService.getUserPosts(username).catch(() => null);

		try {
			const user = await userService.getProfile(username);
			profile = user;
			isLoading = false; // Show profile header immediately after first request.

			// Now fetch followers, following, and posts all in parallel.
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
		if (overlayEl) {
			overlayEl.style.transition = 'none';
			overlayEl.style.transform = 'translateX(100%)';
			void overlayEl.offsetWidth;
			overlayEl.style.transition = 'transform 0.28s cubic-bezier(0.4, 0, 0.2, 1)';
			overlayEl.style.transform = 'translateX(0)';
		}
		load();
	});
</script>

<div
	class="overlay"
	role="dialog"
	aria-label="{username}'s profile"
	use:registerSheet
	bind:this={overlayEl}
	ontouchstart={onSwipeTouchStart}
	ontouchmove={onSwipeTouchMove}
	ontouchend={onSwipeTouchEnd}
>
	<div class="overlay-header">
		<span class="header-username">{username}</span>
	</div>

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
			<OverlayProfileInfo
				{profile}
				{followerCount}
				{followingCount}
				postsCount={posts.length}
				{isOwnProfile}
				{isFollowing}
				{isTogglingFollow}
				onToggleFollow={toggleFollow}
			/>
			<MobilePostList
				{posts}
				loading={postsLoading}
				onOpenComments={(postId) => openMobileComments(postId)}
				onAuthorClick={(u) => openProfile(u)}
			/>
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
		will-change: transform;
		touch-action: pan-y;
	}

	.overlay-header {
		display: flex;
		align-items: center;
		padding: calc(var(--tg-content-top, var(--tg-content-safe-area-inset-top, env(safe-area-inset-top, 0px))) + 14px) 16px 14px;
		border-bottom: 1px solid rgba(255, 255, 255, 0.06);
		background: var(--tg-bg, #0f0f0f);
	}

	.header-username {
		font-size: 16px;
		font-weight: 600;
		color: var(--tg-text, #f0f0f0);
	}

	.overlay-scroll {
		flex: 1;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
		padding-bottom: 32px;
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
		background: rgba(255, 255, 255, 0.08);
		border: none;
		color: var(--tg-text, #f0f0f0);
		cursor: pointer;
		font-size: 14px;
	}
</style>
