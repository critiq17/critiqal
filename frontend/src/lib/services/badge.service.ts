import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';
import type { UserBadge } from '$lib/types';

// Service for badge endpoints
export const badgeService = {
	async listForUser(userId: string): Promise<UserBadge[]> {
		return apiClient.get<UserBadge[]>(API.users.badges(userId));
	}
};
