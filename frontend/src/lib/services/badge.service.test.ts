import { describe, it, expect, vi, beforeEach } from 'vitest';
import { badgeService } from './badge.service';
import { apiClient } from '$lib/api/client';
import type { UserBadge } from '$lib/types';

vi.mock('$lib/api/client', () => ({
	apiClient: {
		get: vi.fn(),
		post: vi.fn()
	}
}));

describe('badgeService', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('lists badges for a user', async () => {
		const badges: UserBadge[] = [
			{
				id: 'b1',
				code: 'ORIGIN',
				name: 'Origin',
				description: 'Founding member',
				iconUrl: null,
				metadata: null,
				awardedAt: '2026-05-29T00:00:00Z'
			},
			{
				id: 'b2',
				code: 'CENTURION',
				name: 'Centurion',
				description: 'Reached 100',
				iconUrl: 'https://example.test/centurion.png',
				metadata: { count: 100 },
				awardedAt: '2026-05-29T01:00:00Z'
			}
		];
		vi.mocked(apiClient.get).mockResolvedValue(badges);

		const result = await badgeService.listForUser('user-1');

		expect(apiClient.get).toHaveBeenCalledWith('/api/users/user-1/badges');
		expect(result).toEqual(badges);
	});

	it('returns an empty array when the user has no badges', async () => {
		vi.mocked(apiClient.get).mockResolvedValue([]);

		const result = await badgeService.listForUser('user-2');

		expect(apiClient.get).toHaveBeenCalledWith('/api/users/user-2/badges');
		expect(result).toEqual([]);
	});
});
