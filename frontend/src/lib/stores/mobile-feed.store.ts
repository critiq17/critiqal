import { get, writable } from 'svelte/store';
import { postService } from '$lib/services';
import type { Post } from '$lib/types';

export type FeedStatus = 'idle' | 'loading' | 'loaded' | 'error';

export interface FeedState {
	posts: Post[];
	loadedAt: number | null;
	page: number;
	hasNext: boolean;
	status: FeedStatus;
	isLoadingMore: boolean;
	error: string | null;
	requestStartedAt: number | null;
}

const FEED_CACHE_TTL_MS = 30000;
const STALE_LOADING_MS = 10000;

const INITIAL_STATE: FeedState = {
	posts: [],
	loadedAt: null,
	page: 0,
	hasNext: false,
	status: 'idle',
	isLoadingMore: false,
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

	function markError(error: unknown): void {
		const message = toFeedError(error);
		store.update((state) => ({
			...state,
			status: 'error',
			error: message,
			requestStartedAt: null
		}));
	}

	function markLoaded(posts: Post[], hasNext: boolean): void {
		store.update((state) => ({
			...state,
			posts,
			hasNext,
			loadedAt: Date.now(),
			page: 0,
			status: 'loaded',
			error: null,
			requestStartedAt: null
		}));
	}

	function load(options: { force?: boolean; resetPage?: boolean } = {}): Promise<Post[]> {
		const { force = false } = options;
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
			page: 0
		}));

		const request = postService
			.getFeed(0)
			.then((pageResponse) => {
				if (requestId === activeRequestId) {
					markLoaded(pageResponse.content, pageResponse.hasNext);
				}
				return pageResponse.content;
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

	async function loadMore(): Promise<void> {
		const state = get({ subscribe });
		if (!state.hasNext || state.isLoadingMore || state.status === 'loading') return;

		store.update((current) => ({ ...current, isLoadingMore: true }));

		try {
			const nextPage = state.page + 1;
			const pageResponse = await postService.getFeed(nextPage);
			store.update((current) => ({
				...current,
				posts: [...current.posts, ...pageResponse.content],
				page: nextPage,
				hasNext: pageResponse.hasNext,
				isLoadingMore: false
			}));
		} catch {
			// Non-fatal: leave posts intact, just stop loading indicator
			store.update((current) => ({ ...current, isLoadingMore: false }));
		}
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
		loadMore,
		ensureLoaded,
		prependPost,
		reset
	};
}

export const mobileFeedStore = createMobileFeedStore();
