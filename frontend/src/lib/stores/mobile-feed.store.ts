import { get, writable } from 'svelte/store';
import { postService } from '$lib/services';
import type { Post } from '$lib/types';

export type FeedStatus = 'idle' | 'loading' | 'loaded' | 'error';

export interface FeedState {
	posts: Post[];
	loadedAt: number | null;
	page: number;
	status: FeedStatus;
	error: string | null;
	requestStartedAt: number | null;
}

const FEED_CACHE_TTL_MS = 30000;
const STALE_LOADING_MS = 10000;

const INITIAL_STATE: FeedState = {
	posts: [],
	loadedAt: null,
	page: 0,
	status: 'idle',
	error: null,
	requestStartedAt: null
};

function toFeedError(error: unknown): string {
	const message = error instanceof Error ? error.message : String(error);
	return message.length > 120 ? 'Feed load failed (check console)' : message;
}

function createMobileFeedStore() {
	const store = writable<FeedState>(INITIAL_STATE);
	let activeRequestId = 0;
	let activeRequest: Promise<Post[]> | null = null;

	function isLoadingStale(state: FeedState): boolean {
		if (state.status !== 'loading') return false;
		if (state.requestStartedAt === null) return true;
		return Date.now() - state.requestStartedAt >= STALE_LOADING_MS;
	}

	function needsRefresh(state: FeedState): boolean {
		if (state.posts.length === 0) return true;
		if (state.loadedAt === null) return true;
		return Date.now() - state.loadedAt >= FEED_CACHE_TTL_MS;
	}

	function subscribe(run: (value: FeedState) => void) {
		return store.subscribe(run);
	}

	function reset(): void {
		activeRequestId += 1;
		activeRequest = null;
		store.set(INITIAL_STATE);
	}

	function prependPost(post: Post): void {
		store.update((state) => ({
			...state,
			posts: [post, ...state.posts],
			loadedAt: Date.now(),
			status: 'loaded',
			error: null
		}));
	}

	function setPage(page: number): void {
		store.update((state) => ({ ...state, page }));
	}

	function markError(error: unknown): void {
		const message = toFeedError(error);
		console.error('[mobileFeedStore] load failed:', error);
		store.update((state) => ({
			...state,
			status: 'error',
			error: message,
			requestStartedAt: null
		}));
	}

	function markLoaded(posts: Post[], resetPage: boolean): void {
		store.update((state) => ({
			...state,
			posts,
			loadedAt: Date.now(),
			page: resetPage ? 0 : state.page,
			status: 'loaded',
			error: null,
			requestStartedAt: null
		}));
	}

	function load(options: { force?: boolean; resetPage?: boolean } = {}): Promise<Post[]> {
		const { force = false, resetPage = false } = options;
		const state = get({ subscribe });
		const staleLoading = isLoadingStale(state);

		if (!force && activeRequest && !staleLoading) {
			return activeRequest;
		}

		const requestId = ++activeRequestId;
		store.update((current) => ({
			...current,
			status: 'loading',
			error: null,
			requestStartedAt: Date.now(),
			page: resetPage ? 0 : current.page
		}));

		const request = postService
			.getFeed()
			.then((posts) => {
				if (requestId === activeRequestId) {
					markLoaded(posts, resetPage);
				}
				return posts;
			})
			.catch((error) => {
				if (requestId === activeRequestId) {
					markError(error);
				}
				throw error;
			})
			.finally(() => {
				if (requestId === activeRequestId) {
					activeRequest = null;
				}
			});

		activeRequest = request;
		return request;
	}

	function ensureLoaded(options: { force?: boolean; resetPage?: boolean } = {}): Promise<Post[]> {
		const { force = false, resetPage = false } = options;
		const state = get({ subscribe });

		if (force) {
			return load({ force: true, resetPage });
		}

		if (isLoadingStale(state)) {
			return load({ force: true, resetPage });
		}

		if (state.status === 'loading' && activeRequest) {
			return activeRequest;
		}

		if (needsRefresh(state)) {
			return load({ resetPage });
		}

		return Promise.resolve(state.posts);
	}

	return {
		subscribe,
		load,
		ensureLoaded,
		prependPost,
		reset,
		setPage
	};
}

export const mobileFeedStore = createMobileFeedStore();
