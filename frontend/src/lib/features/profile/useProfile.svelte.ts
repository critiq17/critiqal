import { userService } from '$lib/services/user.service';
import { mediaService } from '$lib/services/media.service';
import { authStore } from '$lib/stores/auth.store.svelte';
import { profileCache } from '$lib/stores/profile-cache.store.svelte';
import type { User, Post } from '$lib/types';

export class UseProfile {
  profile = $state<User | null>(null);
  posts = $state<Post[]>([]);
  followersList = $state<User[]>([]);
  followingList = $state<User[]>([]);
  postsCount = $state<number | null>(null);
  followersCount = $state<number | null>(null);
  followingCount = $state<number | null>(null);
  listsLoaded = $state(false);

  isLoading = $state(false);
  postsLoading = $state(false);
  profileError = $state<string | null>(null);
  listsLoading = $state(false);

  isEditing = $state(false);
  editName = $state('');
  editBio = $state('');
  isSaving = $state(false);
  saveError = $state<string | null>(null);

  isUploadingAvatar = $state(false);
  avatarError = $state<string | null>(null);

  constructor() {
    // Hydrate from cache so revisits to the profile tab don't flash skeletons.
    const username = authStore.user?.username;
    if (!username) return;
    const cached = profileCache.get(username);
    if (cached?.profile) this.profile = cached.profile;
    if (cached && cached.posts.length > 0) this.posts = cached.posts;
    if (cached?.listsLoaded) {
      this.followersList = cached.followersList;
      this.followingList = cached.followingList;
      this.listsLoaded = true;
    }
    if (cached?.stats) {
      this.postsCount = cached.stats.postsCount;
      this.followersCount = cached.stats.followersCount;
      this.followingCount = cached.stats.followingCount;
    }
  }

  private async loadStats(userId: string, username: string): Promise<void> {
    try {
      const stats = await userService.getStats(userId);
      this.postsCount = stats.postsCount;
      this.followersCount = stats.followersCount;
      this.followingCount = stats.followingCount;
      profileCache.set(username, { stats });
    } catch {
      // non-critical
    }
  }

  async load(): Promise<void> {
    const username = authStore.user?.username;
    if (!username) {
      this.isLoading = false;
      return;
    }

    const isFresh = profileCache.isFresh(username);
    const hasCachedProfile = this.profile !== null;

    // Cache hit + fresh → no network at all, UI already rendered.
    if (isFresh && hasCachedProfile && this.posts.length > 0) return;

    // Show loading only on truly cold start.
    if (!hasCachedProfile) this.isLoading = true;
    this.profileError = null;

    const postsPromise = userService.getUserPosts(username).catch(() => null);

    try {
      const user = await userService.getProfile(username);
      this.profile = user;
      this.isLoading = false;
      profileCache.set(username, { profile: user });

      // Fire stats in parallel — populates counters without pulling full lists.
      this.loadStats(user.id, username);

      if (!hasCachedProfile) this.postsLoading = true;
      const postsPage = await postsPromise;
      if (postsPage) {
        this.posts = postsPage.content;
        profileCache.set(username, {
          posts: postsPage.content,
          postsHasNext: postsPage.hasNext,
          postsPage: 0,
        });
      }
      this.postsLoading = false;
    } catch (err: unknown) {
      if (!hasCachedProfile) {
        this.profileError = err instanceof Error ? err.message : 'Failed to load profile';
      }
      this.isLoading = false;
    }
  }

  /**
   * Lazy: only load when the followers/following sheet is actually opened.
   * Heavy: backend returns full lists O(N). Don't trigger on profile load.
   */
  async loadFollowLists(): Promise<void> {
    if (!this.profile || this.listsLoaded || this.listsLoading) return;
    const username = authStore.user?.username;
    this.listsLoading = true;
    try {
      const [followers, following] = await Promise.all([
        userService.getFollowers(this.profile.id),
        userService.getFollowing(this.profile.id),
      ]);
      this.followersList = followers;
      this.followingList = following;
      this.listsLoaded = true;
      if (username) {
        profileCache.set(username, {
          followersList: followers,
          followingList: following,
          listsLoaded: true,
        });
      }
    } catch {
      // non-critical
    } finally {
      this.listsLoading = false;
    }
  }

  startEdit(): void {
    if (!this.profile) return;
    this.editName = this.profile.name ?? '';
    this.editBio = this.profile.bio ?? '';
    this.isEditing = true;
    this.saveError = null;
  }

  // Optimistic removal after a post is deleted from the shared Post component.
  handlePostDeleted(id: string): void {
    this.posts = this.posts.filter((p) => p.id !== id);
    if (this.postsCount !== null) this.postsCount = Math.max(0, this.postsCount - 1);

    const username = authStore.user?.username;
    if (!username) return;
    profileCache.set(username, { posts: this.posts });
    if (this.postsCount !== null) {
      profileCache.set(username, {
        stats: {
          postsCount: this.postsCount,
          followersCount: this.followersCount ?? 0,
          followingCount: this.followingCount ?? 0,
        },
      });
    }
  }

  cancelEdit(): void {
    this.isEditing = false;
    this.saveError = null;
  }

  async saveEdit(): Promise<void> {
    this.isSaving = true;
    this.saveError = null;
    try {
      const updated = await userService.updateProfile({
        name: this.editName.trim() || undefined,
        bio: this.editBio.trim() || undefined,
        avatarUrl: this.profile?.avatarUrl ?? undefined,
      });
      this.profile = updated;
      authStore.updateUser(updated);
      profileCache.set(updated.username, { profile: updated });
      this.isEditing = false;
    } catch (err: unknown) {
      this.saveError = err instanceof Error ? err.message : 'Failed to save changes';
    } finally {
      this.isSaving = false;
    }
  }

  async uploadAvatar(file: File): Promise<void> {
    if (!this.profile) return;
    this.isUploadingAvatar = true;
    this.avatarError = null;
    try {
      const result = await mediaService.uploadAvatar(file);
      this.profile = { ...this.profile, avatarUrl: result.avatarUrl };
      authStore.updateUser({ ...authStore.user!, avatarUrl: result.avatarUrl });
      profileCache.set(this.profile.username, { profile: this.profile });
    } catch (err: unknown) {
      this.avatarError = err instanceof Error ? err.message : 'Upload failed';
    } finally {
      this.isUploadingAvatar = false;
    }
  }
}
