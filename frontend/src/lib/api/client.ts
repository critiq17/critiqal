import { ApiError } from '$lib/types';

// Empty string = relative URLs (proxied via Vite dev server or same-origin in prod)
const BASE_URL: string = import.meta.env.VITE_API_URL ?? '';

function getStoredToken(): string | null {
	if (typeof window === 'undefined') return null;
	return localStorage.getItem('auth_token');
}

function buildHeaders(authenticated: boolean): Record<string, string> {
	const headers: Record<string, string> = {
		'Content-Type': 'application/json'
	};

	if (authenticated) {
		const token = getStoredToken();
		if (token) {
			headers['Authorization'] = `Bearer ${token}`;
		}
	}

	return headers;
}

async function parseResponse<T>(response: Response): Promise<T> {
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
		throw new ApiError(response.status, text);
	}

	if (!response.ok) {
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

	const response = await fetch(`${BASE_URL}${path}`, {
		method,
		headers: buildHeaders(authenticated),
		body: body !== undefined ? JSON.stringify(body) : undefined,
		credentials: 'include' // cookie-ready for future auth migration
	});

	return parseResponse<T>(response);
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
	}
};
