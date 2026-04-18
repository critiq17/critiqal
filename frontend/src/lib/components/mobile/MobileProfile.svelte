<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import { goto } from '$app/navigation';
	import type { User, Post, ReactionType, ReactionsMap } from '$lib/types';
	import { userService } from '$lib/services/user.service';
	import { mediaService } from '$lib/services/media.service';
	import { postService } from '$lib/services/post.service';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import { DEFAULT_REACTIONS, REACTION_TYPES, REACTION_VISUALS } from '$lib/reactions';
	import { openSheet } from '$lib/stores/sheet.store';
	import { viewTracker } from '$lib/utils/viewTracker';
	import CommentSheet from './CommentSheet.svelte';

	// ---------------------------------------------------------------------------
	// State
	// ---------------------------------------------------------------------------

	let profile = $state<User | null>(null);
	let posts = $state<Post[]>([]);
	let followersList = $state<User[]>([]);
	let followingList = $state<User[]>([]);

	let isLoading = $state(true);
	let profileError = $state<string | null>(null);
	let postsError = $state<string | null>(null);
	let listsLoading = $state(false);

	// Edit mode
	let isEditing = $state(false);
	let editName = $state('');
	let editBio = $state('');
	let isSaving = $state(false);
	let saveError = $state<string | null>(null);

	// Avatar upload
	let isUploadingAvatar = $state(false);
	let avatarError = $state<string | null>(null);
	let fileInputEl = $state<HTMLInputElement | null>(null);

	// Sticky header visibility
	let showStickyHeader = $state(false);
	let sentinelEl = $state<HTMLElement | null>(null);

	// Stats bottom sheet
	let statsSheetType = $state<'followers' | 'following' | null>(null);

	// Post detail bottom sheet
	let selectedPost = $state<Post | null>(null);

	// Comment sheet
	let openCommentSheetPostId = $state<number | null>(null);

	// Reactions
	interface PostReactionState {
		reactions: ReactionsMap;
		myReaction: ReactionType | null;
		loaded: boolean;
		poppingType: ReactionType | null;
	}
	let reactionStates = $state<Map<number, PostReactionState>>(new Map());

	// Settings bottom sheet
	let settingsOpen = $state(false);

	// Sheet cleanup functions (registered with global sheet store so BottomNav hides)
	let closeStatsSheet_: (() => void) | null = null;
	let closePostSheet_: (() => void) | null = null;
	let closeSettings_: (() => void) | null = null;

	// Drag-to-dismiss shared state (reused per sheet via a helper)
	let statsDragY = $state(0);
	let statsIsDragging = $state(false);
	let statsDragStartY = 0;
	let statsSheetEl = $state<HTMLElement | null>(null);

	let postDragY = $state(0);
	let postIsDragging = $state(false);
	let postDragStartY = 0;
	let postSheetEl = $state<HTMLElement | null>(null);

	let settingsDragY = $state(0);
	let settingsIsDragging = $state(false);
	let settingsDragStartY = 0;
	let settingsSheetEl = $state<HTMLElement | null>(null);

	// ---------------------------------------------------------------------------
	// Derived
	// ---------------------------------------------------------------------------

	let followersCount = $derived(followersList.length);
	let followingCount = $derived(followingList.length);

	// ---------------------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------------------

	function formatViews(n: number): string {
		if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
		if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
		return String(n);
	}

	function trackView(el: HTMLElement, postId: number): { destroy: () => void } {
		const cleanup = viewTracker.observe(el, postId, authStore.isAuthenticated);
		return { destroy: cleanup };
	}

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

	function getInitialFromParts(name: string | null, username: string): string {
		return (name ?? username).charAt(0).toUpperCase();
	}

	// ---------------------------------------------------------------------------
	// Reaction helpers
	// ---------------------------------------------------------------------------

	function getReactionState(postId: number): PostReactionState {
		return reactionStates.get(postId) ?? {
			reactions: { ...DEFAULT_REACTIONS },
			myReaction: null,
			loaded: false,
			poppingType: null
		};
	}

	function setReactionState(postId: number, patch: Partial<PostReactionState>): void {
		const prev = getReactionState(postId);
		const next = new Map(reactionStates);
		next.set(postId, { ...prev, ...patch });
		reactionStates = next;
	}

	async function loadReactionsForPost(postId: number): Promise<void> {
		if (getReactionState(postId).loaded) return;
		try {
			const [reactions, myReaction] = await Promise.all([
				postService.getReactions(postId),
				postService.getMyReaction(postId).catch(() => undefined)
			]);
			setReactionState(postId, {
				reactions: { ...DEFAULT_REACTIONS, ...reactions },
				myReaction: myReaction ?? null,
				loaded: true
			});
		} catch {
			// Non-critical
		}
	}

	async function handleReaction(post: Post, type: ReactionType): Promise<void> {
		const state = getReactionState(post.id);
		const already = state.myReaction === type;

		getTelegramWebApp()?.HapticFeedback.impactOccurred('light');

		setReactionState(post.id, { poppingType: type });
		setTimeout(() => setReactionState(post.id, { poppingType: null }), 300);

		const prev = { ...state.reactions };
		const newReactions = { ...prev };
		if (already) {
			newReactions[type] = Math.max(0, newReactions[type] - 1);
			setReactionState(post.id, { reactions: newReactions, myReaction: null });
		} else {
			if (state.myReaction) {
				newReactions[state.myReaction] = Math.max(0, newReactions[state.myReaction] - 1);
			}
			newReactions[type] = newReactions[type] + 1;
			setReactionState(post.id, { reactions: newReactions, myReaction: type });
		}

		try {
			if (already) {
				await postService.removeReaction(post.id);
			} else {
				await postService.react(post.id, type);
			}
		} catch {
			setReactionState(post.id, { reactions: prev, myReaction: state.myReaction });
		}
	}

	// ---------------------------------------------------------------------------
	// Data loading
	// ---------------------------------------------------------------------------

	async function loadProfile(): Promise<void> {
		const username = authStore.user?.username;
		if (!username) {
			isLoading = false;
			return;
		}

		isLoading = true;
		profileError = null;

		try {
			const [user, postsPage] = await Promise.all([
				userService.getProfile(username),
				userService.getUserPosts(username)
			]);
			profile = user;
			posts = postsPage.content;
			loadFollowLists(user.id);
			postsPage.content.forEach((p) => loadReactionsForPost(p.id));
		} catch (err: unknown) {
			profileError = err instanceof Error ? err.message : 'Failed to load profile';
		} finally {
			isLoading = false;
		}
	}

	async function loadFollowLists(userId: number): Promise<void> {
		listsLoading = true;
		try {
			const [followers, following] = await Promise.all([
				userService.getFollowers(userId),
				userService.getFollowing(userId)
			]);
			followersList = followers;
			followingList = following;
		} catch {
			// non-critical — counts stay at 0
		} finally {
			listsLoading = false;
		}
	}

	// ---------------------------------------------------------------------------
	// Avatar upload
	// ---------------------------------------------------------------------------

	async function handleAvatarChange(e: Event): Promise<void> {
		const input = e.target as HTMLInputElement;
		const file = input.files?.[0];
		if (!file || !profile) return;

		isUploadingAvatar = true;
		avatarError = null;
		try {
			const result = await mediaService.uploadAvatar(file);
			profile = { ...profile, avatarUrl: result.avatarUrl };
			authStore.updateUser({ ...authStore.user!, avatarUrl: result.avatarUrl });
		} catch (err: unknown) {
			avatarError = err instanceof Error ? err.message : 'Upload failed';
		} finally {
			isUploadingAvatar = false;
			input.value = '';
		}
	}

	function triggerAvatarPicker(): void {
		fileInputEl?.click();
	}

	// ---------------------------------------------------------------------------
	// Edit profile
	// ---------------------------------------------------------------------------

	function startEdit(): void {
		if (!profile) return;
		editName = profile.name ?? '';
		editBio = profile.bio ?? '';
		isEditing = true;
		saveError = null;
	}

	function cancelEdit(): void {
		isEditing = false;
		saveError = null;
	}

	async function saveEdit(): Promise<void> {
		isSaving = true;
		saveError = null;
		try {
			const updated = await userService.updateProfile({
				name: editName.trim() || undefined,
				bio: editBio.trim() || undefined,
				avatarUrl: profile?.avatarUrl ?? undefined
			});
			profile = updated;
			authStore.updateUser(updated);
			isEditing = false;
		} catch (err: unknown) {
			saveError = err instanceof Error ? err.message : 'Failed to save changes';
		} finally {
			isSaving = false;
		}
	}

	// ---------------------------------------------------------------------------
	// Stats sheet
	// ---------------------------------------------------------------------------

	function openStatsSheet(type: 'followers' | 'following'): void {
		statsSheetType = type;
		closeStatsSheet_ = openSheet();
	}

	function closeStatsSheet(): void {
		statsSheetType = null;
		statsDragY = 0;
		if (statsSheetEl) statsSheetEl.style.transform = '';
		closeStatsSheet_?.();
		closeStatsSheet_ = null;
	}

	function onStatsTouchStart(e: TouchEvent): void {
		statsDragStartY = e.touches[0].clientY;
		statsIsDragging = true;
		if (statsSheetEl) statsSheetEl.classList.add('dragging');
	}

	function onStatsTouchMove(e: TouchEvent): void {
		if (!statsIsDragging) return;
		const delta = e.touches[0].clientY - statsDragStartY;
		statsDragY = Math.max(0, delta);
		if (statsSheetEl) statsSheetEl.style.transform = `translateY(${statsDragY}px)`;
	}

	function onStatsTouchEnd(): void {
		statsIsDragging = false;
		if (statsSheetEl) statsSheetEl.classList.remove('dragging');
		if (statsDragY > 120) {
			closeStatsSheet();
		} else {
			statsDragY = 0;
			if (statsSheetEl) statsSheetEl.style.transform = '';
		}
	}

	// ---------------------------------------------------------------------------
	// Post detail sheet
	// ---------------------------------------------------------------------------

	function openPostSheet(post: Post): void {
		selectedPost = post;
		closePostSheet_ = openSheet();
	}

	function closePostSheet(): void {
		selectedPost = null;
		postDragY = 0;
		if (postSheetEl) postSheetEl.style.transform = '';
		closePostSheet_?.();
		closePostSheet_ = null;
	}

	function onPostTouchStart(e: TouchEvent): void {
		postDragStartY = e.touches[0].clientY;
		postIsDragging = true;
		if (postSheetEl) postSheetEl.classList.add('dragging');
	}

	function onPostTouchMove(e: TouchEvent): void {
		if (!postIsDragging) return;
		const delta = e.touches[0].clientY - postDragStartY;
		postDragY = Math.max(0, delta);
		if (postSheetEl) postSheetEl.style.transform = `translateY(${postDragY}px)`;
	}

	function onPostTouchEnd(): void {
		postIsDragging = false;
		if (postSheetEl) postSheetEl.classList.remove('dragging');
		if (postDragY > 120) {
			closePostSheet();
		} else {
			postDragY = 0;
			if (postSheetEl) postSheetEl.style.transform = '';
		}
	}

	// ---------------------------------------------------------------------------
	// Settings sheet
	// ---------------------------------------------------------------------------

	function openSettings(): void {
		settingsOpen = true;
		closeSettings_ = openSheet();
	}

	function closeSettings(): void {
		settingsOpen = false;
		settingsDragY = 0;
		if (settingsSheetEl) settingsSheetEl.style.transform = '';
		closeSettings_?.();
		closeSettings_ = null;
	}

	function onSettingsTouchStart(e: TouchEvent): void {
		settingsDragStartY = e.touches[0].clientY;
		settingsIsDragging = true;
		if (settingsSheetEl) settingsSheetEl.classList.add('dragging');
	}

	function onSettingsTouchMove(e: TouchEvent): void {
		if (!settingsIsDragging) return;
		const delta = e.touches[0].clientY - settingsDragStartY;
		settingsDragY = Math.max(0, delta);
		if (settingsSheetEl) settingsSheetEl.style.transform = `translateY(${settingsDragY}px)`;
	}

	function onSettingsTouchEnd(): void {
		settingsIsDragging = false;
		if (settingsSheetEl) settingsSheetEl.classList.remove('dragging');
		if (settingsDragY > 120) {
			closeSettings();
		} else {
			settingsDragY = 0;
			if (settingsSheetEl) settingsSheetEl.style.transform = '';
		}
	}

	function openSupport(): void {
		const tg = getTelegramWebApp();
		if (tg) {
			tg.openTelegramLink('https://t.me/critiq1');
		} else {
			window.open('https://t.me/critiq1', '_blank');
		}
	}

	function openDesktopVersion(): void {
		const tg = getTelegramWebApp();
		if (tg) {
			tg.openLink('https://dev.critiqal.xyz');
		} else {
			window.open('https://dev.critiqal.xyz', '_blank');
		}
	}

	async function handleLogout(): Promise<void> {
		await authStore.logout();
		goto('/');
	}

	// ---------------------------------------------------------------------------
	// Telegram BackButton wiring
	// ---------------------------------------------------------------------------

	$effect(() => {
		const tg = getTelegramWebApp();
		const anySheetOpen = statsSheetType !== null || selectedPost !== null || settingsOpen;

		if (tg) {
			if (anySheetOpen) {
				tg.BackButton.show();

				const handleBack = () => {
					if (settingsOpen) { closeSettings(); return; }
					if (selectedPost !== null) { closePostSheet(); return; }
					if (statsSheetType !== null) { closeStatsSheet(); return; }
				};

				tg.BackButton.onClick(handleBack);
				return () => {
					tg.BackButton.offClick(handleBack);
					tg.BackButton.hide();
				};
			} else {
				tg.BackButton.hide();
			}
		}
	});

	// ---------------------------------------------------------------------------
	// IntersectionObserver for sticky header
	// ---------------------------------------------------------------------------

	let headerObserver: IntersectionObserver | null = null;

	$effect(() => {
		if (!sentinelEl) return;
		headerObserver = new IntersectionObserver(
			(entries) => {
				showStickyHeader = !entries[0].isIntersecting;
			},
			{ threshold: 0 }
		);
		headerObserver.observe(sentinelEl);

		return () => {
			headerObserver?.disconnect();
			headerObserver = null;
		};
	});

	// ---------------------------------------------------------------------------
	// Lifecycle
	// ---------------------------------------------------------------------------

	onMount(() => {
		loadProfile();
	});
