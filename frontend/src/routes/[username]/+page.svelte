<script lang="ts">
	import { page } from '$app/stores';
	import { onMount } from 'svelte';
	import { fade } from 'svelte/transition';
	import type { Post, User } from '$lib/types';
	import { userService, mediaService } from '$lib/services';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';
	import PostCard from '$lib/components/PostCard.svelte';
	import PostCardSkeleton from '$lib/components/PostCardSkeleton.svelte';

	const username = $page.params.username as string;

	type ProfileState = 'loading' | 'loaded' | 'error';
	type ModalTab = 'followers' | 'following';

	// --- Posts paginated state ---
	let posts = $state<Post[]>([]);
	let postsPage = $state(0);
	let postsHasNext = $state(false);
	let postsLoading = $state(false);
	let postsLoadingMore = $state(false);
	let postsError = $state<string | null>(null);

	let profile = $state<User | null>(null);
	let followersList = $state<User[]>([]);
	let followingList = $state<User[]>([]);
	let followersCount = $derived(followersList.length);
	let followingCount = $derived(followingList.length);
	let profileState = $state<ProfileState>('loading');
	let profileError = $state('');
	let isFollowing = $state(false);
	let isFollowLoading = $state(false);
	let listsLoading = $state(false);

	let modalOpen = $state(false);
	let modalTab = $state<ModalTab>('followers');

	function openModal(tab: ModalTab): void {
		modalTab = tab;
		modalOpen = true;
	}

	function closeModal(): void {
		modalOpen = false;
	}

	function handleBackdropKeydown(e: KeyboardEvent): void {
		if (e.key === 'Escape') closeModal();
	}

	function handleBackdropClick(e: MouseEvent): void {
		if (e.target === e.currentTarget) closeModal();
	}

	let isUploadingAvatar = $state(false);
	let avatarError = $state('');

	let isEditing = $state(false);
	let editName = $state('');
	let editBio = $state('');
	let isSaving = $state(false);
	let saveError = $state('');

	const isOwnProfile = $derived(authStore.user?.username === username);
	const SKELETON_COUNT = 3;

	onMount(() => {
		loadProfile();
	});

	async function loadProfile(): Promise<void> {
		profileState = 'loading';
		profileError = '';
		try {
			const user = await userService.getProfile(username);
			profile = user;
			profileState = 'loaded';
			loadCounts(user.id);
			loadPosts();
		} catch (err: unknown) {
			profileError = err instanceof Error ? err.message : 'User not found.';
			profileState = 'error';
		}
	}

	async function loadCounts(userId: number): Promise<void> {
		listsLoading = true;
		try {
			const [followers, following] = await Promise.all([
				userService.getFollowers(userId),
				userService.getFollowing(userId)
			]);
			followersList = followers;
			followingList = following;
			if (authStore.user) {
				isFollowing = followers.some((f) => f.id === authStore.user!.id);
			}
		} catch {
			// Non-critical — lists stay empty
		} finally {
			listsLoading = false;
		}
	}

	function infiniteScroll(el: HTMLElement): { destroy: () => void } {
		const obs = new IntersectionObserver(
			([entry]) => {
				if (entry.isIntersecting && !postsLoadingMore) loadMorePosts();
			},
			{ threshold: 0.1 }
		);
		obs.observe(el);
		return { destroy: () => obs.disconnect() };
	}

	async function loadPosts(): Promise<void> {
		postsLoading = true;
		postsError = null;
		posts = [];
		postsPage = 0;
		postsHasNext = false;
		try {
			const res = await userService.getUserPosts(username, 0);
			posts = res.content;
			postsHasNext = res.hasNext;
		} catch {
			postsError = 'Could not load posts.';
		} finally {
			postsLoading = false;
		}
	}

	async function loadMorePosts(): Promise<void> {
		if (!postsHasNext || postsLoadingMore) return;
		postsLoadingMore = true;
		try {
			const nextPage = postsPage + 1;
			const res = await userService.getUserPosts(username, nextPage);
			posts = [...posts, ...res.content];
			postsPage = nextPage;
			postsHasNext = res.hasNext;
		} catch {
			// non-fatal
		} finally {
			postsLoadingMore = false;
		}
	}

	function handlePostDeleted(postId: number): void {
		posts = posts.filter((p) => p.id !== postId);
	}

	async function handleFollow(): Promise<void> {
		if (!authStore.isAuthenticated || !profile || isFollowLoading) return;

		const currentUser = authStore.user!;
		const wasFollowing = isFollowing;
		const prevFollowersList = followersList;

		isFollowing = !isFollowing;
		followersList = isFollowing
			? [...followersList, currentUser]
			: followersList.filter((f) => f.id !== currentUser.id);

		isFollowLoading = true;
		try {
			if (isFollowing) {
				await userService.follow(profile.id);
			} else {
				await userService.unfollow(profile.id);
			}
		} catch {
			isFollowing = wasFollowing;
			followersList = prevFollowersList;
		} finally {
			isFollowLoading = false;
		}
	}

	function startEdit(): void {
		if (!profile) return;
		editName = profile.name ?? '';
		editBio = profile.bio ?? '';
		isEditing = true;
		saveError = '';
	}

	function cancelEdit(): void {
		isEditing = false;
		saveError = '';
	}

	async function saveEdit(): Promise<void> {
		isSaving = true;
		saveError = '';
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
			saveError = err instanceof Error ? err.message : 'Failed to save changes.';
		} finally {
			isSaving = false;
		}
	}

	async function handleAvatarUpload(e: Event): Promise<void> {
		const input = e.target as HTMLInputElement;
		const file = input.files?.[0];
		if (!file || !profile) return;

		isUploadingAvatar = true;
		avatarError = '';
		try {
			const result = await mediaService.uploadAvatar(file);
			profile = { ...profile, avatarUrl: result.avatarUrl };
			authStore.updateUser({ ...authStore.user!, avatarUrl: result.avatarUrl });
		} catch (err: unknown) {
			avatarError = err instanceof Error ? err.message : 'Upload failed.';
		} finally {
			isUploadingAvatar = false;
			input.value = '';
		}
	}

	function getAvatarInitial(user: User): string {
		return (user.name ?? user.username).charAt(0).toUpperCase();
	}

	function formatCount(n: number): string {
		if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
		if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
		return String(n);
	}
