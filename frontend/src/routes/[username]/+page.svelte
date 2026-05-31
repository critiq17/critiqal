<script lang="ts">
	import { page } from '$app/stores';
	import { onMount, untrack, tick } from 'svelte';
	import { afterNavigate } from '$app/navigation';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { stravaStore } from '$lib/stores/strava.store.svelte';
	import { UseProfilePage } from '$lib/features/profile/useProfilePage.svelte';
	import { infiniteScroll } from '$lib/actions/infiniteScroll';
	import { formatCount } from '$lib/utils/formatCount';
	import LeftSidebar from '$lib/components/LeftSidebar.svelte';
	import { Post as PostCard } from '$lib/components/post';
	import PostCardSkeleton from '$lib/components/PostCardSkeleton.svelte';
	import ProfileStravaWidget from '$lib/components/profile/ProfileStravaWidget.svelte';
	import ProfileAvatarUpload from '$lib/components/profile/ProfileAvatarUpload.svelte';
	import ProfileEditForm from '$lib/components/profile/ProfileEditForm.svelte';
	import ProfileFollowButton from '$lib/components/profile/ProfileFollowButton.svelte';
	import ProfileShareButton from '$lib/components/profile/ProfileShareButton.svelte';
	import ProfileInlineStats from '$lib/components/profile/ProfileInlineStats.svelte';
	import ProfileTabs from '$lib/components/profile/ProfileTabs.svelte';
	import ProfileEmptyPosts from '$lib/components/profile/ProfileEmptyPosts.svelte';
	// Lazy-loaded: only needed on first modal open.
	let FollowersModal = $state<typeof import('$lib/components/profile/FollowersModal.svelte').default | null>(null);
	import BadgePillRow from '$lib/components/badges/BadgePillRow.svelte';

	let username = $state($page.params.username as string);
	let profilePage = $state(new UseProfilePage($page.params.username as string));

	const isOwnProfile = $derived(authStore.user?.username === username);
	const SKELETON_COUNT = 3;

	// Header frost interpolates over the first 80px of scroll inside .col-center.
	let centerEl: HTMLElement | null = $state(null);
	let headerProgress = $state(0);
	let headerTicking = false;
	function onCenterScroll(): void {
		if (headerTicking) return;
		headerTicking = true;
		requestAnimationFrame(() => {
			const top = centerEl?.scrollTop ?? 0;
			headerProgress = Math.min(1, Math.max(0, top / 80));
			headerTicking = false;
		});
	}

	type ModalTab = 'followers' | 'following';
	let modalOpen = $state(false);
	let modalTab = $state<ModalTab>('followers');

	function openModal(tab: ModalTab): void {
		modalTab = tab;
		modalOpen = true;
		profilePage.loadFollowLists();
	}

	const tabs = $derived([
		{ id: 'posts', label: 'Posts', count: profilePage.postsCount ?? profilePage.posts.length },
	] as const);

	const displayName = $derived(
		profilePage.profile?.name ?? profilePage.profile?.username ?? username
	);

	onMount(() => {
		if (isOwnProfile) stravaStore.load();
	});

	// The [username] route component is reused across profile→profile
	// navigations, so onMount fires only once. afterNavigate fires on every
	// navigation; when the username param actually changed, swap in a fresh
	// feature hook and load it so the new profile never gets stuck on the
	// previous one's (or an infinite) skeleton.
	afterNavigate(async () => {
		const next = $page.params.username as string;
		if (next === username) return;
		username = next;
		// Constructor calls loadProfile() immediately, so no explicit call needed here.
		profilePage = new UseProfilePage(next);
		await tick();
		if (authStore.user?.username === next) stravaStore.load();
	});

	$effect(() => {
		const profileId = profilePage.profile?.id;
		if (profileId && !isOwnProfile) {
			// untrack: stravaStore.loadPublic reads internal $state synchronously,
			// which would otherwise create a dep and loop on every strava update.
			untrack(() => stravaStore.loadPublic(profileId));
		}
	});

	$effect(() => {
		if (modalOpen && !FollowersModal) {
			import('$lib/components/profile/FollowersModal.svelte').then((m) => { FollowersModal = m.default; });
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

	<main
		class="col-center"
		aria-label="Profile"
		bind:this={centerEl}
		onscroll={onCenterScroll}
	>
		<header class="page-header" style="--header-progress: {headerProgress}">
			<h1 class="page-title">{displayName}</h1>
			{#if profilePage.profile}
				<span class="post-count-label">
					{formatCount(profilePage.postsCount ?? profilePage.posts.length)} posts
				</span>
			{/if}
		</header>

		{#if profilePage.profileState === 'loading' && !profilePage.profile}
			<div class="profile-skeleton" aria-hidden="true">
				<div class="skeleton-avatar"></div>
				<div class="skeleton-info">
					<div class="skeleton-line" style="width:8rem;height:1rem;"></div>
					<div class="skeleton-line" style="width:5rem;height:.75rem;margin-top:.375rem;"></div>
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
			<section class="identity-card" aria-label="Profile info">
				<div class="identity-avatar">
					{#if isOwnProfile}
						<ProfileAvatarUpload
							profile={profilePage.profile}
							isUploading={profilePage.isUploadingAvatar}
							error={profilePage.avatarError}
							onUpload={(file) => profilePage.uploadAvatar(file)}
						/>
					{:else}
						<div class="avatar-display" aria-hidden="true">
							{#if profilePage.profile.avatarUrl}
								<img
									src={profilePage.profile.avatarUrl}
									alt={profilePage.profile.username}
									class="avatar-img"
								/>
							{:else}
								<span class="avatar-initial">
									{(profilePage.profile.name ?? profilePage.profile.username)
										.charAt(0)
										.toUpperCase()}
								</span>
							{/if}
						</div>
					{/if}
				</div>

				<div class="identity-body">
					{#if profilePage.isEditing}
						<ProfileEditForm
							editName={profilePage.editName}
							editBio={profilePage.editBio}
							isSaving={profilePage.isSaving}
							saveError={profilePage.saveError}
							onSave={() => profilePage.saveEdit()}
							onCancel={() => profilePage.cancelEdit()}
							onNameChange={(v) => {
								profilePage.editName = v;
							}}
							onBioChange={(v) => {
								profilePage.editBio = v;
							}}
						/>
					{:else}
						<div class="identity-top">
							<div class="identity-names">
								<p class="display-name">
									{profilePage.profile.name ?? profilePage.profile.username}
								</p>
								<p class="username">@{profilePage.profile.username}</p>
							</div>
							<div class="identity-actions">
								<ProfileShareButton
									username={profilePage.profile.username}
									displayName={profilePage.profile.name}
								/>
								{#if isOwnProfile}
									<button class="btn btn-outline" onclick={() => profilePage.startEdit()}>
										Edit profile
									</button>
								{:else if authStore.isAuthenticated}
									<ProfileFollowButton
										isFollowing={profilePage.isFollowing}
										isLoading={profilePage.isFollowLoading}
										onFollow={() => profilePage.toggleFollow()}
									/>
								{/if}
							</div>
						</div>

						{#if profilePage.profile.bio}
							<p class="bio">{profilePage.profile.bio}</p>
						{/if}

						{#if profilePage.profile.badges && profilePage.profile.badges.length > 0}
							<BadgePillRow badges={profilePage.profile.badges} />
						{/if}

						<ProfileInlineStats
							postsCount={profilePage.postsCount}
							followersCount={profilePage.followersCount}
							followingCount={profilePage.followingCount}
							onOpenFollowers={() => openModal('followers')}
							onOpenFollowing={() => openModal('following')}
						/>
					{/if}
				</div>
			</section>

			{#if stravaStore.connection}
				<div class="strava-slot">
					<ProfileStravaWidget {isOwnProfile} />
				</div>
			{/if}

			<div class="tabs-wrap" style="--header-progress: {headerProgress}">
				<ProfileTabs tabs={tabs} activeId="posts" />
			</div>
		{/if}

		<div
			id="profile-tabpanel-posts"
			role="tabpanel"
			aria-labelledby="profile-tab-posts"
			tabindex="0"
			class="posts-panel"
		>
			{#if profilePage.postsLoading && profilePage.posts.length === 0}
				<div aria-busy="true" aria-label="Loading posts">
					{#each { length: SKELETON_COUNT } as _empty, i (i)}
						<PostCardSkeleton />
					{/each}
				</div>
			{:else if profilePage.postsError && profilePage.posts.length === 0}
				<div class="state-box">
					<p class="state-body">{profilePage.postsError}</p>
					<button class="retry-btn" onclick={() => profilePage.loadPosts()}>Try again</button>
				</div>
			{:else if profilePage.posts.length === 0 && profilePage.profile}
				<ProfileEmptyPosts {isOwnProfile} />
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
							disabled: profilePage.postsLoadingMore,
						}}
					></div>
				{/if}
				{#if profilePage.postsLoadingMore}
					<div class="loading-more">Loading more…</div>
				{/if}
			{/if}
		</div>
	</main>

	<aside class="col-right" aria-hidden="true"></aside>
</div>

{#if FollowersModal}
	<FollowersModal
		open={modalOpen}
		tab={modalTab}
		followersList={profilePage.followersList}
		followingList={profilePage.followingList}
		loading={profilePage.listsLoading}
		onClose={() => {
			modalOpen = false;
		}}
	/>
{/if}

<style>
	.page-layout {
		height: 100vh;
		overflow: hidden;
	}

	.col-left {
		position: fixed;
		right: calc(50% + 21rem);
		top: 0;
		bottom: 0;
		width: 16rem;
		overflow-y: auto;
		padding: 0 1.5rem 0 1rem;
		z-index: 20;
	}

	.col-center {
		height: 100vh;
		max-width: 42rem;
		margin: 0 auto;
		padding: 0 2rem;
		overflow-y: auto;
		scrollbar-width: none;
		-ms-overflow-style: none;
	}

	.col-center::-webkit-scrollbar {
		display: none;
	}

	.col-right {
		display: none;
	}

	.page-header {
		padding: 1.25rem 0 1rem;
		position: sticky;
		top: 0;
		background-color: color-mix(in srgb, var(--color-bg) calc(var(--header-progress, 0) * 82%), transparent);
		backdrop-filter: blur(calc(var(--header-progress, 0) * 18px)) saturate(180%);
		-webkit-backdrop-filter: blur(calc(var(--header-progress, 0) * 18px)) saturate(180%);
		z-index: 10;
		display: flex;
		align-items: baseline;
		gap: 0.625rem;
	}

	.page-header::after {
		content: '';
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		height: 1px;
		background: linear-gradient(to right, transparent, var(--glass-border), transparent);
		opacity: var(--header-progress, 0);
		pointer-events: none;
	}

	.page-title {
		margin: 0;
		font-size: 1.0625rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.015em;
	}

	.post-count-label {
		font-size: 0.8125rem;
		color: var(--color-text-muted);
		font-variant-numeric: tabular-nums;
	}

	.identity-card {
		display: grid;
		grid-template-columns: auto 1fr;
		gap: 1.25rem;
		padding: 1rem 0 1.25rem;
		animation: fadeIn 0.2s ease-out;
	}

	.identity-avatar {
		flex-shrink: 0;
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

	.identity-body {
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 0.75rem;
	}

	.identity-top {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 0.75rem;
		flex-wrap: wrap;
	}

	.identity-names {
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 0.125rem;
	}

	.display-name {
		margin: 0;
		font-size: 1.125rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.01em;
		word-break: break-word;
	}

	.username {
		margin: 0;
		font-size: 0.9375rem;
		color: var(--color-text-muted);
	}

	.identity-actions {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		flex-shrink: 0;
	}

	.bio {
		margin: 0;
		font-size: 0.9375rem;
		color: var(--color-text-primary);
		line-height: 1.55;
		white-space: pre-wrap;
		word-break: break-word;
	}

	.btn {
		padding: 0.5rem 1.125rem;
		border-radius: 9999px;
		font-size: 0.875rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		border: none;
		transform-origin: center;
		will-change: transform;
		transition:
			background-color var(--duration-micro) var(--ease-out-quart),
			box-shadow var(--duration-micro) var(--ease-out-quart),
			transform var(--duration-press) var(--ease-out-quart);
	}

	.btn:active:not(:disabled) {
		transform: scale(0.96);
	}

	/* Inset shadow border avoids layout shift between rest/hover. */
	.btn-outline {
		background: none;
		color: var(--color-text-primary);
		box-shadow: inset 0 0 0 1px var(--divider-soft);
	}

	.btn-outline:hover {
		background-color: var(--surface-tint-soft);
		box-shadow: inset 0 0 0 1px var(--divider-strong);
	}

	.strava-slot {
		padding-bottom: 1rem;
	}

	.tabs-wrap {
		margin: 0 -2rem;
		padding: 0 2rem;
		position: sticky;
		top: 3.625rem;
		background-color: color-mix(in srgb, var(--color-bg) calc(var(--header-progress, 0) * 82%), transparent);
		backdrop-filter: blur(calc(var(--header-progress, 0) * 18px)) saturate(180%);
		-webkit-backdrop-filter: blur(calc(var(--header-progress, 0) * 18px)) saturate(180%);
		z-index: 9;
	}

	.posts-panel {
		padding-top: 0.25rem;
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

	.state-body {
		font-size: 0.875rem;
		color: var(--color-text-muted);
	}

	.retry-btn {
		margin-top: 0.75rem;
		padding: 0.5rem 1.25rem;
		border-radius: 0.5rem;
		border: none;
		background: var(--surface-tint-subtle);
		box-shadow: inset 0 0 0 1px var(--divider-soft);
		color: var(--color-text-primary);
		font-size: 0.875rem;
		font-weight: 500;
		cursor: pointer;
		transform-origin: center;
		will-change: transform;
		transition:
			background-color var(--duration-micro) var(--ease-out-quart),
			box-shadow var(--duration-micro) var(--ease-out-quart),
			transform var(--duration-press) var(--ease-out-quart);
	}

	.retry-btn:hover {
		background-color: var(--surface-tint-soft);
		box-shadow: inset 0 0 0 1px var(--divider-strong);
	}

	.retry-btn:active {
		transform: scale(0.97);
	}

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

	.profile-skeleton {
		display: flex;
		align-items: flex-start;
		gap: 1.25rem;
		padding: 1rem 0 1.25rem;
	}

	.skeleton-avatar {
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

	@media (prefers-reduced-motion: reduce) {
		.identity-card,
		.post-list {
			animation: none;
		}
		.skeleton-avatar,
		.skeleton-line {
			animation: none;
		}
		.btn,
		.retry-btn {
			transition:
				background-color var(--duration-micro) var(--ease-out-quart),
				box-shadow var(--duration-micro) var(--ease-out-quart);
		}
		.btn:active,
		.retry-btn:active { transform: none; }
	}

	@media (max-width: 900px) {
		.col-left {
			width: 4.5rem;
			padding: 0 0.5rem;
		}
	}

	@media (max-width: 640px) {
		.col-left {
			display: none;
		}
		.col-center {
			padding: 0 1rem;
		}
		.tabs-wrap {
			margin: 0 -1rem;
			padding: 0 1rem;
		}
		.identity-card {
			gap: 1rem;
		}
		.identity-top {
			flex-direction: column;
			align-items: flex-start;
		}
	}
</style>
