import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('$lib/services', () => ({
  postService: {
    getReactions: vi.fn(),
    getMyReaction: vi.fn(),
    react: vi.fn(),
    removeReaction: vi.fn(),
  },
}));

vi.mock('$lib/stores/auth.store.svelte', () => ({
  authStore: { isAuthenticated: true },
}));

import { postService } from '$lib/services';
import { UseReactions } from './useReactions.svelte';

const mockPostService = postService as unknown as {
  getReactions: ReturnType<typeof vi.fn>;
  getMyReaction: ReturnType<typeof vi.fn>;
  react: ReturnType<typeof vi.fn>;
  removeReaction: ReturnType<typeof vi.fn>;
};

beforeEach(() => {
  vi.clearAllMocks();
  mockPostService.getReactions.mockResolvedValue({ GIGACHAD: 0, THE_ROCK: 0, DAVID: 0 });
  mockPostService.getMyReaction.mockResolvedValue(undefined);
  mockPostService.react.mockResolvedValue(undefined);
  mockPostService.removeReaction.mockResolvedValue(undefined);
});

describe('UseReactions', () => {
  it('starts with default state', () => {
    const r = new UseReactions('1');
    expect(r.loaded).toBe(false);
    expect(r.myReaction).toBeNull();
    expect(r.reacting).toBe(false);
  });

  it('loads reactions from service', async () => {
    mockPostService.getReactions.mockResolvedValue({ GIGACHAD: 5, THE_ROCK: 2, DAVID: 0 });
    mockPostService.getMyReaction.mockResolvedValue('GIGACHAD');

    const r = new UseReactions('42');
    await r.load();

    expect(r.loaded).toBe(true);
    expect(r.reactions.GIGACHAD).toBe(5);
    expect(r.myReaction).toBe('GIGACHAD');
  });

  it('does not re-load if already loaded', async () => {
    const r = new UseReactions('1');
    await r.load();
    await r.load();

    expect(mockPostService.getReactions).toHaveBeenCalledTimes(1);
  });

  it('optimistically adds a reaction', async () => {
    const r = new UseReactions('1');
    await r.load();

    await r.react('GIGACHAD');

    expect(r.reactions.GIGACHAD).toBe(1);
    expect(r.myReaction).toBe('GIGACHAD');
  });

  it('optimistically removes a reaction when same type clicked', async () => {
    mockPostService.getMyReaction.mockResolvedValue('GIGACHAD');
    mockPostService.getReactions.mockResolvedValue({ GIGACHAD: 3, THE_ROCK: 0, DAVID: 0 });

    const r = new UseReactions('1');
    await r.load();
    await r.react('GIGACHAD');

    expect(r.reactions.GIGACHAD).toBe(2);
    expect(r.myReaction).toBeNull();
    expect(mockPostService.removeReaction).toHaveBeenCalledOnce();
  });

  it('rolls back on API error', async () => {
    mockPostService.react.mockRejectedValue(new Error('network'));

    const r = new UseReactions('1');
    await r.load();

    const prevCount = r.reactions.GIGACHAD;
    await r.react('GIGACHAD');

    expect(r.reactions.GIGACHAD).toBe(prevCount);
    expect(r.myReaction).toBeNull();
  });
});