</script>

<svelte:head>
	<title>
		{profile ? `${profile.name ?? profile.username} (@${profile.username})` : `@${username}`} — Critiqal
	</title>
	<meta name="description" content={profile?.bio ?? `${username}'s profile on Critiqal`} />
</svelte:head>

<div class="page-layout">
	<div class="col-left">
		<LeftSidebar />
	</div>

	<main class="col-center" aria-label="Profile">
		<header class="page-header">
			<h1 class="page-title">
				{#if profileState === 'loaded' && profile}
					{profile.name ?? `@${profile.username}`}
				{:else}
					@{username}
				{/if}
			</h1>
			{#if !postsLoading && posts.length > 0}
				<span class="post-count-label">{formatCount(posts.length)} posts</span>
			{/if}
		</header>

		{#if profileState === 'loading'}
			<div class="profile-skeleton" aria-hidden="true">
				<div class="skeleton-avatar-lg"></div>
				<div class="skeleton-info">
					<div class="skeleton-line" style="width: 8rem; height: 1rem;"></div>
					<div class="skeleton-line" style="width: 5rem; height: 0.75rem; margin-top: 0.25rem;"></div>
					<div class="skeleton-line" style="width: 100%; height: 0.75rem; margin-top: 0.875rem;"></div>
					<div class="skeleton-line" style="width: 72%; height: 0.75rem; margin-top: 0.375rem;"></div>
				</div>
			</div>
		{:else if profileState === 'error'}
			<div class="state-box" role="alert">
				<p class="state-title">User not found</p>
				<p class="state-body">{profileError}</p>
				<button class="retry-btn" onclick={loadProfile}>Try again</button>
			</div>
		{:else if profile}
			<section class="profile-section" aria-label="Profile info">
				<div class="profile-top">
					{#if isOwnProfile}
						<label class="avatar-upload-label" aria-label="Upload avatar" title="Change profile photo">
							<input
								type="file"
								accept="image/jpeg,image/png,image/webp"
								class="avatar-file-input"
								onchange={handleAvatarUpload}
								disabled={isUploadingAvatar}
							/>
							<div class="avatar-lg" aria-hidden="true">
								{#if profile.avatarUrl}
									<img src={profile.avatarUrl} alt={profile.username} class="avatar-img" />
								{:else}
									<span class="avatar-initial">{getAvatarInitial(profile)}</span>
								{/if}
								{#if isUploadingAvatar}
									<div class="avatar-spinner-overlay" aria-hidden="true">
										<svg class="avatar-spinner" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
											<path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
										</svg>
									</div>
								{:else}
									<div class="avatar-camera-overlay" aria-hidden="true">
										<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
											<path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
											<circle cx="12" cy="13" r="4" />
										</svg>
									</div>
								{/if}
							</div>
						</label>
						{#if avatarError}
							<p class="avatar-error" role="alert">{avatarError}</p>
						{/if}
					{:else}
						<div class="avatar-lg" aria-hidden="true">
							{#if profile.avatarUrl}
								<img src={profile.avatarUrl} alt={profile.username} class="avatar-img" />
							{:else}
								<span class="avatar-initial">{getAvatarInitial(profile)}</span>
							{/if}
						</div>
					{/if}

					<div class="profile-action">
						{#if isOwnProfile && !isEditing}
							<button class="action-btn btn-outline" onclick={startEdit}>Edit profile</button>
						{:else if !isOwnProfile && authStore.isAuthenticated}
							<button
								class="action-btn"
								class:btn-follow={!isFollowing}
								class:btn-following={isFollowing}
								onclick={handleFollow}
								disabled={isFollowLoading}
								aria-pressed={isFollowing}
							>
								{isFollowing ? 'Following' : 'Follow'}
							</button>
						{/if}
					</div>
				</div>

				{#if isEditing}
					<form class="edit-form" aria-label="Edit profile" onsubmit={(e) => { e.preventDefault(); saveEdit(); }}>
						<div class="edit-field">
							<label for="edit-name" class="edit-label">Name</label>
							<input
								id="edit-name"
								type="text"
								class="edit-input"
								bind:value={editName}
								placeholder="Your name"
								maxlength={80}
								disabled={isSaving}
							/>
						</div>
						<div class="edit-field">
							<label for="edit-bio" class="edit-label">Bio</label>
							<textarea
								id="edit-bio"
								class="edit-textarea"
								bind:value={editBio}
								placeholder="Write something about yourself..."
								maxlength={280}
								rows={3}
								disabled={isSaving}
							></textarea>
						</div>
						{#if saveError}
							<p class="save-error" role="alert">{saveError}</p>
						{/if}
						<div class="edit-actions">
							<button type="submit" class="action-btn btn-follow" disabled={isSaving}>
								{isSaving ? 'Saving...' : 'Save'}
							</button>
							<button type="button" class="action-btn btn-outline" onclick={cancelEdit} disabled={isSaving}>
								Cancel
							</button>
						</div>
					</form>
				{:else}
					<div class="profile-info">
						<p class="profile-name">{profile.name ?? profile.username}</p>
						<p class="profile-username">@{profile.username}</p>
						{#if profile.bio}
							<p class="profile-bio">{profile.bio}</p>
						{/if}
					</div>
				{/if}

				<div class="profile-stats">
					<button class="stat-item" onclick={() => openModal('followers')}>
						<span class="stat-value">{formatCount(followersCount)}</span>
						<span class="stat-label">Followers</span>
					</button>
					<button class="stat-item" onclick={() => openModal('following')}>
						<span class="stat-value">{formatCount(followingCount)}</span>
						<span class="stat-label">Following</span>
					</button>
				</div>
			</section>
		{/if}

		<div class="posts-divider" aria-hidden="true"></div>

		{#if postsLoading}
			<div aria-busy="true" aria-label="Loading posts">
				{#each { length: SKELETON_COUNT } as _, i (i)}
					<PostCardSkeleton />
				{/each}
			</div>
		{:else if postsError}
			<div class="state-box">
				<p class="state-body">{postsError}</p>
				<button class="retry-btn" onclick={loadPosts}>Try again</button>
			</div>
		{:else if posts.length === 0}
			<div class="state-box">
				<p class="state-title">No posts yet</p>
				<p class="state-body">
					{#if isOwnProfile}Share something with the world.{:else}Nothing here yet.{/if}
				</p>
			</div>
		{:else}
			<div class="post-list">
				{#each posts as post (post.id)}
					<PostCard {post} onDeleted={handlePostDeleted} />
				{/each}
			</div>

			{#if postsHasNext}
				<div class="posts-sentinel" use:infiniteScroll></div>
			{/if}
			{#if postsLoadingMore}
				<div class="loading-more">Loading more…</div>
			{/if}
		{/if}
	</main>

	<div class="col-right" aria-hidden="true"></div>
</div>

{#if modalOpen}
	<div
		class="modal-backdrop"
		role="presentation"
		transition:fade={{ duration: 150 }}
		onclick={handleBackdropClick}
		onkeydown={handleBackdropKeydown}
	>
		<div
			class="modal-panel"
			role="dialog"
			aria-modal="true"
			aria-label={modalTab === 'followers' ? 'Followers' : 'Following'}
		>
			<header class="modal-header">
				<h2 class="modal-title">{modalTab === 'followers' ? 'Followers' : 'Following'}</h2>
				<button class="modal-close" onclick={closeModal} aria-label="Close">×</button>
			</header>

			<div class="modal-body">
				{#if listsLoading}
					{#each { length: 3 } as _, i (i)}
						<div class="user-skeleton" aria-hidden="true">
							<div class="skeleton-avatar-sm"></div>
							<div class="skeleton-text-group">
								<div class="skeleton-line" style="width: 7rem; height: 0.8125rem;"></div>
								<div class="skeleton-line" style="width: 4.5rem; height: 0.75rem; margin-top: 0.25rem;"></div>
							</div>
						</div>
					{/each}
				{:else}
					{@const list = modalTab === 'followers' ? followersList : followingList}
					{#if list.length === 0}
						<p class="modal-empty">
							{modalTab === 'followers' ? 'No followers yet.' : 'No one followed yet.'}
						</p>
					{:else}
						{#each list as user (user.id)}
							<a href="/{user.username}" class="user-row" onclick={closeModal}>
								<div class="user-avatar-sm" aria-hidden="true">
									{#if user.avatarUrl}
										<img src={user.avatarUrl} alt={user.username} class="avatar-img" />
									{:else}
										<span class="user-avatar-initial">{getAvatarInitial(user)}</span>
									{/if}
								</div>
								<div class="user-info">
									<span class="user-name">{user.name ?? user.username}</span>
									<span class="user-handle">@{user.username}</span>
								</div>
							</a>
						{/each}
					{/if}
				{/if}
			</div>
		</div>
	</div>
{/if}

<style>
	.page-layout {
		display: grid;
		grid-template-columns: 16rem 42rem 14rem;
		justify-content: center;
		height: 100vh;
		overflow: hidden;
	}

	.col-left {
		overflow-y: auto;
		padding: 0 1.5rem 0 1rem;
	}

	.col-center {
		overflow-y: auto;
		padding: 0 2rem;
	}

	.col-right {
		overflow-y: auto;
		padding: 1.5rem 1rem 1.5rem 1.5rem;
	}

	.col-left {
		position: sticky;
		top: 0;
		height: 100vh;
		overflow-y: auto;
		padding: 0 1.5rem 0 1rem;
	}

	.col-center {
		padding: 0 2rem;
		overflow-y: auto;
		scrollbar-width: none;
		-ms-overflow-style: none;
	}

	.col-center::-webkit-scrollbar {
		display: none;
	}

	.col-right {
		padding: 1.5rem 1rem 1.5rem 1.5rem;
	}

	/* Header */
	.page-header {
		padding: 1.25rem 0;
		border-bottom: 1px solid var(--color-border);
		position: sticky;
		top: 0;
		background-color: var(--color-bg);
		z-index: 10;
		display: flex;
		align-items: baseline;
		gap: 0.625rem;
	}

	.page-title {
		font-size: 1.0625rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.01em;
	}

	.post-count-label {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
	}

	/* Profile section */
	.profile-section {
		padding: 1.5rem 0;
		animation: fadeIn 0.2s ease-out;
	}

	.profile-top {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		margin-bottom: 1rem;
	}

	.avatar-lg {
		width: 5rem;
		height: 5rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
		flex-shrink: 0;
	}

	.avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.avatar-initial {
		font-size: 1.75rem;
		font-weight: 600;
		color: var(--color-text-muted);
		user-select: none;
	}

	.avatar-upload-label {
		position: relative;
		cursor: pointer;
		display: block;
		flex-shrink: 0;
	}

	.avatar-file-input {
		display: none;
	}

	.avatar-upload-label .avatar-lg {
		position: relative;
	}

	.avatar-camera-overlay {
		position: absolute;
		inset: 0;
		border-radius: 50%;
		background: rgba(0, 0, 0, 0.45);
		display: flex;
		align-items: center;
		justify-content: center;
		color: #fff;
		opacity: 0;
		transition: opacity 0.18s ease;
	}

	.avatar-upload-label:hover .avatar-camera-overlay {
		opacity: 1;
	}

	.avatar-spinner-overlay {
		position: absolute;
		inset: 0;
		border-radius: 50%;
		background: rgba(0, 0, 0, 0.5);
		display: flex;
		align-items: center;
		justify-content: center;
		color: #fff;
	}

	.avatar-spinner {
		width: 1.25rem;
		height: 1.25rem;
		animation: spin 0.9s linear infinite;
	}

	.avatar-error {
		font-size: 0.75rem;
		color: var(--color-accent);
		margin-top: 0.375rem;
		max-width: 5rem;
		text-align: center;
		line-height: 1.3;
	}

	@keyframes spin {
		from { transform: rotate(0deg); }
		to { transform: rotate(360deg); }
	}

	.profile-action {
		padding-top: 0.25rem;
	}

	/* Buttons */
	.action-btn {
		padding: 0.5rem 1.125rem;
		border-radius: 9999px;
		font-size: 0.875rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition:
			background-color 0.15s ease,
			color 0.15s ease,
			border-color 0.15s ease,
			transform 0.1s ease;
	}

	.action-btn:active:not(:disabled) {
		transform: scale(0.96);
	}

	.action-btn:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}

	.btn-follow {
		background-color: var(--color-text-primary);
		color: var(--color-bg);
		border: none;
	}

	.btn-follow:hover:not(:disabled) {
		background-color: var(--color-text-muted);
	}

	.btn-following {
		background: none;
		color: var(--color-text-primary);
		border: 1px solid var(--color-border);
	}

	.btn-following:hover:not(:disabled) {
		background-color: rgba(224, 82, 82, 0.08);
		border-color: var(--color-accent);
		color: var(--color-accent);
	}

	.btn-outline {
		background: none;
		color: var(--color-text-primary);
		border: 1px solid var(--color-border);
	}

	.btn-outline:hover:not(:disabled) {
		background-color: var(--color-surface-raised);
	}

	/* Profile info */
	.profile-info {
		display: flex;
		flex-direction: column;
		gap: 0.125rem;
		animation: fadeIn 0.15s ease-out;
	}

	.profile-name {
		font-size: 1.125rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.01em;
	}

	.profile-username {
		font-size: 0.9375rem;
		color: var(--color-text-muted);
	}

	.profile-bio {
		margin-top: 0.625rem;
		font-size: 0.9375rem;
		color: var(--color-text-primary);
		line-height: 1.55;
		white-space: pre-wrap;
		word-break: break-word;
	}

	/* Stats */
	.profile-stats {
		display: flex;
		gap: 1.5rem;
		margin-top: 1.25rem;
	}

	.stat-item {
		display: flex;
		align-items: baseline;
		gap: 0.3125rem;
		background: none;
		border: none;
		padding: 0;
		font-family: inherit;
		cursor: pointer;
		transition: opacity 0.15s ease;
	}

	.stat-item:hover .stat-label {
		text-decoration: underline;
	}

	.stat-item:focus-visible {
		outline: 2px solid var(--color-text-muted);
		outline-offset: 3px;
		border-radius: 2px;
	}

	.stat-value {
		font-size: 0.9375rem;
		font-weight: 700;
		color: var(--color-text-primary);
	}

	.stat-label {
		font-size: 0.875rem;
		color: var(--color-text-muted);
	}

	/* Modal */
	.modal-backdrop {
		position: fixed;
		inset: 0;
		z-index: 100;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.modal-panel {
		background: var(--color-bg);
		border: 1px solid var(--color-border);
		border-radius: 1rem;
		width: 90vw;
		max-width: 420px;
		max-height: 70vh;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.modal-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 1rem 1.25rem;
		border-bottom: 1px solid var(--color-border);
		flex-shrink: 0;
	}

	.modal-title {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.01em;
	}

	.modal-close {
		background: none;
		border: none;
		cursor: pointer;
		font-size: 1.5rem;
		line-height: 1;
		color: var(--color-text-muted);
		padding: 0.125rem 0.375rem;
		border-radius: 0.375rem;
		font-family: inherit;
		transition: color 0.15s ease, background-color 0.15s ease;
	}

	.modal-close:hover {
		color: var(--color-text-primary);
		background-color: var(--color-surface-raised);
	}

	.modal-close:focus-visible {
		outline: 2px solid var(--color-text-muted);
		outline-offset: 2px;
	}

	.modal-body {
		overflow-y: auto;
		flex: 1;
	}

	.modal-empty {
		padding: 2.5rem 1.25rem;
		text-align: center;
		font-size: 0.9375rem;
		color: var(--color-text-muted);
	}

	/* User list rows */
	.user-row {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		padding: 0.75rem 1.25rem;
		text-decoration: none;
		transition: background-color 0.15s ease;
	}

	.user-row:hover {
		background-color: var(--color-surface-raised);
	}

	.user-row:focus-visible {
		outline: 2px solid var(--color-text-muted);
		outline-offset: -2px;
	}

	.user-avatar-sm {
		width: 2.5rem;
		height: 2.5rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
		flex-shrink: 0;
	}

	.user-avatar-initial {
		font-size: 1rem;
		font-weight: 600;
		color: var(--color-text-muted);
		user-select: none;
	}

	.user-info {
		display: flex;
		flex-direction: column;
		gap: 0.0625rem;
		min-width: 0;
	}

	.user-name {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text-primary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.user-handle {
		font-size: 0.875rem;
		color: var(--color-text-muted);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	/* Modal skeleton rows */
	.user-skeleton {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		padding: 0.75rem 1.25rem;
	}

	.skeleton-avatar-sm {
		width: 2.5rem;
		height: 2.5rem;
		border-radius: 50%;
		background: var(--color-skeleton);
		animation: shimmer 1.6s ease-in-out infinite;
		flex-shrink: 0;
	}

	.skeleton-text-group {
		flex: 1;
	}

	/* Edit form */
	.edit-form {
		display: flex;
		flex-direction: column;
		gap: 0.875rem;
		animation: fadeIn 0.15s ease-out;
	}

	.edit-field {
		display: flex;
		flex-direction: column;
		gap: 0.375rem;
	}

	.edit-label {
		font-size: 0.8125rem;
		font-weight: 500;
		color: var(--color-text-muted);
	}

	.edit-input,
	.edit-textarea {
		width: 100%;
		background: var(--color-surface-raised);
		border: 1px solid var(--color-border);
		border-radius: 0.5rem;
		padding: 0.625rem 0.75rem;
		font-size: 0.9375rem;
		color: var(--color-text-primary);
		font-family: inherit;
		outline: none;
		resize: none;
		transition:
			border-color 0.15s ease,
			box-shadow 0.15s ease;
	}

	.edit-input::placeholder,
	.edit-textarea::placeholder {
		color: var(--color-text-muted);
		opacity: 0.5;
	}

	.edit-input:focus,
	.edit-textarea:focus {
		border-color: var(--color-text-muted);
		box-shadow: 0 0 0 3px rgba(240, 240, 240, 0.06);
	}

	.edit-input:disabled,
	.edit-textarea:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}

	.save-error {
		font-size: 0.875rem;
		color: var(--color-accent);
	}

	.edit-actions {
		display: flex;
		gap: 0.625rem;
	}

	/* Divider */
	.posts-divider {
		height: 1px;
		background: var(--color-border);
		margin: 0 -2rem;
	}

	/* State boxes */
	.state-box {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 0.5rem;
		padding: 3rem 1rem;
		text-align: center;
	}

	.state-title {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text-primary);
	}

	.state-body {
		font-size: 0.875rem;
		color: var(--color-text-muted);
	}

	.retry-btn {
		margin-top: 0.75rem;
		padding: 0.5rem 1.25rem;
		border-radius: 0.5rem;
		border: 1px solid var(--color-border);
		background: none;
		color: var(--color-text-primary);
		font-size: 0.875rem;
		font-weight: 500;
		cursor: pointer;
		transition:
			background-color 0.15s ease,
			transform 0.1s ease;
	}

	.retry-btn:hover {
		background-color: var(--color-surface-raised);
	}

	.retry-btn:active {
		transform: scale(0.97);
	}

	/* Posts */
	.post-list {
		animation: fadeIn 0.2s ease-out;
	}

	.posts-sentinel {
		height: 1px;
	}

	.loading-more {
		padding: 1.5rem;
		text-align: center;
		font-size: 0.8125rem;
		color: var(--color-text-muted);
	}

	/* Skeleton */
	.profile-skeleton {
		display: flex;
		align-items: flex-start;
		gap: 1rem;
		padding: 1.5rem 0;
	}

	.skeleton-avatar-lg {
		width: 5rem;
		height: 5rem;
		border-radius: 50%;
		background: var(--color-skeleton);
		animation: shimmer 1.6s ease-in-out infinite;
		flex-shrink: 0;
	}

	.skeleton-info {
		flex: 1;
		padding-top: 0.5rem;
	}

	.skeleton-line {
		border-radius: 4px;
		background: var(--color-skeleton);
		animation: shimmer 1.6s ease-in-out infinite;
		display: block;
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

	@media (max-width: 1024px) {
		.page-layout {
			grid-template-columns: 4.5rem 1fr;
		}

		.col-right {
			display: none;
		}
	}

	@media (max-width: 640px) {
		.page-layout {
			grid-template-columns: 1fr;
		}

		.col-left {
			display: none;
		}

		.col-center {
			padding: 0 1rem;
		}

		.posts-divider {
			margin: 0 -1rem;
		}
	}
</style>
