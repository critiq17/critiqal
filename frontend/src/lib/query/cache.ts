import { Query } from './query.svelte';

const cache = new Map<string, Query<unknown>>();

export function getOrCreateQuery<T>(key: string, factory: () => Query<T>): Query<T> {
	if (!cache.has(key)) {
		cache.set(key, factory() as Query<unknown>);
	}
	return cache.get(key) as Query<T>;
}

export function invalidateQuery(key: string): void {
	cache.get(key)?.invalidate();
}

export function setQueryData<T>(key: string, data: T): void {
	(cache.get(key) as Query<T> | undefined)?.setData(data);
}

export function clearCache(): void {
	cache.clear();
}
