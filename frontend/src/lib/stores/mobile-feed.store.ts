import { writable } from 'svelte/store';
import type { Post } from '$lib/types';

interface FeedState {
	posts: Post[];
	loadedAt: number | null;
	page: number;
	isLoading: boolean;
}

export const mobileFeedStore = writable<FeedState>({
	posts: [],
	loadedAt: null,
	page: 0,
	isLoading: false,
});
