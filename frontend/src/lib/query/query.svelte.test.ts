import { describe, it, expect, vi, beforeEach } from 'vitest';
import { Query } from './query.svelte';

describe('Query', () => {
  it('starts in idle state', () => {
    const q = new Query(() => Promise.resolve('data'));
    expect(q.status).toBe('idle');
    expect(q.data).toBeNull();
    expect(q.error).toBeNull();
    expect(q.isLoading).toBe(false);
  });

  it('transitions to loading then success', async () => {
    const fetcher = vi.fn().mockResolvedValue('hello');
    const q = new Query(fetcher);

    const promise = q.fetch();
    expect(q.status).toBe('loading');

    await promise;
    expect(q.status).toBe('success');
    expect(q.data).toBe('hello');
    expect(q.error).toBeNull();
  });

  it('transitions to error on rejection', async () => {
    const fetcher = vi.fn().mockRejectedValue(new Error('boom'));
    const q = new Query(fetcher);

    await q.fetch();
    expect(q.status).toBe('error');
    expect(q.error?.message).toBe('boom');
    expect(q.data).toBeNull();
  });

  it('wraps non-Error rejections', async () => {
    const q = new Query(() => Promise.reject('string error'));
    await q.fetch();
    expect(q.error).toBeInstanceOf(Error);
    expect(q.error?.message).toBe('string error');
  });

  it('returns cached data when not stale', async () => {
    const fetcher = vi.fn().mockResolvedValue('cached');
    const q = new Query(fetcher, 60_000);

    await q.fetch();
    await q.fetch();

    expect(fetcher).toHaveBeenCalledTimes(1);
    expect(q.data).toBe('cached');
  });

  it('re-fetches when forced', async () => {
    const fetcher = vi.fn().mockResolvedValue('fresh');
    const q = new Query(fetcher, 60_000);

    await q.fetch();
    await q.fetch(true);

    expect(fetcher).toHaveBeenCalledTimes(2);
  });

  it('re-fetches after invalidation', async () => {
    const fetcher = vi.fn().mockResolvedValue('value');
    const q = new Query(fetcher, 60_000);

    await q.fetch();
    q.invalidate();
    await q.fetch();

    expect(fetcher).toHaveBeenCalledTimes(2);
  });

  it('isStale is true before first fetch', () => {
    const q = new Query(() => Promise.resolve('x'));
    expect(q.isStale).toBe(true);
  });

  it('isStale is false immediately after fetch within staleTime', async () => {
    const q = new Query(() => Promise.resolve('x'), 60_000);
    await q.fetch();
    expect(q.isStale).toBe(false);
  });

  it('invalidate marks query as stale', async () => {
    const q = new Query(() => Promise.resolve('x'), 60_000);
    await q.fetch();
    q.invalidate();
    expect(q.isStale).toBe(true);
  });

  it('setData sets data and marks success', () => {
    const q = new Query<string>(() => Promise.resolve(''));
    q.setData('injected');
    expect(q.data).toBe('injected');
    expect(q.status).toBe('success');
    expect(q.isStale).toBe(false);
  });

  it('abort does not crash when no request is active', () => {
    const q = new Query(() => Promise.resolve('x'));
    expect(() => q.abort()).not.toThrow();
  });
});
