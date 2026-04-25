<script lang="ts">
	import { page } from '$app/stores';
	import { onMount, untrack } from 'svelte';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { stravaStore } from '$lib/stores/strava.store.svelte';
	import { UseProfilePage } from '$lib/features/profile/useProfilePage.svelte';
	import { infiniteScroll } from '$lib/actions/infiniteScroll';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';
	import { Post as PostCard } from '$lib/components/post';
	import PostCardSkeleton from '$lib/components/PostCardSkeleton.svelte';
	import ProfileStravaWidget from '$lib/components/profile/ProfileStravaWidget.svelte';
	import ProfileAvatarUpload from '$lib/components/profile/ProfileAvatarUpload.svelte';
	import ProfileEditForm from '$lib/components/profile/ProfileEditForm.svelte';
	import ProfileStats from '$lib/components/profile/ProfileStats.svelte';
	import ProfileFollowButton from '$lib/components/profile/ProfileFollowButton.svelte';
	import FollowersModal from '$lib/components/profile/FollowersModal.svelte';

	const username = $page.params.username as string;
	const profilePage = new UseProfilePage(username);

	const isOwnProfile = $derived(authStore.user?.username === username);
	const SKELETON_COUNT = 3;

	type ModalTab = 'followers' | 'following';
	let modalOpen = $state(false);
	let modalTab = $state<ModalTab>('followers');

	function openModal(tab: ModalTab): void {
		modalTab = tab;
		modalOpen = true;
		// Lazy-load follower/following lists only when user actually opens the modal.
		profilePage.loadFollowLists();
	}

	function formatCount(n: number): string {
		if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
		if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
		return String(n);
	}

	onMount(() => {
		profilePage.loadProfile();
		if (isOwnProfile) stravaStore.load();
	});

	$effect(() => {
		const profileId = profilePage.profile?.id;
		if (profileId && !isOwnProfile) {
			// untrack: stravaStore.loadPublic reads internal $state synchronously,
			// which would create a dep and cause this effect to loop on every strava update.
			untrack(() => stravaStore.loadPublic(profileId));
		}
	});
</script>

<svelte:head>
	<title>
		{profilePage.profile
			? `${profilePage.profile.name ?? profilePage.profile.username} (@${profilePage.profile.username})`
			: `@${username}`} — Critiqal
	</title>
	<meta
		name="description"
		content={profilePage.profile?.bio ?? `${username}'s profile on Critiqal`}
	/>
</svelte:head>

