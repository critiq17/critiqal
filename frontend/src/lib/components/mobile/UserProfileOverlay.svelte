<script lang="ts">
	import { onMount } from 'svelte';
	import type { User, Post } from '$lib/types';
	import { userService } from '$lib/services/user.service';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import { closeProfile } from '$lib/stores/profile-nav.store';
	import { notifyOverlaySwipe } from '$lib/overlay-swipe';

	// ── Swipe-to-dismiss (direct DOM, bypass Svelte reactivity for 60fps) ───────
	//
	// Root cause of the "sticky/delayed" feel in the previous version:
	//   CSS `animation: slideIn ... both` has fill-mode that OVERRIDES inline
	//   style.transform (CSS animations sit above inline styles in the cascade).
	//   The fix: no CSS animation — we drive the slide-in imperatively in onMount.
	//
	// Additionally we notify MobileLayout on every drag frame so the background
	// content can track the overlay position (iOS-style parallax).

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
		// Kill any running transition so the next touchmove sticks to finger instantly
		_applyTransform(_currentX, 'none');
		_touchStartX = 0; // reset — will be populated in move
		_dirLocked = null;
		_velocity = 0;
	}

	function onSwipeTouchMove(e: TouchEvent): void {
		const t = e.touches[0];
		if (!t) return;

		// Capture start on first move (avoids the 1-frame lag from touchstart)
		if (!_dirLocked && _touchStartX === 0) {
			_touchStartX = t.clientX;
			_touchStartY = t.clientY;
			_lastX = t.clientX;
			_lastT = Date.now();
		}

		const dx = t.clientX - _touchStartX;
		const dy = t.clientY - _touchStartY;

		// Direction lock: first 5px determines axis
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

		// Write directly — zero framework overhead
		if (overlayEl) overlayEl.style.transform = `translateX(${dx}px)`;

		// Notify background (parallax)
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
			const [user, postsPage, followers, following] = await Promise.all([
				userService.getProfile(username),
				userService.getUserPosts(username),
				userService.getFollowers(0).catch(() => [] as User[]),
				userService.getFollowing(0).catch(() => [] as User[])
			]);

			profile = user;
			posts = postsPage.content;

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
		// Programmatic slide-in replaces the CSS `animation: slideIn` declaration.
		// CSS animations with fill-mode:both override inline style.transform, which
		// breaks our direct DOM manipulation during swipe. Driving it here avoids
		// that cascade conflict entirely.
		if (overlayEl) {
			overlayEl.style.transition = 'none';
			overlayEl.style.transform = 'translateX(100%)';
			// Force a reflow so the browser registers the starting position
			// before we add the transition.
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
	aria-label={`${username}'s profile`}
	bind:this={overlayEl}
	ontouchstart={onSwipeTouchStart}
	ontouchmove={onSwipeTouchMove}
	ontouchend={onSwipeTouchEnd}
>
	<!-- Header bar — back navigation is handled by tg.BackButton (wired in $effect below) -->
	<div class="overlay-header">
		<span class="header-username">{username}</span>
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
										{#each [...post.photos].sort((a, b) => a.position - b.position).slice(0, 1) as photo (photo.id)}
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
		/* No CSS animation — slide-in is driven programmatically in onMount.
		   CSS animation fill-mode overrides style.transform, which breaks
		   direct DOM manipulation used for the swipe gesture. */
		will-change: transform;
		/* pan-y: browser handles vertical scroll natively,
		   we own horizontal — eliminates ~100ms touch-start latency. */
		touch-action: pan-y;
	}


	/* Header — username only; back nav uses tg.BackButton */
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
