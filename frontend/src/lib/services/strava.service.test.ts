import { beforeEach, describe, expect, it, vi } from 'vitest';

const apiClientGet = vi.fn();
const apiClientDelete = vi.fn();

vi.mock('$lib/api/client', () => ({
	apiClient: {
		get: (path: string) => apiClientGet(path),
		delete: (path: string) => apiClientDelete(path)
	}
}));

import { stravaService } from './strava.service';

describe('stravaService', () => {
	beforeEach(() => {
		vi.clearAllMocks();
	});

	it('normalizes 204-style empty connection responses to null', async () => {
		apiClientGet.mockResolvedValueOnce(undefined);

		await expect(stravaService.getConnection()).resolves.toBeNull();
	});

	it('normalizes empty public connection responses to null', async () => {
		apiClientGet.mockResolvedValueOnce(undefined);

		await expect(stravaService.getPublicConnection('user-uuid')).resolves.toBeNull();
	});
});
