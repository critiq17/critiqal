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
    remove: (key: string) => mockCloudStorageRemove(key),
  },
}));

const mockApiClientGet = vi.fn(async (_path: string): Promise<unknown> => null);
const mockApiClientPost = vi.fn(
  async (
    _path: string,
    _body: unknown,
    _options?: { skipUnauthorizedHandler?: boolean }
  ): Promise<unknown> => undefined
);

vi.mock('$lib/api/client', () => ({
  apiClient: {
    get: (path: string) => mockApiClientGet(path),
    post: (
      path: string,
      body: unknown,
      options?: { skipUnauthorizedHandler?: boolean }
    ) => mockApiClientPost(path, body, options),
  },
}));

// Import after mocks are registered
import { authStore } from './auth.store.svelte';
import type { User } from '$lib/types';

const mockUser: User = {
  id: '1',
  username: 'testuser',
  name: 'Test User',
  bio: null,
  avatarUrl: null,
  createdAt: '2024-01-01T00:00:00Z',
};

const updatedUser: User = {
  ...mockUser,
  name: 'Updated Name',
  bio: 'A bio',
};

const AUTH_USER_KEY = 'auth_user';

beforeEach(async () => {
  // Reset mock behaviour
  mockIsTelegramMiniApp.mockReturnValue(false);
  mockCloudStorageGet.mockResolvedValue(null);
  mockCloudStorageSet.mockResolvedValue(undefined);
  mockCloudStorageRemove.mockResolvedValue(undefined);
  mockApiClientGet.mockResolvedValue(null);
  mockApiClientPost.mockResolvedValue(undefined);

  vi.clearAllMocks();

  // Re-register defaults after clearAllMocks
  mockIsTelegramMiniApp.mockReturnValue(false);
  mockCloudStorageGet.mockResolvedValue(null);
  mockCloudStorageSet.mockResolvedValue(undefined);
  mockCloudStorageRemove.mockResolvedValue(undefined);
  mockApiClientGet.mockResolvedValue(null);
  mockApiClientPost.mockResolvedValue(undefined);

  localStorage.clear();
  // Reset store to logged-out state without hitting backend
  await authStore.logout();
});

// ── init() — web ──────────────────────────────────────────────────────────

describe('init() web', () => {
  it('renders cached user immediately, then updates from GET /api/auth/me', async () => {
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(mockUser));
    mockApiClientGet.mockResolvedValue(updatedUser);

    await authStore.init();

    expect(authStore.user).toEqual(updatedUser);
    expect(authStore.isAuthenticated).toBe(true);
  });

  it('clears cache and sets user to null on 401 from /api/auth/me', async () => {
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(mockUser));
    const err = Object.assign(new Error('Unauthorized'), { status: 401 });
    mockApiClientGet.mockRejectedValue(err);

    await authStore.init();

    expect(authStore.user).toBeNull();
    expect(authStore.isAuthenticated).toBe(false);
    expect(localStorage.getItem(AUTH_USER_KEY)).toBeNull();
  });

  it('keeps cached user on network error (non-401)', async () => {
    localStorage.setItem(AUTH_USER_KEY, JSON.stringify(mockUser));
    const err = Object.assign(new Error('Network error'), { status: undefined });
    mockApiClientGet.mockRejectedValue(err);

    await authStore.init();

    // Optimistic cached user must be preserved — do not log out on network blips.
    expect(authStore.user).toEqual(mockUser);
    expect(authStore.isAuthenticated).toBe(true);
  });

  it('sets user to null when no cache and /api/auth/me fails with network error', async () => {
    // No cached user, network fails
    const err = new Error('Network error');
    mockApiClientGet.mockRejectedValue(err);

    await authStore.init();

    expect(authStore.user).toBeNull();
    expect(authStore.isAuthenticated).toBe(false);
  });

  it('leaves user null when no session and no cache', async () => {
    const err = Object.assign(new Error('Unauthorized'), { status: 401 });
    mockApiClientGet.mockRejectedValue(err);

    await authStore.init();

    expect(authStore.user).toBeNull();
    expect(authStore.isAuthenticated).toBe(false);
  });
});

// ── login() ───────────────────────────────────────────────────────────────

describe('login()', () => {
  it('sets state.user and persists to localStorage auth_user', async () => {
    await authStore.login(mockUser);

    expect(authStore.user).toEqual(mockUser);
    expect(localStorage.getItem(AUTH_USER_KEY)).toBe(JSON.stringify(mockUser));
  });

  it('does not write auth_token to localStorage', async () => {
    await authStore.login(mockUser);

    expect(localStorage.getItem('auth_token')).toBeNull();
  });

  it('saves user to cloudStorage when in Telegram', async () => {
    mockIsTelegramMiniApp.mockReturnValue(true);

    await authStore.login(mockUser);

    expect(mockCloudStorageSet).toHaveBeenCalledWith(AUTH_USER_KEY, JSON.stringify(mockUser));
    expect(authStore.user).toEqual(mockUser);
  });

  it('does not call cloudStorage.set with auth_token in Telegram', async () => {
    mockIsTelegramMiniApp.mockReturnValue(true);

    await authStore.login(mockUser);

    const tokenCalls = mockCloudStorageSet.mock.calls.filter(([key]) => key === 'auth_token');
    expect(tokenCalls).toHaveLength(0);
  });
});

