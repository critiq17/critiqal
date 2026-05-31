import { ApiError } from '$lib/types';

const REQUEST_TIMEOUT_MS = 15000;

function extractErrorMessage(data: unknown, fallback: string): string {
	if (typeof data === 'object' && data !== null) {
		const record = data as Record<string, unknown>;
		if (typeof record.error === 'string' && record.error.trim() !== '') {
			return record.error;
		}
		if (typeof record.message === 'string' && record.message.trim() !== '') {
			return record.message;
		}
	}
	return fallback;
}

async function parseResponse<T>(response: Response): Promise<T> {
	if (response.status === 204) {
		return undefined as T;
	}

	const text = await response.text();

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
		throw new ApiError(response.status, 'Unexpected non-JSON response from API');
	}

	if (!response.ok) {
		const message = extractErrorMessage(
			data,
			response.statusText || `Request failed with status ${response.status}`
		);
		throw new ApiError(response.status, message);
	}

	return data as T;
}

interface PostOptions {
	skipUnauthorizedHandler?: boolean;
}

async function request<T>(method: string, path: string, body?: unknown): Promise<T> {
	const controller = new AbortController();
	const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT_MS);

	try {
		const response = await fetch(path, {
			method,
			headers: {
				Accept: 'application/json',
				'Content-Type': 'application/json',
			},
			body: body !== undefined ? JSON.stringify(body) : undefined,
			credentials: 'include',
			signal: controller.signal,
		});

		return parseResponse<T>(response);
	} catch (error) {
		if (
			typeof error === 'object' &&
			error !== null &&
			'name' in error &&
			error.name === 'AbortError'
		) {
			throw new ApiError(408, 'Request timed out. Please try again.');
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

	// skipUnauthorizedHandler is accepted for API compatibility but unused —
	// the admin client has no global logout handler to skip.
	post<T>(path: string, body: unknown, _options: PostOptions = {}): Promise<T> {
		return request<T>('POST', path, body);
	},

	delete<T = void>(path: string): Promise<T> {
		return request<T>('DELETE', path);
	},
};
