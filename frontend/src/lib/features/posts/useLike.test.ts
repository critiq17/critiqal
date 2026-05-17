import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

vi.mock('$lib/stores/auth.store.svelte', () => ({
  authStore: { isAuthenticated: true },
}));

import { authStore } from '$lib/stores/auth.store.svelte';
import { UseLike } from './useLike.svelte';
import type { LikeResponse } from '$lib/types';

const mutableAuth = authStore as unknown as { isAuthenticated: boolean };

beforeEach(() => {
  mutableAuth.isAuthenticated = true;
});

afterEach(() => {
  vi.useRealTimers();
});

describe('UseLike', () => {
  it('seeds state from constructor args', () => {
    const like = new UseLike(() => Promise.resolve({ likedByMe: true, count: 3 }), true, 3);
    expect(like.liked).toBe(true);
    expect(like.count).toBe(3);
    expect(like.pending).toBe(false);
  });

  it('clamps a negative initial count to zero', () => {
    const like = new UseLike(() => Promise.resolve({ likedByMe: false, count: 0 }), false, -5);
    expect(like.count).toBe(0);
  });

  it('optimistically likes then reconciles with the server', async () => {
    const fn = vi.fn<() => Promise<LikeResponse>>().mockResolvedValue({
      likedByMe: true,
      count: 6,
    });
    const like = new UseLike(fn, false, 5);

    const promise = like.toggle();
    // Optimistic state applied synchronously before the request resolves.
    expect(like.liked).toBe(true);
    expect(like.count).toBe(6);

    await promise;
    expect(fn).toHaveBeenCalledOnce();
    expect(like.liked).toBe(true);
    expect(like.count).toBe(6);
    expect(like.pending).toBe(false);
  });

  it('optimistically unlikes when already liked', async () => {
    const fn = vi.fn<() => Promise<LikeResponse>>().mockResolvedValue({
      likedByMe: false,
      count: 2,
    });
    const like = new UseLike(fn, true, 3);

    await like.toggle();

    expect(like.liked).toBe(false);
    expect(like.count).toBe(2);
  });

  it('rolls back to the previous state on API error', async () => {
    const fn = vi.fn<() => Promise<LikeResponse>>().mockRejectedValue(new Error('network'));
    const like = new UseLike(fn, false, 4);

    await like.toggle();

    expect(like.liked).toBe(false);
    expect(like.count).toBe(4);
    expect(like.pending).toBe(false);
  });

  it('does nothing when the user is not authenticated', async () => {
    mutableAuth.isAuthenticated = false;
    const fn = vi.fn<() => Promise<LikeResponse>>();
    const like = new UseLike(fn, false, 1);

    await like.toggle();

    expect(fn).not.toHaveBeenCalled();
    expect(like.liked).toBe(false);
    expect(like.count).toBe(1);
  });

  it('ignores re-entrant toggles while a request is pending', async () => {
    let resolve!: (v: LikeResponse) => void;
    const fn = vi
      .fn<() => Promise<LikeResponse>>()
      .mockReturnValue(new Promise<LikeResponse>((r) => (resolve = r)));
    const like = new UseLike(fn, false, 0);

    const first = like.toggle();
    await like.toggle(); // should be ignored — request in flight
    expect(fn).toHaveBeenCalledOnce();

    resolve({ likedByMe: true, count: 1 });
    await first;
    expect(like.count).toBe(1);
  });

  it('floors the optimistic count at zero when unliking from zero', async () => {
    const fn = vi
      .fn<() => Promise<LikeResponse>>()
      .mockResolvedValue({ likedByMe: false, count: 0 });
    const like = new UseLike(fn, true, 0);

    const promise = like.toggle();
    expect(like.count).toBe(0); // never goes negative

    await promise;
    expect(like.liked).toBe(false);
    expect(like.count).toBe(0);
  });

  it('pops while liking and clears the flag after the pop window', async () => {
    vi.useFakeTimers();
    const fn = vi
      .fn<() => Promise<LikeResponse>>()
      .mockResolvedValue({ likedByMe: true, count: 1 });
    const like = new UseLike(fn, false, 0);

    const promise = like.toggle();
    expect(like.popping).toBe(true);

    vi.advanceTimersByTime(360);
    expect(like.popping).toBe(false);

    await promise;
  });

  it('does not pop when unliking', async () => {
    const fn = vi
      .fn<() => Promise<LikeResponse>>()
      .mockResolvedValue({ likedByMe: false, count: 0 });
    const like = new UseLike(fn, true, 1);

    await like.toggle();

    expect(like.popping).toBe(false);
  });
});