// ── logout() ─────────────────────────────────────────────────────────────

describe('logout()', () => {
  it('calls POST /api/auth/logout and clears user + cache', async () => {
    await authStore.login(mockUser);
    expect(authStore.user).toEqual(mockUser);

    await authStore.logout();

    expect(mockApiClientPost).toHaveBeenLastCalledWith('/api/auth/logout', {}, {
      skipUnauthorizedHandler: true,
    });
    expect(authStore.user).toBeNull();
    expect(authStore.isAuthenticated).toBe(false);
    expect(localStorage.getItem(AUTH_USER_KEY)).toBeNull();
  });

  it('clears local state even when backend returns 500 (idempotent)', async () => {
    await authStore.login(mockUser);
    mockApiClientPost.mockRejectedValue(Object.assign(new Error('Server error'), { status: 500 }));

    await authStore.logout();

    expect(authStore.user).toBeNull();
    expect(localStorage.getItem(AUTH_USER_KEY)).toBeNull();
  });

  it('clears cloudStorage auth_user when in Telegram', async () => {
    mockIsTelegramMiniApp.mockReturnValue(true);
    await authStore.login(mockUser);

    await authStore.logout();

    expect(mockCloudStorageRemove).toHaveBeenCalledWith(AUTH_USER_KEY);
    expect(authStore.user).toBeNull();
  });

  it('clears legacy auth_token from localStorage during init', async () => {
    localStorage.setItem('auth_token', 'legacy-jwt');
    const err = Object.assign(new Error('Unauthorized'), { status: 401 });
    mockApiClientGet.mockRejectedValue(err);

    await authStore.init();

    expect(localStorage.getItem('auth_token')).toBeNull();
    expect(authStore.isAuthenticated).toBe(false);
  });
});

// ── isAuthenticated ───────────────────────────────────────────────────────

describe('isAuthenticated', () => {
  it('is true after a successful login', async () => {
    await authStore.login(mockUser);
    expect(authStore.isAuthenticated).toBe(true);
  });

  it('is false after logout', async () => {
    await authStore.login(mockUser);
    await authStore.logout();
    expect(authStore.isAuthenticated).toBe(false);
  });

  it('is false on a fresh store with no stored credentials', () => {
    expect(authStore.isAuthenticated).toBe(false);
  });
});

// ── init() — TMA ──────────────────────────────────────────────────────────

describe('init() TMA', () => {
  it('reads auth_user from cloudStorage and verifies with /api/auth/me', async () => {
    mockIsTelegramMiniApp.mockReturnValue(true);
    mockCloudStorageGet.mockImplementation(async (key: string) => {
      if (key === AUTH_USER_KEY) return JSON.stringify(mockUser);
      return null;
    });
    mockApiClientGet.mockResolvedValue(updatedUser);

    await authStore.init();

    expect(mockCloudStorageGet).toHaveBeenCalledWith(AUTH_USER_KEY);
    expect(authStore.user).toEqual(updatedUser);
    expect(authStore.isAuthenticated).toBe(true);
  });

  it('clears cloudStorage and sets user null on 401 from /api/auth/me', async () => {
    mockIsTelegramMiniApp.mockReturnValue(true);
    mockCloudStorageGet.mockImplementation(async (key: string) => {
      if (key === AUTH_USER_KEY) return JSON.stringify(mockUser);
      return null;
    });
    const err = Object.assign(new Error('Unauthorized'), { status: 401 });
    mockApiClientGet.mockRejectedValue(err);

    await authStore.init();

    expect(mockCloudStorageRemove).toHaveBeenCalledWith(AUTH_USER_KEY);
    expect(authStore.user).toBeNull();
  });

  it('sets user null when cloudStorage is empty and /api/auth/me fails', async () => {
    mockIsTelegramMiniApp.mockReturnValue(true);
    const err = Object.assign(new Error('Unauthorized'), { status: 401 });
    mockApiClientGet.mockRejectedValue(err);

    await authStore.init();

    expect(authStore.user).toBeNull();
  });

  it('treats an empty successful auth payload as logged out instead of authenticated', async () => {
    mockIsTelegramMiniApp.mockReturnValue(true);
    mockCloudStorageGet.mockImplementation(async (key: string) => {
      if (key === AUTH_USER_KEY) return JSON.stringify(mockUser);
      return null;
    });
    mockApiClientGet.mockResolvedValue(undefined);

    await authStore.init();

    expect(authStore.user).toBeNull();
    expect(authStore.isAuthenticated).toBe(false);
    expect(mockCloudStorageRemove).toHaveBeenCalledWith(AUTH_USER_KEY);
  });
});
