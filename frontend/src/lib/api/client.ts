import { ApiError } from '$lib/types';

// Empty string = relative URLs (proxied via Vite dev server or same-origin in prod)
const BASE_URL: string = import.meta.env.VITE_API_URL ?? '';
const REQUEST_TIMEOUT_MS = 15000;

let memoryToken: string | null = null;
let onUnauthorized: (() => void) | null = null;

export function registerUnauthorizedHandler(handler: () => void): void {
  onUnauthorized = handler;
}

export function setInMemoryToken(token: string | null): void {
  memoryToken = token;
}

function getStoredToken(): string | null {
  if (memoryToken !== null) return memoryToken;
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('auth_token');
}

function isTunnelHost(hostname: string): boolean {
  return (
    hostname.includes('ngrok-free.app') ||
    hostname.includes('ngrok.app') ||
    hostname.includes('ngrok.io')
  );
}

function shouldBypassTunnelWarning(): boolean {
  return import.meta.env.DEV;
}

function buildHeaders(
  authenticated: boolean,
  options: { json?: boolean } = {}
): Record<string, string> {
  const { json = true } = options;
  const headers: Record<string, string> = {};

  if (json) {
    headers['Accept'] = 'application/json';
    headers['Content-Type'] = 'application/json';
  }

  if (authenticated) {
    const token = getStoredToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  // Skip ngrok browser warning in Telegram WebView/tunnel environments.
  if (shouldBypassTunnelWarning()) {
    headers['ngrok-skip-browser-warning'] = 'true';
  }

  return headers;
}

function isAbortError(error: unknown): boolean {
  return (
    typeof error === 'object' && error !== null && 'name' in error && error.name === 'AbortError'
  );
}

function getUnexpectedResponseMessage(response: Response, text: string): string {
  const trimmed = text.trim();
  const contentType = response.headers.get('content-type') ?? '';

  if (
    trimmed.startsWith('<!DOCTYPE html') ||
    trimmed.startsWith('<html') ||
    contentType.includes('text/html')
  ) {
    return 'Unexpected HTML response from API';
  }

  if (!response.ok) {
    return response.statusText || `Request failed with status ${response.status}`;
  }

  return 'Unexpected non-JSON response from API';
}

async function parseResponse<T>(response: Response, authenticated = false): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();

  if (!text) {
    return undefined as T;
  }

  let data: unknown;
  try {
    data = JSON.parse(text);
  } catch {
    throw new ApiError(response.status, getUnexpectedResponseMessage(response, text));
  }

  if (!response.ok) {
    // Only trigger global logout for requests that explicitly required auth.
    // An unauthenticated request returning 401 means the resource needs a login,
    // but the current token isn't expired — no reason to kick the user out.
    if (response.status === 401 && authenticated) {
      onUnauthorized?.();
    }
    const message =
      typeof data === 'object' && data !== null && 'message' in data
        ? String((data as Record<string, unknown>).message)
        : response.statusText;
    throw new ApiError(response.status, message);
  }

  return data as T;
}

interface RequestOptions {
  body?: unknown;
  authenticated?: boolean;
}

async function request<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  const { body, authenticated = false } = options;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    const response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers: buildHeaders(authenticated),
      body: body !== undefined ? JSON.stringify(body) : undefined,
      credentials: 'include',
      signal: controller.signal,
    });

    return parseResponse<T>(response, authenticated);
  } catch (error) {
    if (isAbortError(error)) {
      throw new ApiError(408, 'Request timed out. Please try again.');
    }
    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}

async function upload<T>(path: string, formData: FormData, authenticated = true): Promise<T> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    const response = await fetch(`${BASE_URL}${path}`, {
      method: 'POST',
      headers: buildHeaders(authenticated, { json: false }),
      body: formData,
      credentials: 'include',
      signal: controller.signal,
    });

    return parseResponse<T>(response, authenticated);
  } catch (error) {
    if (isAbortError(error)) {
      throw new ApiError(408, 'Upload timed out. Please try again.');
    }
    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}

export const apiClient = {
  get<T>(path: string, authenticated = false): Promise<T> {
    return request<T>('GET', path, { authenticated });
  },

  post<T>(path: string, body: unknown, authenticated = false): Promise<T> {
    return request<T>('POST', path, { body, authenticated });
  },

  put<T>(path: string, body: unknown, authenticated = false): Promise<T> {
    return request<T>('PUT', path, { body, authenticated });
  },

  delete<T = void>(path: string, authenticated = false): Promise<T> {
    return request<T>('DELETE', path, { authenticated });
  },

  upload<T>(path: string, formData: FormData, authenticated = true): Promise<T> {
    return upload<T>(path, formData, authenticated);
  },
};
