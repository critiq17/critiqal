import { postService } from '$lib/services';
import type { Post } from '$lib/types';
import { readCache, writeCache } from '$lib/utils/persistentCache';
import { emit, subscribe } from '$lib/realtime/broadcast';

const STALE_TIME_MS = 30_000;
const REVALIDATE_AFTER_MS = 20_000;
const CACHE_KEY = 'feed:desktop';
const PERSIST_LIMIT = 20;

interface PersistedFeed {
  posts: Post[];
  hasNext: boolean;
}

function createFeedCacheStore() {
  // Hydrate synchronously from sessionStorage so the first paint shows the
  // last known feed instead of a skeleton flash.
  const hydrated = readCache<PersistedFeed>(CACHE_KEY);

  let posts = $state<Post[]>(hydrated?.data.posts ?? []);
  let page = $state(0);
  let hasNext = $state(hydrated?.data.hasNext ?? false);
  let isLoadingMore = $state(false);
  let error = $state<string | null>(null);
  let loadedAt: number | null = hydrated?.loadedAt ?? null;
  let activeRequest: Promise<void> | null = null;

  function persist(): void {
    writeCache<PersistedFeed>(CACHE_KEY, {
      posts: posts.slice(0, PERSIST_LIMIT),
      hasNext,
    });
  }

  function isFresh(): boolean {
    return loadedAt !== null && Date.now() - loadedAt < STALE_TIME_MS;
  }

  function load(options: { force?: boolean } = {}): Promise<void> {
    const { force = false } = options;

    // If we already have fresh data and no force, skip the network entirely.
    if (!force && posts.length > 0 && isFresh()) {
      return Promise.resolve();
    }

    // De-dupe in-flight requests.
    if (activeRequest && !force) return activeRequest;

    const isInitial = posts.length === 0;
    error = null;
    page = 0;

    const req = postService
      .getFeed(0)
      .then((res) => {
        posts = res.content;
        hasNext = res.hasNext;
        loadedAt = Date.now();
        page = 0;
        persist();
      })
      .catch((err: unknown) => {
        if (isInitial) {
          error = err instanceof Error ? err.message : 'Failed to load feed.';
        }
      })
      .finally(() => {
        activeRequest = null;
      });

    activeRequest = req;
    return req;
  }

  async function loadMore(): Promise<void> {
    if (!hasNext || isLoadingMore) return;
    isLoadingMore = true;
    try {
      const next = page + 1;
      const res = await postService.getFeed(next);
      posts = [...posts, ...res.content];
      page = next;
      hasNext = res.hasNext;
    } catch {
      // non-fatal
    } finally {
      isLoadingMore = false;
    }
  }

  function prependPost(post: Post, options: { broadcast?: boolean } = {}): void {
    // Skip if we already have this post (avoids duplicate from echo).
    if (posts.some((p) => p.id === post.id)) return;
    posts = [post, ...posts];
    loadedAt = Date.now();
    persist();
    if (options.broadcast !== false) emit({ type: 'post:created', post });
  }

  function removePost(postId: string, options: { broadcast?: boolean } = {}): void {
    const before = posts.length;
    posts = posts.filter((p) => p.id !== postId);
    if (posts.length !== before) persist();
    if (options.broadcast !== false) emit({ type: 'post:deleted', postId });
  }

  function updatePost(postId: string, patch: Partial<Post>): void {
    let changed = false;
    posts = posts.map((p) => {
      if (p.id !== postId) return p;
      changed = true;
      return { ...p, ...patch };
    });
    if (changed) persist();
  }

  function invalidate(): void {
    loadedAt = null;
  }

  // Apply remote events from other tabs without rebroadcasting.
  subscribe((evt) => {
    if (evt.type === 'post:created') prependPost(evt.post, { broadcast: false });
    else if (evt.type === 'post:deleted') removePost(evt.postId, { broadcast: false });
  });

  // Revalidate silently when the tab becomes visible after a quiet stretch.
  if (typeof document !== 'undefined') {
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState !== 'visible') return;
      if (loadedAt === null || Date.now() - loadedAt < REVALIDATE_AFTER_MS) return;
      load({ force: true });
    });
  }

  return {
    get posts() {
      return posts;
    },
    get hasNext() {
      return hasNext;
    },
    get isLoadingMore() {
      return isLoadingMore;
    },
    get error() {
      return error;
    },
    get hasData(): boolean {
      return posts.length > 0;
    },
    load,
    loadMore,
    prependPost,
    removePost,
    updatePost,
    invalidate,
  };
}

export const feedCacheStore = createFeedCacheStore();
