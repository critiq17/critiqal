import { describe, it, expect, beforeEach } from 'vitest';
import { clearCache, getOrCreateQuery, invalidateQuery, setQueryData } from './cache';
import { Query } from './query.svelte';

beforeEach(() => {
	clearCache();
});

describe('getOrCreateQuery', () => {
	it('creates a new Query on first call', () => {
		const q = getOrCreateQuery('key1', () => new Query(() => Promise.resolve('a')));
		expect(q).toBeInstanceOf(Query);
	});

	it('returns the same instance on subsequent calls with the same key', () => {
		const q1 = getOrCreateQuery('key1', () => new Query(() => Promise.resolve('a')));
		const q2 = getOrCreateQuery('key1', () => new Query(() => Promise.resolve('b')));
		expect(q1).toBe(q2);
	});

	it('returns different instances for different keys', () => {
		const q1 = getOrCreateQuery('key1', () => new Query(() => Promise.resolve('a')));
		const q2 = getOrCreateQuery('key2', () => new Query(() => Promise.resolve('b')));
		expect(q1).not.toBe(q2);
	});
});

describe('invalidateQuery', () => {
	it('marks the query as stale', async () => {
		const q = getOrCreateQuery('inv', () => new Query(() => Promise.resolve('x'), 60_000));
		await q.fetch();
		expect(q.isStale).toBe(false);

		invalidateQuery('inv');
		expect(q.isStale).toBe(true);
	});

	it('does not throw when key does not exist', () => {
		expect(() => invalidateQuery('nonexistent')).not.toThrow();
	});
});

describe('setQueryData', () => {
	it('injects data into an existing query', () => {
		const q = getOrCreateQuery('set', () => new Query<string>(() => Promise.resolve('')));
		setQueryData('set', 'injected');
		expect(q.data).toBe('injected');
		expect(q.status).toBe('success');
	});

	it('does not throw when key does not exist', () => {
		expect(() => setQueryData('ghost', 'value')).not.toThrow();
	});
});
