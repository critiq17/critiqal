import { postService } from '$lib/services';
import type { Post } from '$lib/types';

const STALE_TIME_MS = 30_000;

function createFeedCacheStore() {
	let posts = $state<Post[]>([]);
	let page = $state(0);
	let hasNext = $state(false);
	let isLoadingMore = $state(false);
	let error = $state<string | null>(null);
	let loadedAt: number | null = null;
	let activeRequest: Promise<void> | null = null;

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

	function prependPost(post: Post): void {
		posts = [post, ...posts];
		loadedAt = Date.now();
	}

	function removePost(postId: number): void {
		posts = posts.filter((p) => p.id !== postId);
	}

	function invalidate(): void {
		loadedAt = null;
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
		invalidate,
	};
}

export const feedCacheStore = createFeedCacheStore();
