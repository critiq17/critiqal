import type { UserBadge } from './badge';

export interface User {
  id: string;
  username: string;
  name: string | null;
  bio: string | null;
  avatarUrl: string | null;
  email: string | null;
  emailVerified: boolean;
  pendingEmail: string | null;
  twoFactorEnabled: boolean;
  createdAt: string;
  badges?: UserBadge[];
  stats?: { postsCount: number; followersCount: number; followingCount: number } | null;
  isFollowing?: boolean | null;
}

export interface UpdateProfileRequest {
  name?: string;
  bio?: string;
  avatarUrl?: string;
}

export interface UserStats {
  postsCount: number;
  followersCount: number;
  followingCount: number;
}
