import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('$lib/api/client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}));

import { adminService } from './admin.service';
import { apiClient } from '$lib/api/client';

const get = apiClient.get as ReturnType<typeof vi.fn>;
const post = apiClient.post as ReturnType<typeof vi.fn>;
const del = apiClient.delete as ReturnType<typeof vi.fn>;

const SKIP = { skipUnauthorizedHandler: true };

describe('adminService', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('login posts credentials and skips the unauthorized handler', async () => {
    post.mockResolvedValue({ challengeToken: 'tok', method: 'TOTP' });

    const result = await adminService.login('admin', 'pw');

    expect(post).toHaveBeenCalledWith('/api/admin/auth/login', { username: 'admin', password: 'pw' }, SKIP);
    expect(result.challengeToken).toBe('tok');
  });

  it('verifyTwoFactor posts the challenge and code with skip flag', async () => {
    post.mockResolvedValue({ ok: true });

    await adminService.verifyTwoFactor('tok', '123456');

    expect(post).toHaveBeenCalledWith('/api/admin/auth/2fa', { challengeToken: 'tok', code: '123456' }, SKIP);
  });

  it('me hits /api/admin/me', async () => {
    get.mockResolvedValue({ admin: true });
    await adminService.me();
    expect(get).toHaveBeenCalledWith('/api/admin/me');
  });

  it('searchUsers builds a paged, encoded query', async () => {
    get.mockResolvedValue({ content: [], page: 0, size: 20, total: 0, hasNext: false });
    await adminService.searchUsers('a b', 2, 10);
    expect(get).toHaveBeenCalledWith('/api/admin/users/search?q=a%20b&page=2&size=10');
  });

  it('getUser hits the user endpoint', async () => {
    get.mockResolvedValue({ id: 'u1', username: 'x', name: null, avatarUrl: null, badges: [] });
    await adminService.getUser('u1');
    expect(get).toHaveBeenCalledWith('/api/admin/users/u1');
  });

  it('searchPosts builds a paged, encoded query', async () => {
    get.mockResolvedValue({ content: [], page: 0, size: 20, total: 0, hasNext: false });
    await adminService.searchPosts('hi', 0, 20);
    expect(get).toHaveBeenCalledWith('/api/admin/posts/search?q=hi&page=0&size=20');
  });

  it('listBadges hits /api/admin/badges', async () => {
    get.mockResolvedValue([]);
    await adminService.listBadges();
    expect(get).toHaveBeenCalledWith('/api/admin/badges');
  });

  it('grantBadge posts the code with skip flag', async () => {
    post.mockResolvedValue({ granted: 'ORIGIN', user: 'u1' });
    await adminService.grantBadge('u1', 'ORIGIN');
    expect(post).toHaveBeenCalledWith('/api/admin/users/u1/badges', { code: 'ORIGIN' }, SKIP);
  });

  it('revokeBadge deletes the code path', async () => {
    del.mockResolvedValue({ revoked: 'ORIGIN', user: 'u1', removed: true });
    await adminService.revokeBadge('u1', 'ORIGIN');
    expect(del).toHaveBeenCalledWith('/api/admin/users/u1/badges/ORIGIN');
  });

  it('logout posts with the skip flag', async () => {
    post.mockResolvedValue(undefined);
    await adminService.logout();
    expect(post).toHaveBeenCalledWith('/api/admin/auth/logout', undefined, SKIP);
  });
});
