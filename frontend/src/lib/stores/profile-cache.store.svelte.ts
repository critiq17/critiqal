import type { User, Post } from '$lib/types';

interface ProfileEntry {
	profile: User | null;
	posts: Post[];
	postsHasNext: boolean;
	postsPage: number;
	followersList: User[];
	followingList: User[];
	listsLoaded: boolean;
	loadedAt: number;
}

const STALE_TIME_MS = 60_000;

const cache = new Map<string, ProfileEntry>();

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
};
