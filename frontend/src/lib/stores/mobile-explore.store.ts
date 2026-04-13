import { writable } from 'svelte/store';
import type { Post, User } from '$lib/types';

export type ExploreTab = 'posts' | 'people';

export interface ExploreState {
	query: string;
	tab: ExploreTab;
	posts: Post[];
	users: User[];
	loadedAt: number | null;
}

export const mobileExploreStore = writable<ExploreState>({
	query: '',
	tab: 'posts',
	posts: [],
	users: [],
	loadedAt: null,
});
