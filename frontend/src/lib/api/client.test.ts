import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiClient, setInMemoryToken } from './client';

describe('apiClient', () => {
	beforeEach(() => {
		localStorage.clear();
		setInMemoryToken(null);
	});

	afterEach(() => {
		setInMemoryToken(null);
		vi.unstubAllGlobals();
		vi.restoreAllMocks();
	});

	it('adds auth and API headers for authenticated JSON requests', async () => {
		setInMemoryToken('secret-token');

		const fetchMock = vi.fn(async () => {
			return new Response(JSON.stringify([]), {
				status: 200,
				headers: { 'content-type': 'application/json' }
			});
		});
		vi.stubGlobal('fetch', fetchMock);

		await apiClient.get('/api/posts', true);

		expect(fetchMock).toHaveBeenCalledWith(
			'/api/posts',
			expect.objectContaining({
				method: 'GET',
				headers: expect.objectContaining({
					Accept: 'application/json',
					Authorization: 'Bearer secret-token',
					'Content-Type': 'application/json'
				})
			})
		);
	});

	it('normalizes unexpected HTML API responses into a readable ApiError', async () => {
		const fetchMock = vi.fn(async () => {
			return new Response('<html>ngrok warning</html>', {
				status: 200,
				headers: { 'content-type': 'text/html' }
			});
		});
		vi.stubGlobal('fetch', fetchMock);

		await expect(apiClient.get('/api/posts')).rejects.toMatchObject({
			name: 'ApiError',
			status: 200,
			message: 'Unexpected HTML response from API'
		});
	});

	it('maps abort failures to a timeout ApiError', async () => {
		const fetchMock = vi.fn(async () => {
			const error = new Error('The operation was aborted.');
			error.name = 'AbortError';
			throw error;
		});
		vi.stubGlobal('fetch', fetchMock);

		await expect(apiClient.get('/api/posts')).rejects.toMatchObject({
			name: 'ApiError',
			status: 408,
			message: 'Request timed out. Please try again.'
		});
	});
});
