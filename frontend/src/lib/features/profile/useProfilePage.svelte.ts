/**
 * Feature hook for the public [username]/+page.svelte profile page.
 * Stale-while-revalidate caching, lazy follower-list loading.
 */
import { userService, mediaService } from '$lib/services';
import { authStore } from '$lib/stores/auth.store.svelte';
import { authGate } from '$lib/stores/auth-gate.store.svelte';
import { profileCache } from '$lib/stores/profile-cache.store.svelte';
import type { User, Post } from '$lib/types';

export type ProfilePageState = 'loading' | 'loaded' | 'error';

export class UseProfilePage {
  // Profile
  profile = $state<User | null>(null);
  profileState = $state<ProfilePageState>('loading');
  profileError = $state('');

  // Posts (paginated)
  posts = $state<Post[]>([]);
  postsPage = $state(0);
  postsHasNext = $state(false);
  postsLoading = $state(false);
  postsLoadingMore = $state(false);
  postsError = $state<string | null>(null);

  // Follow lists — lazy: loaded only when modal opens.
  followersList = $state<User[]>([]);
  followingList = $state<User[]>([]);
  listsLoaded = $state(false);
  listsLoading = $state(false);

  // Counts come from the dedicated /stats endpoint, then mutate optimistically
  // on follow/unfollow and post deletion.
  postsCount = $state<number | null>(null);
  followersCount = $state<number | null>(null);
  followingCount = $state<number | null>(null);

  isFollowing = $state(false);
  isFollowLoading = $state(false);

  // Edit (own profile only)
  isEditing = $state(false);
  editName = $state('');
  editBio = $state('');
  isSaving = $state(false);
  saveError = $state('');

  // Avatar upload
  isUploadingAvatar = $state(false);
  avatarError = $state('');

  constructor(private username: string) {
    this.hydrateFromCache();

    // Silent revalidate when tab returns to foreground after a quiet stretch.
    profileCache.onStaleOnReturn((u) => {
      if (u === this.username) this.loadProfile();
    });
  }

  private hydrateFromCache(): void {
    const cached = profileCache.get(this.username);
    if (!cached) return;

    if (cached.profile) {
      this.profile = cached.profile;
      this.profileState = 'loaded';
    }
    if (cached.posts.length > 0) {
      this.posts = cached.posts;
      this.postsPage = cached.postsPage;
      this.postsHasNext = cached.postsHasNext;
    }
    if (cached.listsLoaded) {
      this.followersList = cached.followersList;
      this.followingList = cached.followingList;
      this.listsLoaded = true;
      if (authStore.user) {
        this.isFollowing = cached.followersList.some((f) => f.id === authStore.user!.id);
      }
    }
    if (cached.stats) {
      this.postsCount = cached.stats.postsCount;
      this.followersCount = cached.stats.followersCount;
      this.followingCount = cached.stats.followingCount;
    }
  }

  async loadProfile(): Promise<void> {
    const isFresh = profileCache.isFresh(this.username);
    const hasCachedProfile = this.profile !== null;

    // If cache is fresh, do nothing — UI is already rendered from hydrateFromCache.
    if (isFresh && hasCachedProfile && this.posts.length > 0) return;

    // Only show loading state on TRULY cold load (no cached profile at all).
    if (!hasCachedProfile) {
      this.profileState = 'loading';
    }
    this.profileError = '';

    // Fire posts in parallel — independent of profile fetch.
    const postsPromise = this.loadPostsInternal({ silent: hasCachedProfile });

    try {
      const user = await userService.getProfile(this.username);
      this.profile = user;
      this.profileState = 'loaded';
      profileCache.set(this.username, { profile: user });

      // Fire stats in parallel — populates posts/followers/following counters
      // without waiting on the heavyweight follower-list endpoints.
      this.loadStats(user.id);

      // Determine isFollowing only if not already known via cached list.
      if (!this.listsLoaded && authStore.isAuthenticated && authStore.user?.id !== user.id) {
        this.refreshIsFollowing(user.id);
      }
    } catch (err: unknown) {
      if (!hasCachedProfile) {
        this.profileError = err instanceof Error ? err.message : 'User not found.';
        this.profileState = 'error';
      }
    }

    await postsPromise;
  }

  private async refreshIsFollowing(userId: string): Promise<void> {
    // Lightweight check: fetch only the followers list for this user, find self.
    // Still O(N) — backend should expose /follow/check, but until it does this
    // is run async without blocking UI.
    try {
      const followers = await userService.getFollowers(userId);
      if (authStore.user) {
        this.isFollowing = followers.some((f) => f.id === authStore.user!.id);
      }
      // Stash in cache so future opens don't refetch.
      this.followersList = followers;
    } catch {
      // non-critical
    }
  }

  private async loadStats(userId: string): Promise<void> {
    try {
      const stats = await userService.getStats(userId);
      this.postsCount = stats.postsCount;
      this.followersCount = stats.followersCount;
      this.followingCount = stats.followingCount;
      profileCache.set(this.username, { stats });
    } catch {
      // non-critical — UI keeps existing counters or "—" placeholders.
    }
  }

