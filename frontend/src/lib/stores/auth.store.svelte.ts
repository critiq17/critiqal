import type { User } from '$lib/types';
import { isTelegramMiniApp, cloudStorage } from '$lib/telegram';
import { apiClient } from '$lib/api/client';

// ── user cache helpers ─────────────────────────────────────────────────────
// The user object is cached for optimistic rendering only — the session itself
// lives exclusively in the HttpOnly cookie set by the backend.

function getCachedUser(): User | null {
  try {
    const raw = localStorage.getItem('auth_user');
    return raw ? (JSON.parse(raw) as User) : null;
  } catch {
    return null;
  }
}

function setCachedUser(user: User | null): void {
  try {
    if (user) {
      localStorage.setItem('auth_user', JSON.stringify(user));
    } else {
      localStorage.removeItem('auth_user');
    }
  } catch {
    // ignore — localStorage not available (private mode on some browsers)
  }
}

// ── store factory ─────────────────────────────────────────────────────────

function readCachedUserSync(): User | null {
  // Synchronous best-effort read so the UI can render optimistically without
  // waiting for /api/auth/me. cloudStorage in TMA is async — we only attempt
  // localStorage here; init() will refresh from cloudStorage shortly after.
  if (typeof window === 'undefined') return null;
  try {
    const raw = localStorage.getItem('auth_user');
    return raw ? (JSON.parse(raw) as User) : null;
  } catch {
    return null;
  }
}

function createAuthStore() {
  // Optimistic boot: if we have a cached user, render the app immediately and
  // verify the session in the background. Skip the initial loading flash.
  const cachedUser = readCachedUserSync();
  let state = $state<{ user: User | null; isLoading: boolean }>({
    user: cachedUser,
    isLoading: cachedUser === null,
  });

  // While init() is running we suppress the global 401 handler so a failed
  // /api/auth/me during startup doesn't cause a logout() → init() feedback loop.
  let initializing = false;

  // ── init ──────────────────────────────────────────────────────────────────

  async function init(): Promise<void> {
    initializing = true;
    try {
      if (isTelegramMiniApp()) {
        await _initTMA();
      } else {
        await _initWeb();
      }
    } finally {
      initializing = false;
    }
  }

  async function _initWeb(): Promise<void> {
    const cached = getCachedUser();

    // Optimistic: render with cached user immediately.
    if (cached) state = { user: cached, isLoading: false };

    try {
      const verified = await apiClient.get<User>('/api/auth/me');
      state = { user: verified, isLoading: false };
      setCachedUser(verified);
    } catch (err) {
      const status = (err as { status?: number } | null)?.status;
      if (status === 401 || status === 403) {
        // Real auth failure — session is gone.
        setCachedUser(null);
        state = { user: null, isLoading: false };
      } else if (!cached) {
        // Network/5xx/timeout with no cached user — show logged-out state.
        state = { user: null, isLoading: false };
      }
      // Network/5xx/timeout + cached user → keep optimistic state; a subsequent
      // authenticated request will trigger a real logout via onUnauthorized if needed.
    }
  }

  async function _initTMA(): Promise<void> {
    const userRaw = await cloudStorage.get('auth_user');
    const cached = _tryParse<User>(userRaw);

    if (cached) state = { user: cached, isLoading: false };

    try {
      const verified = await apiClient.get<User>('/api/auth/me');
      state = { user: verified, isLoading: false };
      cloudStorage.set('auth_user', JSON.stringify(verified)).catch(() => {});
    } catch (err) {
      const status = (err as { status?: number } | null)?.status;
      if (status === 401 || status === 403) {
        cloudStorage.remove('auth_user').catch(() => {});
        state = { user: null, isLoading: false };
      } else if (!cached) {
        state = { user: null, isLoading: false };
      }
      // Network/5xx/timeout + cached user → keep optimistic state.
    }
  }

  // ── public API ────────────────────────────────────────────────────────────

  async function login(user: User): Promise<void> {
    if (isTelegramMiniApp()) {
      await cloudStorage.set('auth_user', JSON.stringify(user));
    }
    setCachedUser(user);
    state = { user, isLoading: false };
  }

  async function logout(): Promise<void> {
    try {
      await apiClient.post<void>('/api/auth/logout', {});
    } catch {
      // Idempotent — clear local state regardless of backend response.
    }

    if (isTelegramMiniApp()) {
      await cloudStorage.remove('auth_user').catch(() => {});
    }

    setCachedUser(null);
    state = { ...state, user: null };
  }

  function updateUser(user: User): void {
    setCachedUser(user);
    state = { ...state, user };
  }

  function _tryParse<T>(raw: string | null | undefined): T | null {
    if (!raw) return null;
    try {
      return JSON.parse(raw) as T;
    } catch {
      return null;
    }
  }

  // Expose initializing flag so the global 401 handler can check it.
  return {
    get user() {
      return state.user;
    },
    get isLoading() {
      return state.isLoading;
    },
    get isAuthenticated() {
      return state.user !== null;
    },
    get isInitializing() {
      return initializing;
    },
    init,
    login,
    logout,
    updateUser,
  };
}

export const authStore = createAuthStore();