<div class="page-layout">
	<div class="col-left">
		<LeftSidebar />
	</div>

	<main class="col-center" aria-label="Profile">
		<header class="page-header">
			<h1 class="page-title">
				{#if profilePage.profileState === 'loaded' && profilePage.profile}
					{profilePage.profile.name ?? `@${profilePage.profile.username}`}
				{:else}
					@{username}
				{/if}
			</h1>
			{#if !profilePage.postsLoading && profilePage.posts.length > 0}
				<span class="post-count-label">{formatCount(profilePage.posts.length)} posts</span>
			{/if}
		</header>

		{#if profilePage.profileState === 'loading' && !profilePage.profile}
			<div class="profile-skeleton" aria-hidden="true">
				<div class="skeleton-avatar-lg"></div>
				<div class="skeleton-info">
					<div class="skeleton-line" style="width:8rem;height:1rem;"></div>
					<div class="skeleton-line" style="width:5rem;height:.75rem;margin-top:.25rem;"></div>
					<div class="skeleton-line" style="width:100%;height:.75rem;margin-top:.875rem;"></div>
					<div class="skeleton-line" style="width:72%;height:.75rem;margin-top:.375rem;"></div>
				</div>
			</div>
		{:else if profilePage.profileState === 'error' && !profilePage.profile}
			<div class="state-box" role="alert">
				<p class="state-title">User not found</p>
				<p class="state-body">{profilePage.profileError}</p>
				<button class="retry-btn" onclick={() => profilePage.loadProfile()}>Try again</button>
			</div>
		{:else if profilePage.profile}
			<section class="profile-section" aria-label="Profile info">
				<div class="profile-top">
					{#if isOwnProfile}
						<ProfileAvatarUpload
							profile={profilePage.profile}
							isUploading={profilePage.isUploadingAvatar}
							error={profilePage.avatarError}
							onUpload={(file) => profilePage.uploadAvatar(file)}
						/>
					{:else}
						<div class="avatar-display">
							{#if profilePage.profile.avatarUrl}
								<img
									src={profilePage.profile.avatarUrl}
									alt={profilePage.profile.username}
									class="avatar-img"
								/>
							{:else}
								<span class="avatar-initial">
									{(profilePage.profile.name ?? profilePage.profile.username).charAt(0).toUpperCase()}
								</span>
							{/if}
						</div>
					{/if}

					<div class="profile-action">
						{#if isOwnProfile && !profilePage.isEditing}
							<button class="btn btn-outline" onclick={() => profilePage.startEdit()}>
								Edit profile
							</button>
						{:else if !isOwnProfile && authStore.isAuthenticated}
							<ProfileFollowButton
								isFollowing={profilePage.isFollowing}
								isLoading={profilePage.isFollowLoading}
								onFollow={() => profilePage.toggleFollow()}
							/>
						{/if}
					</div>
				</div>

				{#if profilePage.isEditing}
					<ProfileEditForm
						editName={profilePage.editName}
						editBio={profilePage.editBio}
						isSaving={profilePage.isSaving}
						saveError={profilePage.saveError}
						onSave={() => profilePage.saveEdit()}
						onCancel={() => profilePage.cancelEdit()}
						onNameChange={(v) => { profilePage.editName = v; }}
						onBioChange={(v) => { profilePage.editBio = v; }}
					/>
				{:else}
					<div class="profile-info">
						<p class="profile-name">
							{profilePage.profile.name ?? profilePage.profile.username}
						</p>
						<p class="profile-username">@{profilePage.profile.username}</p>
						{#if profilePage.profile.bio}
							<p class="profile-bio">{profilePage.profile.bio}</p>
						{/if}
					</div>
				{/if}

				<ProfileStats
					followersCount={profilePage.followersCount}
					followingCount={profilePage.followingCount}
					onOpenFollowers={() => openModal('followers')}
					onOpenFollowing={() => openModal('following')}
				/>
			</section>
		{/if}

		<div class="posts-divider" aria-hidden="true"></div>

		{#if profilePage.postsLoading && profilePage.posts.length === 0}
			<div aria-busy="true" aria-label="Loading posts">
				{#each { length: SKELETON_COUNT } as _, i (i)}
					<PostCardSkeleton />
				{/each}
			</div>
		{:else if profilePage.postsError && profilePage.posts.length === 0}
			<div class="state-box">
				<p class="state-body">{profilePage.postsError}</p>
				<button class="retry-btn" onclick={() => profilePage.loadPosts()}>Try again</button>
			</div>
		{:else if profilePage.posts.length === 0}
			<div class="state-box">
				<p class="state-title">No posts yet</p>
				<p class="state-body">
					{#if isOwnProfile}Share something with the world.{:else}Nothing here yet.{/if}
				</p>
			</div>
		{:else}
			<div class="post-list">
				{#each profilePage.posts as post (post.id)}
					<PostCard {post} onDeleted={(id) => profilePage.handlePostDeleted(id)} />
				{/each}
			</div>
			{#if profilePage.postsHasNext}
				<div
					class="posts-sentinel"
					use:infiniteScroll={{
						onTrigger: () => profilePage.loadMorePosts(),
						disabled: profilePage.postsLoadingMore
					}}
				></div>
			{/if}
			{#if profilePage.postsLoadingMore}
				<div class="loading-more">Loading more…</div>
			{/if}
		{/if}
	</main>

	<aside class="col-right" aria-label="Integrations">
		{#if profilePage.profileState === 'loaded' && profilePage.profile}
			<ProfileStravaWidget {isOwnProfile} />
		{/if}
	</aside>
</div>

<FollowersModal
	open={modalOpen}
	tab={modalTab}
	followersList={profilePage.followersList}
	followingList={profilePage.followingList}
	loading={profilePage.listsLoading}
	onClose={() => { modalOpen = false; }}
/>

<style>
	.page-layout {
		display: grid;
		grid-template-columns: 16rem 42rem 14rem;
		justify-content: center;
		height: 100vh;
		overflow: hidden;
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

	.col-center::-webkit-scrollbar { display: none; }

	.col-right {
		overflow-y: auto;
		padding: 1.5rem 1rem 1.5rem 1.5rem;
	}

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

	.avatar-display {
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

	.profile-action { padding-top: 0.25rem; }

	.btn {
		padding: 0.5rem 1.125rem;
		border-radius: 9999px;
		font-size: 0.875rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition: background-color 0.15s ease, transform 0.1s ease;
	}

	.btn:active:not(:disabled) { transform: scale(0.96); }

	.btn-outline {
		background: none;
		color: var(--color-text-primary);
		border: 1px solid var(--color-border);
	}

	.btn-outline:hover { background-color: var(--color-surface-raised); }

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

	.posts-divider {
		height: 1px;
		background: var(--color-border);
		margin: 0 -2rem;
	}

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

	.state-body { font-size: 0.875rem; color: var(--color-text-muted); }

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
		transition: background-color 0.15s ease, transform 0.1s ease;
	}

	.retry-btn:hover { background-color: var(--color-surface-raised); }
	.retry-btn:active { transform: scale(0.97); }

	.post-list { animation: fadeIn 0.2s ease-out; }
	.posts-sentinel { height: 1px; }

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

	.skeleton-info { flex: 1; padding-top: 0.5rem; }

	.skeleton-line {
		border-radius: 4px;
		background: var(--color-skeleton);
		animation: shimmer 1.6s ease-in-out infinite;
		display: block;
	}

	@keyframes fadeIn {
		from { opacity: 0; transform: translateY(0.25rem); }
		to { opacity: 1; transform: translateY(0); }
	}

	@keyframes shimmer {
		0%, 100% { opacity: 0.5; }
		50% { opacity: 1; }
	}

	@media (max-width: 1024px) {
		.page-layout { grid-template-columns: 4.5rem 1fr; }
		.col-right { display: none; }
	}

	@media (max-width: 640px) {
		.page-layout { grid-template-columns: 1fr; }
		.col-left { display: none; }
		.col-center { padding: 0 1rem; }
		.posts-divider { margin: 0 -1rem; }
	}
</style>
