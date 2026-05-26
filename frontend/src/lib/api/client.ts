import { ApiError } from '$lib/types';
import { getDeviceId } from '$lib/deviceId';

// Empty string = relative URLs (proxied via Vite dev server or same-origin in prod)
const BASE_URL: string = import.meta.env.VITE_API_URL ?? '';
const REQUEST_TIMEOUT_MS = 15000;

let onUnauthorized: (() => void) | null = null;
let onEmailVerificationRequired: (() => void) | null = null;

export function registerUnauthorizedHandler(handler: () => void): void {
  onUnauthorized = handler;
}

export function registerEmailVerificationRequiredHandler(handler: () => void): void {
  onEmailVerificationRequired = handler;
}

function isEmailVerificationError(message: string): boolean {
  return message.toLowerCase().includes('email verification');
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

function buildHeaders(options: { json?: boolean } = {}): Record<string, string> {
  const { json = true } = options;
  const headers: Record<string, string> = {};

  if (json) {
    headers['Accept'] = 'application/json';
    headers['Content-Type'] = 'application/json';
  }

  // Skip ngrok browser warning in Telegram WebView/tunnel environments.
  if (shouldBypassTunnelWarning()) {
    headers['ngrok-skip-browser-warning'] = 'true';
  }

  headers['X-Device-Id'] = getDeviceId();

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

async function parseResponse<T>(
  response: Response,
  options: { notifyUnauthorized?: boolean } = {}
): Promise<T> {
  const { notifyUnauthorized = true } = options;

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();

  if (!response.ok && response.status === 401 && notifyUnauthorized) {
    onUnauthorized?.();
  }

  if (!text) {
    if (!response.ok) {
      throw new ApiError(
        response.status,
        response.statusText || `Request failed with status ${response.status}`
      );
    }
    return undefined as T;
  }

  let data: unknown;
  try {
    data = JSON.parse(text);
  } catch {
    throw new ApiError(response.status, getUnexpectedResponseMessage(response, text));
  }

  if (!response.ok) {
    const message =
      typeof data === 'object' && data !== null && 'message' in data
        ? String((data as Record<string, unknown>).message)
        : response.statusText;
    if (response.status === 403 && isEmailVerificationError(message)) {
      onEmailVerificationRequired?.();
    }
    throw new ApiError(response.status, message);
  }

  return data as T;
}

interface RequestOptions {
  body?: unknown;
  skipUnauthorizedHandler?: boolean;
}

async function request<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
  const { body, skipUnauthorizedHandler = false } = options;

  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    const response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers: buildHeaders(),
      body: body !== undefined ? JSON.stringify(body) : undefined,
      credentials: 'include',
      signal: controller.signal,
    });

    return parseResponse<T>(response, {
      notifyUnauthorized: !skipUnauthorizedHandler,
    });
  } catch (error) {
    if (isAbortError(error)) {
      throw new ApiError(408, 'Request timed out. Please try again.');
    }
    throw error;
  } finally {
    clearTimeout(timeoutId);
  }
}

async function upload<T>(path: string, formData: FormData): Promise<T> {
  const controller = new AbortController();
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

  try {
    const response = await fetch(`${BASE_URL}${path}`, {
      method: 'POST',
      headers: buildHeaders({ json: false }),
      body: formData,
      credentials: 'include',
      signal: controller.signal,
    });

    return parseResponse<T>(response);
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
  get<T>(path: string): Promise<T> {
    return request<T>('GET', path);
  },

  post<T>(
    path: string,
    body: unknown,
    options: { skipUnauthorizedHandler?: boolean } = {}
  ): Promise<T> {
    return request<T>('POST', path, { body, ...options });
  },

  put<T>(path: string, body: unknown): Promise<T> {
    return request<T>('PUT', path, { body });
  },

  delete<T = void>(path: string): Promise<T> {
    return request<T>('DELETE', path);
  },

  deleteWithBody<T = void>(path: string, body: unknown): Promise<T> {
    return request<T>('DELETE', path, { body });
  },

  upload<T>(path: string, formData: FormData): Promise<T> {
    return upload<T>(path, formData);
  },
};
