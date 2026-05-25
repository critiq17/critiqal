import type { User, UserStats, Post } from '$lib/types';

interface ProfileEntry {
  profile: User | null;
  posts: Post[];
  postsHasNext: boolean;
  postsPage: number;
  followersList: User[];
  followingList: User[];
  listsLoaded: boolean;
  stats: UserStats | null;
  loadedAt: number;
}

const STALE_TIME_MS = 60_000;
const REVALIDATE_AFTER_MS = 30_000;

const cache = new Map<string, ProfileEntry>();
type Listener = (username: string) => void;
const visibilityListeners = new Set<Listener>();

// One global visibilitychange handler — instead of every UseProfile hook
// attaching its own. Hooks subscribe via onStaleOnReturn(); we fire for any
// cached username whose entry has gone past the revalidate threshold.
if (typeof document !== 'undefined') {
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState !== 'visible') return;
    const now = Date.now();
    for (const [username, entry] of cache.entries()) {
      if (now - entry.loadedAt < REVALIDATE_AFTER_MS) continue;
      for (const l of visibilityListeners) l(username);
    }
  });
}

export const profileCache = {
  get(username: string): ProfileEntry | null {
    const entry = cache.get(username);
    if (!entry) return null;
    return entry;
  },

  isFresh(username: string): boolean {
    const entry = cache.get(username);
    if (!entry) return false;
    return Date.now() - entry.loadedAt < STALE_TIME_MS;
  },

  set(username: string, entry: Partial<ProfileEntry>): void {
    const existing = cache.get(username);
    const merged: ProfileEntry = {
      profile: entry.profile ?? existing?.profile ?? null,
      posts: entry.posts ?? existing?.posts ?? [],
      postsHasNext: entry.postsHasNext ?? existing?.postsHasNext ?? false,
      postsPage: entry.postsPage ?? existing?.postsPage ?? 0,
      followersList: entry.followersList ?? existing?.followersList ?? [],
      followingList: entry.followingList ?? existing?.followingList ?? [],
      listsLoaded: entry.listsLoaded ?? existing?.listsLoaded ?? false,
      stats: entry.stats ?? existing?.stats ?? null,
      loadedAt: entry.loadedAt ?? Date.now(),
    };
    cache.set(username, merged);
  },

  invalidate(username: string): void {
    cache.delete(username);
  },

  clear(): void {
    cache.clear();
  },

  /**
   * Fires `listener(username)` for each cached profile that has gone stale,
   * whenever the tab/app returns to the foreground. Returns an unsubscribe.
   */
  onStaleOnReturn(listener: Listener): () => void {
    visibilityListeners.add(listener);
    return () => visibilityListeners.delete(listener);
  },
};
