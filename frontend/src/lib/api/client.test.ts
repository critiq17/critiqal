import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { apiClient, registerUnauthorizedHandler } from './client';

describe('apiClient', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('sends credentials: include on every request', async () => {
    const fetchMock = vi.fn(async () => {
      return new Response(JSON.stringify([]), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      });
    });
    vi.stubGlobal('fetch', fetchMock);

    await apiClient.get('/api/posts');

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/posts',
      expect.objectContaining({ credentials: 'include' })
    );
  });

  it('never sends an Authorization header', async () => {
    const fetchMock = vi.fn(async () => {
      return new Response(JSON.stringify([]), {
        status: 200,
        headers: { 'content-type': 'application/json' },
      });
    });
    vi.stubGlobal('fetch', fetchMock);

    await apiClient.get('/api/posts');

    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    const headers = init.headers as Record<string, string>;
    expect(headers).not.toHaveProperty('Authorization');
  });

  it('triggers the onUnauthorized handler on a 401 response', async () => {
    const handler = vi.fn();
    registerUnauthorizedHandler(handler);

    const fetchMock = vi.fn(async () => {
      return new Response(JSON.stringify({ message: 'Unauthorized' }), {
        status: 401,
        headers: { 'content-type': 'application/json' },
      });
    });
    vi.stubGlobal('fetch', fetchMock);

    await expect(apiClient.get('/api/protected')).rejects.toMatchObject({ status: 401 });
    expect(handler).toHaveBeenCalledTimes(1);
  });

  it('does NOT trigger onUnauthorized on a 5xx response', async () => {
    const handler = vi.fn();
    registerUnauthorizedHandler(handler);

    const fetchMock = vi.fn(async () => {
      return new Response(JSON.stringify({ message: 'Internal Server Error' }), {
        status: 500,
        headers: { 'content-type': 'application/json' },
      });
    });
    vi.stubGlobal('fetch', fetchMock);

    await expect(apiClient.get('/api/posts')).rejects.toMatchObject({ status: 500 });
    expect(handler).not.toHaveBeenCalled();
  });

  it('maps abort failures to a timeout ApiError with status 408', async () => {
    const fetchMock = vi.fn(async () => {
      const error = new Error('The operation was aborted.');
      error.name = 'AbortError';
      throw error;
    });
    vi.stubGlobal('fetch', fetchMock);

    await expect(apiClient.get('/api/posts')).rejects.toMatchObject({
      name: 'ApiError',
      status: 408,
      message: 'Request timed out. Please try again.',
    });
  });

  it('normalizes unexpected HTML API responses into a readable ApiError', async () => {
    const fetchMock = vi.fn(async () => {
      return new Response('<html>ngrok warning</html>', {
        status: 200,
        headers: { 'content-type': 'text/html' },
      });
    });
    vi.stubGlobal('fetch', fetchMock);

    await expect(apiClient.get('/api/posts')).rejects.toMatchObject({
      name: 'ApiError',
      status: 200,
      message: 'Unexpected HTML response from API',
    });
  });
});
