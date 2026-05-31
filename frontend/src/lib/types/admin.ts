import type { UserBadge, BadgeCode } from './badge';

// Admin user row mirrors backend AdminUserDTO: identity + current badges.
export interface AdminUser {
  id: string;
  username: string;
  name: string | null;
  avatarUrl: string | null;
  badges: UserBadge[];
  banned?: boolean;
  bannedUntil?: string | null;
}

// Grantable badge definition mirrors backend AdminBadgeDTO.
export interface AdminBadge {
  code: BadgeCode;
  name: string;
  description: string;
  iconUrl: string | null;
}

export interface AdminMe {
  admin: boolean;
}

export interface AdminGrantResult {
  granted: string;
  user: string;
}

export interface AdminRevokeResult {
  revoked: string;
  user: string;
  removed: boolean;
}

export interface AdminBanResult {
  banned: string;
}

export interface AdminUnbanResult {
  unbanned: string;
  lifted: boolean;
}
