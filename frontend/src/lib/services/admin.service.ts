import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';
import type {
  AdminUser,
  AdminBadge,
  AdminMe,
  AdminGrantResult,
  AdminRevokeResult,
  AdminBanResult,
  AdminUnbanResult,
  PageResponse,
  Post,
  TwoFactorChallenge,
} from '$lib/types';

// Admin denials are 404 (safe), but admin 2FA returns 401. Every admin POST
// passes skipUnauthorizedHandler so a 401 never logs out the normal logged-in
// user. GET/DELETE on /api/admin/* return 404 on denial, so they are safe as-is
// (apiClient.get/delete also do not accept the option).
const SKIP = { skipUnauthorizedHandler: true } as const;

export const adminService = {
  login(username: string, password: string): Promise<TwoFactorChallenge> {
    return apiClient.post<TwoFactorChallenge>(API.admin.login, { username, password }, SKIP);
  },

  verifyTwoFactor(challengeToken: string, code: string): Promise<{ ok: boolean }> {
    return apiClient.post<{ ok: boolean }>(API.admin.login2fa, { challengeToken, code }, SKIP);
  },

  me(): Promise<AdminMe> {
    return apiClient.get<AdminMe>(API.admin.me);
  },

  logout(): Promise<void> {
    return apiClient.post<void>(API.admin.logout, undefined, SKIP);
  },

  searchUsers(q: string, page = 0, size = 20): Promise<PageResponse<AdminUser>> {
    return apiClient.get<PageResponse<AdminUser>>(API.admin.usersSearch(q, page, size));
  },

  getUser(id: string): Promise<AdminUser> {
    return apiClient.get<AdminUser>(API.admin.user(id));
  },

  searchPosts(q: string, page = 0, size = 20): Promise<PageResponse<Post>> {
    return apiClient.get<PageResponse<Post>>(API.admin.postsSearch(q, page, size));
  },

  listBadges(): Promise<AdminBadge[]> {
    return apiClient.get<AdminBadge[]>(API.admin.badges);
  },

  grantBadge(userId: string, code: string): Promise<AdminGrantResult> {
    return apiClient.post<AdminGrantResult>(API.admin.grantBadge(userId), { code }, SKIP);
  },

  revokeBadge(userId: string, code: string): Promise<AdminRevokeResult> {
    return apiClient.delete<AdminRevokeResult>(API.admin.revokeBadge(userId, code));
  },

  banUser(userId: string, reason: string, days?: number): Promise<AdminBanResult> {
    return apiClient.post<AdminBanResult>(
      API.admin.banUser(userId),
      { reason, days: days?.toString() ?? '' },
      SKIP
    );
  },

  unbanUser(userId: string): Promise<AdminUnbanResult> {
    return apiClient.delete<AdminUnbanResult>(API.admin.unbanUser(userId));
  },
};
