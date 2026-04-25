import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('$lib/services', () => ({
  postService: {
    getComments: vi.fn(),
    addComment: vi.fn(),
    deleteComment: vi.fn(),
    getReplies: vi.fn(),
    addReply: vi.fn(),
  },
}));

import { postService } from '$lib/services';
import { UseComments } from './useComments.svelte';

const mockPostService = postService as unknown as {
  getComments: ReturnType<typeof vi.fn>;
  addComment: ReturnType<typeof vi.fn>;
  deleteComment: ReturnType<typeof vi.fn>;
  getReplies: ReturnType<typeof vi.fn>;
  addReply: ReturnType<typeof vi.fn>;
};

const makeComment = (id: number, content = 'test') => ({
  id,
  content,
  author: { id: 1, username: 'user', name: null, bio: null, avatarUrl: null, createdAt: '' },
  postId: 1,
  parentId: null,
  replyCount: 0,
  createdAt: new Date().toISOString(),
});

beforeEach(() => {
  vi.clearAllMocks();
  mockPostService.getComments.mockResolvedValue([]);
  mockPostService.addComment.mockResolvedValue(makeComment(99, 'new comment'));
  mockPostService.deleteComment.mockResolvedValue(undefined);
  mockPostService.getReplies.mockResolvedValue([]);
  mockPostService.addReply.mockResolvedValue(makeComment(100, 'reply'));
});

describe('UseComments', () => {
  it('starts empty and not loaded', () => {
    const c = new UseComments(1);
    expect(c.comments).toHaveLength(0);
    expect(c.loaded).toBe(false);
    expect(c.submitting).toBe(false);
  });

  it('loads comments from service', async () => {
    mockPostService.getComments.mockResolvedValue([makeComment(1), makeComment(2)]);
    const c = new UseComments(1);
    await c.load();

    expect(c.comments).toHaveLength(2);
    expect(c.loaded).toBe(true);
  });

  it('does not reload when already loaded', async () => {
    const c = new UseComments(1);
    await c.load();
    await c.load();
    expect(mockPostService.getComments).toHaveBeenCalledTimes(1);
  });

  it('toggle loads and expands comments', async () => {
    const c = new UseComments(1);
    await c.toggle();

    expect(c.expanded).toBe(true);
    expect(c.loaded).toBe(true);
  });

  it('toggle collapses on second call', async () => {
    const c = new UseComments(1);
    await c.toggle();
    await c.toggle();
    expect(c.expanded).toBe(false);
  });

  it('submit prepends new comment and clears input', async () => {
    const c = new UseComments(1);
    await c.load();
    c.newComment = 'hello world';
    await c.submit();

    expect(c.comments).toHaveLength(1);
    expect(c.newComment).toBe('');
    expect(c.comments[0]?.content).toBe('new comment');
  });

  it('submit does nothing when text is empty', async () => {
    const c = new UseComments(1);
    c.newComment = '   ';
    await c.submit();
    expect(mockPostService.addComment).not.toHaveBeenCalled();
  });

  it('deleteComment removes from list', async () => {
    mockPostService.getComments.mockResolvedValue([makeComment(1), makeComment(2)]);
    const c = new UseComments(1);
    await c.load();
    await c.deleteComment(1);

    expect(c.comments).toHaveLength(1);
    expect(c.comments[0]?.id).toBe(2);
  });

  it('toggleReplies fetches replies on first call', async () => {
    mockPostService.getReplies.mockResolvedValue([makeComment(10, 'reply')]);
    const c = new UseComments(1);
    await c.toggleReplies(5);

    const rs = c.getReplyState(5);
    expect(rs.loaded).toBe(true);
    expect(rs.expanded).toBe(true);
    expect(rs.replies).toHaveLength(1);
  });
});