</script>

<!-- Hidden file input for avatar -->
<input
	bind:this={fileInputEl}
	type="file"
	accept="image/jpeg,image/png,image/webp"
	class="sr-only"
	onchange={handleAvatarChange}
	disabled={isUploadingAvatar}
	aria-hidden="true"
	tabindex="-1"
/>

<div class="profile-container">

	<!-- Sticky header -->
	<div class="sticky-header" class:visible={showStickyHeader} aria-hidden={!showStickyHeader}>
		<span class="sticky-username">
			{#if profile}@{profile.username}{/if}
		</span>
		<button
			class="settings-btn"
			onclick={openSettings}
			aria-label="Settings"
			tabindex={showStickyHeader ? 0 : -1}
		>
			<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
				<circle cx="12" cy="12" r="3"/>
				<path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/>
			</svg>
		</button>
	</div>

	<!-- Loading state -->
	{#if isLoading}
		<div class="loading-state" aria-busy="true" aria-label="Loading profile">
			<div class="skeleton-avatar"></div>
			<div class="skeleton-name"></div>
			<div class="skeleton-bio"></div>
			<div class="skeleton-bio short"></div>
		</div>

	<!-- Error state -->
	{:else if profileError}
		<div class="error-state" role="alert">
			<p class="error-text">{profileError}</p>
			<button class="retry-btn" onclick={loadProfile}>Try again</button>
		</div>

	<!-- Loaded state -->
	{:else if profile}

		<!-- Profile header -->
		<section class="profile-header" aria-label="Profile info">

			<!-- Settings gear (top-right, positioned absolutely) -->
			<button
				class="settings-btn-standalone"
				onclick={openSettings}
				aria-label="Settings"
			>
				<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
					<circle cx="12" cy="12" r="3"/>
					<path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/>
				</svg>
			</button>

			<!-- Avatar -->
			<div
				class="avatar-wrap"
				role="button"
				tabindex="0"
				onclick={() => fileInputEl?.click()}
				aria-label="Change profile photo"
			>
				{#if profile.avatarUrl}
					<img src={profile.avatarUrl} alt={profile.username} class="avatar" />
				{:else}
					<div class="avatar avatar-fallback">
						{getInitial(profile)}
					</div>
				{/if}
				{#if isUploadingAvatar}
					<div class="avatar-overlay" aria-hidden="true">
						<svg class="spinner" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
							<path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
						</svg>
					</div>
				{/if}
			</div>

			{#if avatarError}
				<p class="avatar-error" role="alert">{avatarError}</p>
			{/if}

			<!-- Name / bio (view or edit mode) -->
			{#if isEditing}
				<form
					class="edit-form"
					aria-label="Edit profile"
					onsubmit={(e) => { e.preventDefault(); saveEdit(); }}
				>
					<input
						type="text"
						class="edit-input"
						bind:value={editName}
						placeholder="Your name"
						maxlength={80}
						disabled={isSaving}
						aria-label="Display name"
					/>
					<textarea
						class="edit-textarea"
						bind:value={editBio}
						placeholder="Write something about yourself..."
						maxlength={280}
						rows={3}
						disabled={isSaving}
						aria-label="Bio"
					></textarea>
					{#if saveError}
						<p class="save-error" role="alert">{saveError}</p>
					{/if}
					<div class="edit-actions">
						<button type="submit" class="edit-save-btn" disabled={isSaving}>
							{isSaving ? 'Saving...' : 'Save'}
						</button>
						<button type="button" class="edit-cancel-btn" onclick={cancelEdit} disabled={isSaving}>
							Cancel
						</button>
					</div>
				</form>
			{:else}
				<div class="identity">
					<div class="name-row">
						{#if profile.name}
							<span class="display-name">{profile.name}</span>
						{/if}
						<button class="edit-icon-btn" onclick={startEdit} aria-label="Edit profile">
							<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
								<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
								<path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
							</svg>
						</button>
					</div>
					<span class="username">@{profile.username}</span>
					{#if profile.bio}
						<p class="bio">{profile.bio}</p>
					{/if}
				</div>
			{/if}
		</section>

		<!-- Sentinel element: when this scrolls off screen the sticky header appears -->
		<div bind:this={sentinelEl} class="scroll-sentinel" aria-hidden="true"></div>

		<!-- Stats row -->
		<div class="stats-row" role="group" aria-label="Profile stats">
			<div class="stat-item">
				<span class="stat-value">{formatCount(posts.length)}</span>
				<span class="stat-label">Posts</span>
			</div>
			<button
				class="stat-item"
				onclick={() => openStatsSheet('followers')}
				aria-label="View followers"
			>
				<span class="stat-value">{formatCount(followersCount)}</span>
				<span class="stat-label">Followers</span>
			</button>
			<button
				class="stat-item"
				onclick={() => openStatsSheet('following')}
				aria-label="View following"
			>
				<span class="stat-value">{formatCount(followingCount)}</span>
				<span class="stat-label">Following</span>
			</button>
		</div>

		<!-- Posts section -->
		{#if postsError}
			<div class="posts-error" role="alert">
				<p class="error-text">{postsError}</p>
				<button class="retry-btn" onclick={() => { if (profile) userService.getUserPosts(profile.username).then(r => { posts = r.content; postsError = null; }).catch(err => { postsError = err instanceof Error ? err.message : 'Failed to load posts'; }); }}>
					Try again
				</button>
			</div>
		{:else if posts.length === 0}
			<div class="posts-empty">
				<p class="posts-empty-text">No posts yet.</p>
			</div>
		{:else}
			<div class="profile-feed" role="list" aria-label="Posts">
				{#each posts as post (post.id)}
					<article class="profile-post-card" role="listitem" use:trackView={post.id}>
						<p class="profile-post-content">{post.content}</p>

						{#if post.photos && post.photos.length > 0}
							<div class="profile-photo-strip">
								{#each post.photos.slice().sort((a, b) => a.position - b.position) as photo (photo.id)}
									<div class="profile-photo-item">
										<img src={photo.url} alt="" loading="lazy" />
									</div>
								{/each}
							</div>
						{/if}

						<!-- Reactions + comment -->
						<div class="profile-action-row">
							{#each REACTION_TYPES as type (type)}
								{@const rs = getReactionState(post.id)}
								{@const reactionVisual = REACTION_VISUALS[type]}
								{@const isActive = rs.myReaction === type}
								{@const isPopping = rs.poppingType === type}
								<button
									class="p-reaction-btn"
									class:active={isActive}
									onclick={() => handleReaction(post, type)}
									aria-label="{type} reaction, count {rs.reactions[type]}"
								>
									{#if reactionVisual.assetPath}
										<img
											src={reactionVisual.assetPath}
											alt={reactionVisual.label}
											class="p-reaction-img"
											class:p-reaction-popping={isPopping}
											loading="lazy"
										/>
									{:else}
										<span
											class="p-reaction-emoji"
											class:p-reaction-popping={isPopping}
											aria-hidden="true"
										>
											{reactionVisual.fallbackEmoji}
										</span>
									{/if}
									{#if rs.reactions[type] > 0}
										<span class="p-reaction-count">{rs.reactions[type]}</span>
									{/if}
								</button>
							{/each}

							<span class="p-view-count" aria-label="{post.viewCount} views">
							<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
								<path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
								<circle cx="12" cy="12" r="3"/>
							</svg>
							{formatViews(post.viewCount)}
						</span>

						<button
								class="p-reaction-btn p-comment-btn"
								onclick={() => (openCommentSheetPostId = post.id)}
								aria-label="Comments"
							>
								<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
									<path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
								</svg>
							</button>

							<span class="profile-post-time">{formatRelativeTime(post.createdAt)}</span>
						</div>
					</article>
				{/each}
			</div>
		{/if}

	{/if}
</div>

<!-- Comment sheet -->
<CommentSheet
	postId={openCommentSheetPostId ?? 0}
	open={openCommentSheetPostId !== null}
	onClose={() => (openCommentSheetPostId = null)}
/>

<!-- ============================================================
     Stats bottom sheet (followers / following)
     ============================================================ -->
<div
	class="backdrop"
	class:open={statsSheetType !== null}
	role="presentation"
	onclick={closeStatsSheet}
></div>

<div
	class="sheet"
	class:open={statsSheetType !== null}
	bind:this={statsSheetEl}
	role="dialog"
	aria-modal="true"
	aria-label={statsSheetType === 'followers' ? 'Followers' : 'Following'}
>
	<div
		class="sheet-handle-area"
		role="presentation"
		ontouchstart={onStatsTouchStart}
		ontouchmove={onStatsTouchMove}
		ontouchend={onStatsTouchEnd}
	>
		<div class="drag-handle"></div>
		<h2 class="sheet-title">{statsSheetType === 'followers' ? 'Followers' : 'Following'}</h2>
	</div>

	<div class="sheet-body">
		{#if listsLoading}
			{#each { length: 4 } as _, i (i)}
				<div class="user-skeleton" aria-hidden="true">
					<div class="skeleton-avatar-sm"></div>
					<div class="skeleton-text-block">
						<div class="skeleton-line wide"></div>
						<div class="skeleton-line narrow"></div>
					</div>
				</div>
			{/each}
		{:else}
			{@const list = statsSheetType === 'followers' ? followersList : followingList}
			{#if list.length === 0}
				<p class="sheet-empty">
					{statsSheetType === 'followers' ? 'No followers yet.' : 'Not following anyone yet.'}
				</p>
			{:else}
				{#each list as user (user.id)}
					<a
						href="/{user.username}"
						class="user-row"
						onclick={closeStatsSheet}
					>
						<div class="user-avatar-sm" aria-hidden="true">
							{#if user.avatarUrl}
								<img src={user.avatarUrl} alt={user.username} class="user-avatar-img" />
							{:else}
								<span class="user-avatar-initial">{getInitialFromParts(user.name, user.username)}</span>
							{/if}
						</div>
						<div class="user-info">
							<span class="user-display-name">{user.name ?? user.username}</span>
							<span class="user-handle">@{user.username}</span>
						</div>
					</a>
				{/each}
			{/if}
		{/if}
	</div>
</div>

<!-- ============================================================
     Post detail bottom sheet
     ============================================================ -->
<div
	class="backdrop"
	class:open={selectedPost !== null}
	role="presentation"
	onclick={closePostSheet}
></div>

<div
	class="sheet sheet-tall"
	class:open={selectedPost !== null}
	bind:this={postSheetEl}
	role="dialog"
	aria-modal="true"
	aria-label="Post detail"
>
	<div
		class="sheet-handle-area"
		role="presentation"
		ontouchstart={onPostTouchStart}
		ontouchmove={onPostTouchMove}
		ontouchend={onPostTouchEnd}
	>
		<div class="drag-handle"></div>
	</div>

	{#if selectedPost && profile}
		<div class="sheet-body post-detail-body">
			<!-- Author row -->
			<div class="post-author-row">
				{#if profile.avatarUrl}
					<img src={profile.avatarUrl} alt={profile.username} class="post-author-avatar" />
				{:else}
					<div class="post-author-avatar post-author-avatar-fallback">
						{getInitial(profile)}
					</div>
				{/if}
				<div class="post-author-info">
					<span class="post-author-name">{profile.name ?? profile.username}</span>
					<span class="post-author-handle">@{profile.username}</span>
				</div>
			</div>

			<!-- Content -->
			<p class="post-detail-content">{selectedPost.content}</p>

			<!-- Photos strip -->
			{#if selectedPost.photos.length > 0}
				<div class="post-photos-strip">
					{#each selectedPost.photos.slice().sort((a, b) => a.position - b.position) as photo (photo.id)}
						<img
							src={photo.url}
							alt=""
							class="post-photo"
							loading="lazy"
						/>
					{/each}
				</div>
			{/if}

			<!-- Meta -->
			<p class="post-meta">{new Date(selectedPost.createdAt).toLocaleString()}</p>
		</div>
	{/if}
</div>

<!-- ============================================================
     Settings bottom sheet
     ============================================================ -->
<div
	class="backdrop"
	class:open={settingsOpen}
	role="presentation"
	onclick={closeSettings}
></div>

<div
	class="sheet sheet-settings"
	class:open={settingsOpen}
	bind:this={settingsSheetEl}
	role="dialog"
	aria-modal="true"
	aria-label="Settings"
>
	<div
		class="sheet-handle-area"
		role="presentation"
		ontouchstart={onSettingsTouchStart}
		ontouchmove={onSettingsTouchMove}
		ontouchend={onSettingsTouchEnd}
	>
		<div class="drag-handle"></div>
		<h2 class="sheet-title">Settings</h2>
	</div>

	<div class="sheet-body settings-body">
		<button class="settings-row settings-row-link" onclick={openSupport}>
			<span>Support</span>
			<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
				<path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
				<polyline points="15 3 21 3 21 9"/>
				<line x1="10" y1="14" x2="21" y2="3"/>
			</svg>
		</button>
		<button class="settings-row settings-row-link" onclick={openDesktopVersion}>
			<span>Desktop Version</span>
			<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
				<path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6"/>
				<polyline points="15 3 21 3 21 9"/>
				<line x1="10" y1="14" x2="21" y2="3"/>
			</svg>
		</button>
		<button class="settings-row settings-row-danger" onclick={handleLogout}>
			Log out
		</button>
		<div class="settings-row settings-row-info" aria-label="App version">
			<span class="settings-row-label">Version</span>
			<span class="settings-row-value">Critiqal v0.1</span>
		</div>
	</div>
</div>

<style>
	/* Screen-reader only utility */
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

	/* ------------------------------------------------------------------ */
	/* Scroll container                                                     */
	/* ------------------------------------------------------------------ */

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

	/* ------------------------------------------------------------------ */
	/* Sticky header                                                        */
	/* ------------------------------------------------------------------ */

	.sticky-header {
		position: sticky;
		top: 0;
		background: var(--color-bg, #0f0f0f);
		z-index: 10;
		padding: 12px 16px;
		display: flex;
		align-items: center;
		justify-content: space-between;
		opacity: 0;
		transition: opacity 0.2s ease;
		pointer-events: none;
	}

	.sticky-header.visible {
		opacity: 1;
		pointer-events: auto;
	}

	.sticky-username {
		font-size: 15px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
	}

	.settings-btn {
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-secondary, rgba(240, 240, 240, 0.6));
		padding: 8px;
		min-width: 44px;
		min-height: 44px;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	/* ------------------------------------------------------------------ */
	/* Loading skeleton                                                     */
	/* ------------------------------------------------------------------ */

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

	.skeleton-bio.short {
		width: 140px;
	}

	@keyframes shimmer {
		0%, 100% { opacity: 0.5; }
		50% { opacity: 1; }
	}

	/* ------------------------------------------------------------------ */
	/* Error / retry                                                        */
	/* ------------------------------------------------------------------ */

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

	.retry-btn:active {
		opacity: 0.7;
	}

	/* ------------------------------------------------------------------ */
	/* Profile header                                                       */
	/* ------------------------------------------------------------------ */

	.profile-header {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: calc(var(--tg-safe-area-inset-top, 0px) + 4px) 24px 10px;
		gap: 6px;
		position: relative;
	}

	.settings-btn-standalone {
		position: absolute;
		top: calc(var(--tg-safe-area-inset-top, 0px) + 4px);
		right: 8px;
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-secondary, rgba(240, 240, 240, 0.6));
		padding: 8px;
		min-width: 44px;
		min-height: 44px;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.avatar-wrap {
		cursor: pointer;
		border-radius: 50%;
		overflow: hidden;
		width: 80px;
		height: 80px;
		flex-shrink: 0;
		position: relative;
	}

	.avatar {
		width: 80px;
		height: 80px;
		border-radius: 50%;
		object-fit: cover;
		display: block;
	}

	.avatar-fallback {
		background: var(--color-surface-raised, #242424);
		color: rgba(240, 240, 240, 0.7);
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 28px;
		font-weight: 600;
		user-select: none;
	}

	.avatar-overlay {
		position: absolute;
		inset: 0;
		border-radius: 50%;
		background: rgba(0, 0, 0, 0.45);
		display: flex;
		align-items: center;
		justify-content: center;
		color: #fff;
	}

	.spinner {
		width: 20px;
		height: 20px;
		animation: spin 0.9s linear infinite;
	}

	@keyframes spin {
		from { transform: rotate(0deg); }
		to { transform: rotate(360deg); }
	}

	.avatar-error {
		font-size: 12px;
		color: #e05252;
		text-align: center;
		margin: 0;
	}

	.identity {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 2px;
		text-align: center;
	}

	.name-row {
		display: flex;
		align-items: center;
		gap: 6px;
	}

	.edit-icon-btn {
		background: none;
		border: none;
		cursor: pointer;
		padding: 0;
		min-width: 44px;
		min-height: 44px;
		color: rgba(240, 240, 240, 0.4);
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 6px;
		transition: color 0.15s ease;
		-webkit-tap-highlight-color: transparent;
	}

	.edit-icon-btn:active {
		color: rgba(240, 240, 240, 0.9);
	}

	.display-name {
		font-size: 18px;
		font-weight: 700;
		color: var(--tg-theme-text-color, var(--color-text-primary, #f0f0f0));
		line-height: 1.2;
		margin: 0;
	}

	.username {
		font-size: 13px;
		color: var(--tg-theme-hint-color, rgba(240, 240, 240, 0.45));
		margin: 0;
	}

	.bio {
		font-size: 14px;
		color: var(--tg-theme-hint-color, rgba(240, 240, 240, 0.65));
		line-height: 1.4;
		margin: 0;
		max-width: 260px;
		text-align: center;
		word-break: break-word;
	}

	/* ------------------------------------------------------------------ */
	/* Edit form                                                            */
	/* ------------------------------------------------------------------ */

	.edit-form {
		width: 100%;
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.edit-input,
	.edit-textarea {
		width: 100%;
		box-sizing: border-box;
		background: var(--color-surface-raised, #242424);
		border: 1px solid var(--color-border, rgba(255, 255, 255, 0.15));
		border-radius: 10px;
		padding: 10px 12px;
		font-size: 14px;
		color: var(--color-text-primary, #f0f0f0);
		font-family: inherit;
		outline: none;
		resize: none;
	}

	.edit-input::placeholder,
	.edit-textarea::placeholder {
		color: rgba(255, 255, 255, 0.3);
	}

	.edit-input:focus,
	.edit-textarea:focus {
		border-color: rgba(255, 255, 255, 0.35);
	}

	.edit-input:disabled,
	.edit-textarea:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}

	.save-error {
		font-size: 13px;
		color: #e05252;
		margin: 0;
		text-align: center;
	}

	.edit-actions {
		display: flex;
		gap: 8px;
	}

	.edit-save-btn {
		flex: 1;
		height: 36px;
		border-radius: 10px;
		background: var(--color-text-primary, #f0f0f0);
		border: none;
		color: var(--color-bg, #0f0f0f);
		font-size: 14px;
		font-weight: 600;
		cursor: pointer;
		font-family: inherit;
	}

	.edit-save-btn:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	.edit-cancel-btn {
		flex: 1;
		height: 36px;
		border-radius: 10px;
		background: none;
		border: 1px solid var(--color-border, rgba(255, 255, 255, 0.15));
		color: var(--color-text-primary, #f0f0f0);
		font-size: 14px;
		font-weight: 500;
		cursor: pointer;
		font-family: inherit;
	}

	.edit-cancel-btn:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	/* ------------------------------------------------------------------ */
	/* Scroll sentinel                                                      */
	/* ------------------------------------------------------------------ */

	.scroll-sentinel {
		height: 1px;
		width: 100%;
		pointer-events: none;
	}

	/* ------------------------------------------------------------------ */
	/* Stats row                                                            */
	/* ------------------------------------------------------------------ */

	.stats-row {
		display: flex;
		width: 100%;
		justify-content: space-around;
		padding: 4px 0 8px;
		margin: 0;
		border: none;
	}

	.stat-item {
		flex: 1;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 4px 0;
		cursor: pointer;
		gap: 1px;
		background: none;
		border: none;
		font-family: inherit;
	}

	.stat-value {
		font-size: 16px;
		font-weight: 700;
		color: var(--tg-theme-text-color, var(--color-text-primary, #f0f0f0));
	}

	.stat-label {
		font-size: 10px;
		color: var(--tg-theme-hint-color, rgba(240, 240, 240, 0.4));
		text-transform: uppercase;
		letter-spacing: 0.4px;
	}

	/* ------------------------------------------------------------------ */
	/* Profile feed (replaces grid)                                        */
	/* ------------------------------------------------------------------ */

	.profile-feed {
		display: flex;
		flex-direction: column;
	}

	.profile-post-card {
		padding: 14px 16px 10px;
		border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.08));
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
	}

	.profile-post-card:active {
		background: rgba(255, 255, 255, 0.03);
	}

	.profile-post-content {
		font-size: 15px;
		line-height: 1.5;
		color: var(--color-text-primary, #f0f0f0);
		margin: 0 0 10px;
		word-break: break-word;
	}

	.profile-photo-strip {
		display: flex;
		overflow-x: auto;
		scroll-snap-type: x mandatory;
		scroll-behavior: smooth;
		-webkit-overflow-scrolling: touch;
		scrollbar-width: none;
		border-radius: 12px;
		overflow: hidden;
		margin-bottom: 8px;
	}

	.profile-photo-strip::-webkit-scrollbar {
		display: none;
	}

	.profile-photo-item {
		flex-shrink: 0;
		width: 100%;
		aspect-ratio: 4 / 3;
		scroll-snap-align: start;
	}

	.profile-photo-item img {
		width: 100%;
		height: 100%;
		object-fit: cover;
		display: block;
	}

	.profile-action-row {
		display: flex;
		align-items: center;
		gap: 2px;
		margin-top: 6px;
	}

	.p-reaction-btn {
		min-width: 44px;
		min-height: 36px;
		background: none;
		border: none;
		cursor: pointer;
		display: flex;
		align-items: center;
		gap: 4px;
		padding: 5px 8px;
		border-radius: 8px;
		transition: background 0.15s ease;
		-webkit-tap-highlight-color: transparent;
	}

	.p-reaction-btn.active {
		background: rgba(224, 82, 82, 0.18);
	}

	.p-reaction-btn:active {
		background: rgba(255, 255, 255, 0.06);
	}

	.p-reaction-img {
		width: 20px;
		height: 20px;
		object-fit: contain;
	}

	.p-reaction-emoji {
		width: 20px;
		height: 20px;
		display: inline-flex;
		align-items: center;
		justify-content: center;
		font-size: 17px;
		line-height: 1;
	}

	.p-reaction-count {
		font-size: 12px;
		font-weight: 500;
		color: rgba(240, 240, 240, 0.6);
	}

	.p-reaction-btn.active .p-reaction-count {
		color: #e05252;
	}

	.p-view-count {
		display: flex;
		align-items: center;
		gap: 4px;
		margin-left: auto;
		font-size: 12px;
		color: rgba(240, 240, 240, 0.38);
		user-select: none;
	}

	.p-comment-btn {
		color: rgba(240, 240, 240, 0.4);
	}

	.profile-post-time {
		font-size: 12px;
		color: rgba(240, 240, 240, 0.3);
		margin-left: 4px;
	}

	@keyframes profileReactionPop {
		0% { transform: scale(1); }
		50% { transform: scale(1.4); }
		100% { transform: scale(1); }
	}

	:global(.p-reaction-popping) {
		animation: profileReactionPop 300ms ease-out;
	}

	/* ------------------------------------------------------------------ */
	/* Posts error / empty                                                  */
	/* ------------------------------------------------------------------ */

	.posts-error,
	.posts-empty {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 10px;
		padding: 40px 16px;
	}

	.posts-empty-text {
		font-size: 14px;
		color: rgba(255, 255, 255, 0.4);
		margin: 0;
	}

	/* ------------------------------------------------------------------ */
	/* Shared sheet / backdrop                                              */
	/* ------------------------------------------------------------------ */

	.backdrop {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.5);
		backdrop-filter: blur(4px);
		z-index: 149;
		opacity: 0;
		pointer-events: none;
		transition: opacity 350ms ease;
	}

	.backdrop.open {
		opacity: 1;
		pointer-events: auto;
	}

	.sheet {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		height: 65dvh;
		border-radius: 24px 24px 0 0;
		background: var(--color-surface, #1a1a1a);
		z-index: 150;
		transform: translateY(100%);
		transition: transform 350ms cubic-bezier(0.32, 0.72, 0, 1);
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.sheet.open {
		transform: translateY(0);
	}

	.sheet-tall {
		height: 80dvh;
	}

	.sheet-settings {
		height: auto;
		min-height: 160px;
	}

	/* Disables transition while dragging so sheet tracks finger exactly */
	:global(.sheet.dragging) {
		transition: none !important;
	}

	.sheet-handle-area {
		flex-shrink: 0;
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 12px 16px 8px;
		cursor: grab;
		user-select: none;
		-webkit-user-select: none;
	}

	.drag-handle {
		width: 36px;
		height: 4px;
		border-radius: 2px;
		background: var(--color-border, rgba(255, 255, 255, 0.2));
		margin-bottom: 12px;
		flex-shrink: 0;
	}

	.sheet-title {
		font-size: 16px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
		margin: 0;
		align-self: flex-start;
	}

	.sheet-body {
		flex: 1;
		overflow-y: auto;
		-webkit-overflow-scrolling: touch;
	}

	.sheet-empty {
		padding: 32px 16px;
		text-align: center;
		font-size: 14px;
		color: rgba(255, 255, 255, 0.4);
	}

	/* ------------------------------------------------------------------ */
	/* User row (followers / following list)                                */
	/* ------------------------------------------------------------------ */

	.user-row {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 10px 16px;
		text-decoration: none;
		transition: background-color 0.15s ease;
	}

	.user-row:active {
		background: var(--color-surface-raised, #242424);
	}

	.user-avatar-sm {
		width: 40px;
		height: 40px;
		border-radius: 50%;
		background: var(--color-surface-raised, #242424);
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
		flex-shrink: 0;
	}

	.user-avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
		display: block;
	}

	.user-avatar-initial {
		font-size: 15px;
		font-weight: 600;
		color: rgba(255, 255, 255, 0.5);
		user-select: none;
	}

	.user-info {
		display: flex;
		flex-direction: column;
		gap: 1px;
		min-width: 0;
	}

	.user-display-name {
		font-size: 14px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.user-handle {
		font-size: 13px;
		color: rgba(255, 255, 255, 0.4);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	/* ------------------------------------------------------------------ */
	/* Skeleton rows                                                        */
	/* ------------------------------------------------------------------ */

	.user-skeleton {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 10px 16px;
	}

	.skeleton-avatar-sm {
		width: 40px;
		height: 40px;
		border-radius: 50%;
		background: var(--color-skeleton, rgba(255, 255, 255, 0.08));
		animation: shimmer 1.6s ease-in-out infinite;
		flex-shrink: 0;
	}

	.skeleton-text-block {
		flex: 1;
		display: flex;
		flex-direction: column;
		gap: 6px;
	}

	.skeleton-line {
		height: 12px;
		border-radius: 4px;
		background: var(--color-skeleton, rgba(255, 255, 255, 0.08));
		animation: shimmer 1.6s ease-in-out infinite;
	}

	.skeleton-line.wide {
		width: 110px;
	}

	.skeleton-line.narrow {
		width: 72px;
	}

	/* ------------------------------------------------------------------ */
	/* Post detail sheet body                                               */
	/* ------------------------------------------------------------------ */

	.post-detail-body {
		padding: 0 16px 24px;
	}

	.post-author-row {
		display: flex;
		align-items: center;
		gap: 10px;
		margin-bottom: 12px;
	}

	.post-author-avatar {
		width: 36px;
		height: 36px;
		border-radius: 50%;
		object-fit: cover;
		background: var(--color-surface-raised, #242424);
		display: block;
		flex-shrink: 0;
	}

	.post-author-avatar-fallback {
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 14px;
		font-weight: 600;
		color: rgba(255, 255, 255, 0.5);
		user-select: none;
	}

	.post-author-info {
		display: flex;
		flex-direction: column;
		gap: 1px;
		min-width: 0;
	}

	.post-author-name {
		font-size: 14px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
	}

	.post-author-handle {
		font-size: 12px;
		color: rgba(255, 255, 255, 0.4);
	}

	.post-detail-content {
		font-size: 15px;
		line-height: 1.5;
		color: var(--color-text-primary, #f0f0f0);
		margin: 0 0 14px;
		word-break: break-word;
		white-space: pre-wrap;
	}

	.post-photos-strip {
		display: flex;
		gap: 6px;
		overflow-x: auto;
		-webkit-overflow-scrolling: touch;
		scrollbar-width: none;
		margin-bottom: 14px;
	}

	.post-photos-strip::-webkit-scrollbar {
		display: none;
	}

	.post-photo {
		flex-shrink: 0;
		height: 240px;
		width: auto;
		border-radius: 10px;
		object-fit: cover;
		display: block;
	}

	.post-meta {
		font-size: 12px;
		color: rgba(255, 255, 255, 0.35);
		margin: 0;
	}

	/* ------------------------------------------------------------------ */
	/* Settings sheet body                                                  */
	/* ------------------------------------------------------------------ */

	.settings-body {
		display: flex;
		flex-direction: column;
		padding-bottom: calc(16px + env(safe-area-inset-bottom, 0px));
	}

	.settings-row {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 16px 16px;
		font-size: 16px;
		font-family: inherit;
		cursor: pointer;
		border: none;
		background: none;
		width: 100%;
		text-align: left;
		border-top: 1px solid var(--color-border, rgba(255, 255, 255, 0.08));
	}

	.settings-row:first-child {
		border-top: none;
	}

	.settings-row-link {
		color: var(--color-text-primary, #f0f0f0);
		font-weight: 400;
	}

	.settings-row-link:active {
		background: rgba(255, 255, 255, 0.05);
	}

	.settings-row-danger {
		color: #e05252;
		font-weight: 500;
	}

	.settings-row-danger:active {
		background: rgba(224, 82, 82, 0.08);
	}

	.settings-row-info {
		cursor: default;
	}

	.settings-row-label {
		font-size: 14px;
		color: rgba(255, 255, 255, 0.5);
	}

	.settings-row-value {
		font-size: 14px;
		color: rgba(255, 255, 255, 0.35);
	}
</style>
