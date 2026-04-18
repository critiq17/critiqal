import { describe, it, expect, vi, beforeEach } from 'vitest';

// --- module mocks must be declared before any imports that depend on them ---

const mockIsTelegramMiniApp = vi.fn(() => false);
const mockCloudStorageGet = vi.fn(async (_key: string) => null as string | null);
const mockCloudStorageSet = vi.fn(async (_key: string, _value: string) => {});
const mockCloudStorageRemove = vi.fn(async (_key: string) => {});

vi.mock('$lib/telegram', () => ({
	isTelegramMiniApp: () => mockIsTelegramMiniApp(),
	cloudStorage: {
		get: (key: string) => mockCloudStorageGet(key),
		set: (key: string, value: string) => mockCloudStorageSet(key, value),
		remove: (key: string) => mockCloudStorageRemove(key)
	}
}));

const mockSetInMemoryToken = vi.fn((_token: string | null) => {});
const mockApiClientGet = vi.fn(async () => null);

vi.mock('$lib/api/client', () => ({
	setInMemoryToken: (token: string | null) => mockSetInMemoryToken(token),
	apiClient: {
		get: (_path: string, _auth: boolean) => mockApiClientGet()
	}
}));

// Import after mocks are registered
import { authStore } from './auth.store.svelte';

import type { User } from '$lib/types';

const mockUser: User = {
	id: 1,
	username: 'testuser',
	name: 'Test User',
	bio: null,
	avatarUrl: null,
	createdAt: '2024-01-01T00:00:00Z'
};

const AUTH_TOKEN_KEY = 'auth_token';
const AUTH_USER_KEY = 'auth_user';

beforeEach(async () => {
	// Reset mock behaviour
	mockIsTelegramMiniApp.mockReturnValue(false);
	mockCloudStorageGet.mockResolvedValue(null);
	mockCloudStorageSet.mockResolvedValue(undefined);
	mockCloudStorageRemove.mockResolvedValue(undefined);
	mockSetInMemoryToken.mockReset();
	mockApiClientGet.mockResolvedValue(null);

	// Clear all mocked call records
	vi.clearAllMocks();

	// Re-register default implementations after clearAllMocks
	mockIsTelegramMiniApp.mockReturnValue(false);
	mockCloudStorageGet.mockResolvedValue(null);
	mockCloudStorageSet.mockResolvedValue(undefined);
	mockCloudStorageRemove.mockResolvedValue(undefined);

	// Clear localStorage and reset store to logged-out state
	localStorage.clear();
	await authStore.logout();
});

describe('init()', () => {
	it('reads token and user from localStorage when isTelegramMiniApp returns false', async () => {
		mockIsTelegramMiniApp.mockReturnValue(false);
		localStorage.setItem(AUTH_TOKEN_KEY, 'test-token-123');
		localStorage.setItem(AUTH_USER_KEY, JSON.stringify(mockUser));

		await authStore.init();

		expect(authStore.user).toEqual(mockUser);
		expect(authStore.isAuthenticated).toBe(true);
	});

	it('reads token from cloudStorage.get when isTelegramMiniApp returns true', async () => {
		mockIsTelegramMiniApp.mockReturnValue(true);
		mockCloudStorageGet.mockImplementation(async (key: string) => {
			if (key === AUTH_TOKEN_KEY) return 'cloud-token-abc';
			if (key === AUTH_USER_KEY) return JSON.stringify(mockUser);
			return null;
		});

		await authStore.init();

		expect(mockCloudStorageGet).toHaveBeenCalledWith(AUTH_TOKEN_KEY);
		expect(authStore.isAuthenticated).toBe(true);
	});

	it('leaves user null when no token is found', async () => {
		mockIsTelegramMiniApp.mockReturnValue(false);
		// localStorage is already clear from beforeEach

		await authStore.init();

		expect(authStore.user).toBeNull();
		expect(authStore.isAuthenticated).toBe(false);
	});
});

describe('login()', () => {
	it('writes token to localStorage when not in Telegram', async () => {
		mockIsTelegramMiniApp.mockReturnValue(false);

		await authStore.login(mockUser, 'my-token');

		expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBe('my-token');
	});

	it('calls cloudStorage.set with auth_token when in Telegram', async () => {
		mockIsTelegramMiniApp.mockReturnValue(true);

		await authStore.login(mockUser, 'cloud-token');

		expect(mockCloudStorageSet).toHaveBeenCalledWith(AUTH_TOKEN_KEY, 'cloud-token');
	});

	it('sets user on the store', async () => {
		await authStore.login(mockUser, 'any-token');

		expect(authStore.user).toEqual(mockUser);
	});
});

describe('logout()', () => {
	it('clears auth_token from localStorage', async () => {
		mockIsTelegramMiniApp.mockReturnValue(false);
		await authStore.login(mockUser, 'remove-me');
		expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBe('remove-me');

		await authStore.logout();

		expect(localStorage.getItem(AUTH_TOKEN_KEY)).toBeNull();
	});

	it('clears auth_user from localStorage', async () => {
		mockIsTelegramMiniApp.mockReturnValue(false);
		await authStore.login(mockUser, 'any-token');
		expect(localStorage.getItem(AUTH_USER_KEY)).not.toBeNull();

		await authStore.logout();

		expect(localStorage.getItem(AUTH_USER_KEY)).toBeNull();
	});
});

describe('isAuthenticated', () => {
	it('is true after a successful login', async () => {
		await authStore.login(mockUser, 'token');
		expect(authStore.isAuthenticated).toBe(true);
	});

	it('is false after logout', async () => {
		await authStore.login(mockUser, 'token');
		await authStore.logout();
		expect(authStore.isAuthenticated).toBe(false);
	});

	it('is false on a fresh store with no stored credentials', async () => {
		expect(authStore.isAuthenticated).toBe(false);
	});
});
