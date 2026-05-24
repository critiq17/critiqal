import { postService } from '$lib/services';
import type { Post } from '$lib/types';
import { readCache, writeCache } from '$lib/utils/persistentCache';
import { emit, subscribe } from '$lib/realtime/broadcast';

export type FeedStatus = 'idle' | 'loading' | 'loaded' | 'error';

const FEED_CACHE_TTL_MS = 30_000;
const STALE_LOADING_MS = 10_000;
const REVALIDATE_AFTER_MS = 20_000;
const CACHE_KEY = 'feed:mobile';
const PERSIST_LIMIT = 20;

interface PersistedFeed {
  posts: Post[];
  hasNext: boolean;
}

function toFeedError(error: unknown): string {
  const message = error instanceof Error ? error.message : String(error);
  return message.length > 120 ? 'Feed load failed (check console)' : message;
}

function createMobileFeedStore() {
  // Hydrate from localStorage so a cold reopen of the TMA paints last seen
  // posts immediately; revalidate runs in the background.
  const hydrated = readCache<PersistedFeed>(CACHE_KEY);

  let posts = $state<Post[]>(hydrated?.data.posts ?? []);
  let page = $state(0);
  let hasNext = $state(hydrated?.data.hasNext ?? false);
  let status = $state<FeedStatus>(hydrated ? 'loaded' : 'idle');
  let isLoadingMore = $state(false);
  let error = $state<string | null>(null);
  let loadedAt: number | null = hydrated?.loadedAt ?? null;
  let requestStartedAt: number | null = null;
  let activeRequestId = 0;
  let activeRequest: Promise<Post[]> | null = null;

  function persist(): void {
    writeCache<PersistedFeed>(CACHE_KEY, {
      posts: posts.slice(0, PERSIST_LIMIT),
      hasNext,
    });
  }

  function isLoadingStale(): boolean {
    if (status !== 'loading') return false;
    if (requestStartedAt === null) return true;
    return Date.now() - requestStartedAt >= STALE_LOADING_MS;
  }

  function needsRefresh(): boolean {
    if (posts.length === 0) return true;
    if (loadedAt === null) return true;
    return Date.now() - loadedAt >= FEED_CACHE_TTL_MS;
  }

  function reset(): void {
    activeRequestId += 1;
    activeRequest = null;
    posts = [];
    page = 0;
    hasNext = false;
    status = 'idle';
    isLoadingMore = false;
    error = null;
    loadedAt = null;
    requestStartedAt = null;
    persist();
  }

  function prependPost(post: Post, options: { broadcast?: boolean } = {}): void {
    if (posts.some((p) => p.id === post.id)) return;
    posts = [post, ...posts];
    loadedAt = Date.now();
    status = 'loaded';
    error = null;
    persist();
    if (options.broadcast !== false) emit({ type: 'post:created', post });
  }

  function removePost(postId: string, options: { broadcast?: boolean } = {}): void {
    const before = posts.length;
    posts = posts.filter((p) => p.id !== postId);
    if (posts.length !== before) persist();
    if (options.broadcast !== false) emit({ type: 'post:deleted', postId });
  }

  // Apply remote events from other tabs without rebroadcasting.
  subscribe((evt) => {
    if (evt.type === 'post:created') prependPost(evt.post, { broadcast: false });
    else if (evt.type === 'post:deleted') removePost(evt.postId, { broadcast: false });
  });

  // Silent revalidate when tab returns to foreground after a quiet stretch.
  if (typeof document !== 'undefined') {
    document.addEventListener('visibilitychange', () => {
      if (document.visibilityState !== 'visible') return;
      if (loadedAt === null || Date.now() - loadedAt < REVALIDATE_AFTER_MS) return;
      load({ force: true }).catch(() => {});
    });
  }

  function load(options: { force?: boolean } = {}): Promise<Post[]> {
    const { force = false } = options;
    const staleLoading = isLoadingStale();

    if (!force && activeRequest && !staleLoading) {
      return activeRequest;
    }

    const requestId = ++activeRequestId;
    status = 'loading';
    error = null;
    requestStartedAt = Date.now();
    page = 0;

    const request = postService
      .getFeed(0)
      .then((pageResponse) => {
        if (requestId === activeRequestId) {
          posts = pageResponse.content;
          hasNext = pageResponse.hasNext;
          loadedAt = Date.now();
          page = 0;
          status = 'loaded';
          error = null;
          requestStartedAt = null;
          persist();
        }
        return pageResponse.content;
      })
      .catch((err: unknown) => {
        if (requestId === activeRequestId) {
          const message = toFeedError(err);
          status = 'error';
          error = message;
          requestStartedAt = null;
        }
        throw err;
      })
      .finally(() => {
        if (requestId === activeRequestId) {
          activeRequest = null;
        }
      });

    activeRequest = request;
    return request;
  }

  async function loadMore(): Promise<void> {
    if (!hasNext || isLoadingMore || status === 'loading') return;

    isLoadingMore = true;
    try {
      const nextPage = page + 1;
      const pageResponse = await postService.getFeed(nextPage);
      posts = [...posts, ...pageResponse.content];
      page = nextPage;
      hasNext = pageResponse.hasNext;
    } catch {
      // Non-fatal — leave posts intact, just stop loading indicator
    } finally {
      isLoadingMore = false;
    }
  }

  function ensureLoaded(options: { force?: boolean } = {}): Promise<Post[]> {
    const { force = false } = options;

    if (force) return load({ force: true });
    if (isLoadingStale()) return load({ force: true });
    if (status === 'loading' && activeRequest) return activeRequest;
    if (needsRefresh()) return load();

    return Promise.resolve(posts);
  }

  return {
    get posts() {
      return posts;
    },
    get page() {
      return page;
    },
    get hasNext() {
      return hasNext;
    },
    get status() {
      return status;
    },
    get isLoadingMore() {
      return isLoadingMore;
    },
    get error() {
      return error;
    },
    load,
    loadMore,
    ensureLoaded,
    prependPost,
    removePost,
    reset,
  };
}

export const mobileFeedStore = createMobileFeedStore();