  private async loadPostsInternal(opts: { silent: boolean }): Promise<void> {
    if (!opts.silent) {
      this.postsLoading = true;
    }
    this.postsError = null;
    try {
      const res = await userService.getUserPosts(this.username, 0);
      this.posts = res.content;
      this.postsHasNext = res.hasNext;
      this.postsPage = 0;
      profileCache.set(this.username, {
        posts: res.content,
        postsHasNext: res.hasNext,
        postsPage: 0,
      });
    } catch {
      if (!opts.silent) this.postsError = 'Could not load posts.';
    } finally {
      this.postsLoading = false;
    }
  }

  async loadPosts(): Promise<void> {
    await this.loadPostsInternal({ silent: false });
  }

  async loadMorePosts(): Promise<void> {
    if (!this.postsHasNext || this.postsLoadingMore) return;
    this.postsLoadingMore = true;
    try {
      const nextPage = this.postsPage + 1;
      const res = await userService.getUserPosts(this.username, nextPage);
      this.posts = [...this.posts, ...res.content];
      this.postsPage = nextPage;
      this.postsHasNext = res.hasNext;
      profileCache.set(this.username, {
        posts: this.posts,
        postsHasNext: res.hasNext,
        postsPage: nextPage,
      });
    } catch {
      // non-fatal
    } finally {
      this.postsLoadingMore = false;
    }
  }

  // Lazy: triggered when followers/following modal opens, not on profile load.
  async loadFollowLists(): Promise<void> {
    if (!this.profile || this.listsLoaded || this.listsLoading) return;
    this.listsLoading = true;
    try {
      const [followers, following] = await Promise.all([
        userService.getFollowers(this.profile.id),
        userService.getFollowing(this.profile.id),
      ]);
      this.followersList = followers;
      this.followingList = following;
      this.listsLoaded = true;
      if (authStore.user) {
        this.isFollowing = followers.some((f) => f.id === authStore.user!.id);
      }
      profileCache.set(this.username, {
        followersList: followers,
        followingList: following,
        listsLoaded: true,
      });
    } catch {
      // non-critical
    } finally {
      this.listsLoading = false;
    }
  }

  handlePostDeleted(postId: string): void {
    this.posts = this.posts.filter((p) => p.id !== postId);
    if (this.postsCount !== null && this.postsCount > 0) {
      this.postsCount -= 1;
    }
    profileCache.set(this.username, { posts: this.posts });
  }

  async toggleFollow(): Promise<void> {
    if (!authStore.isAuthenticated) {
      authGate.open('follow');
      return;
    }
    if (!this.profile || this.isFollowLoading) return;

    const currentUser = authStore.user!;
    const wasFollowing = this.isFollowing;
    const prevList = this.followersList;
    const prevCount = this.followersCount ?? 0;

    this.isFollowing = !this.isFollowing;
    this.followersCount = wasFollowing ? Math.max(0, prevCount - 1) : prevCount + 1;
    if (this.listsLoaded) {
      this.followersList = this.isFollowing
        ? [...this.followersList, currentUser]
        : this.followersList.filter((f) => f.id !== currentUser.id);
    }

    this.isFollowLoading = true;
    try {
      if (this.isFollowing) {
        await userService.follow(this.profile.id);
      } else {
        await userService.unfollow(this.profile.id);
      }
    } catch {
      this.isFollowing = wasFollowing;
      this.followersList = prevList;
      this.followersCount = prevCount;
    } finally {
      this.isFollowLoading = false;
    }
  }

  startEdit(): void {
    if (!this.profile) return;
    this.editName = this.profile.name ?? '';
    this.editBio = this.profile.bio ?? '';
    this.isEditing = true;
    this.saveError = '';
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.saveError = '';
  }

  async saveEdit(): Promise<void> {
    this.isSaving = true;
    this.saveError = '';
    try {
      const updated = await userService.updateProfile({
        name: this.editName.trim() || undefined,
        bio: this.editBio.trim() || undefined,
        avatarUrl: this.profile?.avatarUrl ?? undefined,
      });
      this.profile = updated;
      authStore.updateUser(updated);
      profileCache.set(this.username, { profile: updated });
      this.isEditing = false;
    } catch (err: unknown) {
      this.saveError = err instanceof Error ? err.message : 'Failed to save changes.';
    } finally {
      this.isSaving = false;
    }
  }

  async uploadAvatar(file: File): Promise<void> {
    if (!this.profile) return;
    this.isUploadingAvatar = true;
    this.avatarError = '';
    try {
      const result = await mediaService.uploadAvatar(file);
      this.profile = { ...this.profile, avatarUrl: result.avatarUrl };
      authStore.updateUser({ ...authStore.user!, avatarUrl: result.avatarUrl });
      profileCache.set(this.username, { profile: this.profile });
    } catch (err: unknown) {
      this.avatarError = err instanceof Error ? err.message : 'Upload failed.';
    } finally {
      this.isUploadingAvatar = false;
    }
  }
}
