import type { User } from '$lib/types';
import { isTelegramMiniApp, cloudStorage } from '$lib/telegram';
import { apiClient } from '$lib/api/client';

const AUTH_USER_KEY = 'auth_user';
const LEGACY_TOKEN_KEY = 'auth_token';

// ── user cache helpers ─────────────────────────────────────────────────────
// The user object is cached for optimistic rendering only — the session itself
// lives exclusively in the HttpOnly cookie set by the backend.

function isUser(value: unknown): value is User {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as Record<string, unknown>).id === 'string' &&
    typeof (value as Record<string, unknown>).username === 'string'
  );
}

function parseCachedUser(raw: string | null): User | null {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as unknown;
    return isUser(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

function getCachedUser(): User | null {
  try {
    return parseCachedUser(localStorage.getItem(AUTH_USER_KEY));
  } catch {
    return null;
  }
}

function setCachedUser(user: User | null): void {
  try {
    if (user) {
      localStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(AUTH_USER_KEY);
    }
  } catch {
    // ignore — localStorage not available (private mode on some browsers)
  }
}

function clearLegacyLocalAuth(): void {
  try {
    localStorage.removeItem(LEGACY_TOKEN_KEY);
  } catch {
    // ignore
  }
}

// ── store factory ─────────────────────────────────────────────────────────

function readCachedUserSync(): User | null {
  // Synchronous best-effort read so the UI can render optimistically without
  // waiting for /api/auth/me. cloudStorage in TMA is async — we only attempt
  // localStorage here; init() will refresh from cloudStorage shortly after.
  if (typeof window === 'undefined') return null;
  try {
    const parsed = parseCachedUser(localStorage.getItem(AUTH_USER_KEY));
    if (!parsed) {
      localStorage.removeItem(AUTH_USER_KEY);
    }
    clearLegacyLocalAuth();
    return parsed;
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
    clearLegacyLocalAuth();
    const cached = getCachedUser();

    // Optimistic: render with cached user immediately.
    if (cached) state = { user: cached, isLoading: false };

    try {
      const verified = await apiClient.get<User>('/api/auth/me');
      if (!isUser(verified)) {
        setCachedUser(null);
        state = { user: null, isLoading: false };
        return;
      }
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
    clearLegacyLocalAuth();
    cloudStorage.remove(LEGACY_TOKEN_KEY).catch(() => {});

    const userRaw = await cloudStorage.get(AUTH_USER_KEY);
    const cached = _tryParseUser(userRaw);

    if (cached) state = { user: cached, isLoading: false };
    else if (userRaw) cloudStorage.remove(AUTH_USER_KEY).catch(() => {});

    try {
      const verified = await apiClient.get<User>('/api/auth/me');
      if (!isUser(verified)) {
        cloudStorage.remove(AUTH_USER_KEY).catch(() => {});
        state = { user: null, isLoading: false };
        return;
      }
      state = { user: verified, isLoading: false };
      cloudStorage.set(AUTH_USER_KEY, JSON.stringify(verified)).catch(() => {});
    } catch (err) {
      const status = (err as { status?: number } | null)?.status;
      if (status === 401 || status === 403) {
        cloudStorage.remove(AUTH_USER_KEY).catch(() => {});
        state = { user: null, isLoading: false };
      } else if (!cached) {
        state = { user: null, isLoading: false };
      }
      // Network/5xx/timeout + cached user → keep optimistic state.
    }
  }

  // ── public API ────────────────────────────────────────────────────────────

  async function login(user: User): Promise<void> {
    clearLegacyLocalAuth();
    if (isTelegramMiniApp()) {
      await cloudStorage.remove(LEGACY_TOKEN_KEY).catch(() => {});
      await cloudStorage.set(AUTH_USER_KEY, JSON.stringify(user));
    }
    setCachedUser(user);
    state = { user, isLoading: false };
  }

  async function logout(): Promise<void> {
    try {
      await apiClient.post<void>('/api/auth/logout', {}, { skipUnauthorizedHandler: true });
    } catch {
      // Idempotent — clear local state regardless of backend response.
    }

    if (isTelegramMiniApp()) {
      await cloudStorage.remove(AUTH_USER_KEY).catch(() => {});
      await cloudStorage.remove(LEGACY_TOKEN_KEY).catch(() => {});
    }

    clearLegacyLocalAuth();
    setCachedUser(null);
    state = { user: null, isLoading: false };
  }

  function updateUser(user: User): void {
    setCachedUser(user);
    state = { ...state, user };
  }

  async function refresh(): Promise<void> {
    try {
      const user = await apiClient.get<User>('/api/auth/me');
      if (isUser(user)) updateUser(user);
    } catch {
      /* non-fatal — keep current state */
    }
  }

  function _tryParseUser(raw: string | null | undefined): User | null {
    return parseCachedUser(raw ?? null);
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
    refresh,
  };
}

export const authStore = createAuthStore();
