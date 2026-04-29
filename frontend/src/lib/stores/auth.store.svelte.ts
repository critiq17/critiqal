import type { User } from '$lib/types';
import { isTelegramMiniApp, cloudStorage } from '$lib/telegram';
import { apiClient, setInMemoryToken } from '$lib/api/client';

// ── token persistence helpers ──────────────────────────────────────────────
// TMA:  token lives in Telegram CloudStorage (sandboxed, persistent across restarts)
// Web:  token lives in localStorage so the session survives tab close and browser
//       restart. TEMP: this is a stop-gap until HttpOnly cookie sessions land
//       (see Linear: SESSION-1/2/3). XSS risk is acknowledged trade-off.

function getWebToken(): string | null {
  try {
    return localStorage.getItem('auth_token');
  } catch {
    return null;
  }
}

function setWebToken(token: string | null): void {
  try {
    if (token) {
      localStorage.setItem('auth_token', token);
    } else {
      localStorage.removeItem('auth_token');
    }
  } catch {
    // localStorage not available (private mode on some browsers)
  }
}

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
    const raw = localStorage.getItem('auth_user');
    return raw ? (JSON.parse(raw) as User) : null;
  } catch {
    return null;
  }
}

function createAuthStore() {
  // Optimistic boot: if we have a cached user, render the app immediately and
  // verify the token in the background. Skip the initial loading flash entirely.
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

  async function _initTMA(): Promise<void> {
    // Fetch token and user cache in parallel to save one IPC round-trip.
    const [token, userRaw] = await Promise.all([
      cloudStorage.get('auth_token'),
      cloudStorage.get('auth_user'),
    ]);

    if (!token) {
      state = { user: null, isLoading: false };
      return;
    }

    setInMemoryToken(token);

    // Show cached user immediately for zero-wait perceived auth.
    const cached = _tryParse<User>(userRaw);
    if (cached) {
      state = { user: cached, isLoading: false };
    }

    // Verify token with backend. Use 'true' so the Bearer header is sent,
    // but we handle the failure ourselves — no global logout during init.
    try {
      const verified = await apiClient.get<User>('/api/auth/me', true);
      state = { user: verified, isLoading: false };
      // Keep cloudStorage user cache in sync (fire-and-forget).
      cloudStorage.set('auth_user', JSON.stringify(verified)).catch(() => {});
    } catch {
      // Token is expired or invalid. Clear everything and show login.
      setInMemoryToken(null);
      await Promise.allSettled([
        cloudStorage.remove('auth_token'),
        cloudStorage.remove('auth_user'),
      ]);
      setCachedUser(null);
      state = { user: null, isLoading: false };
    }
  }

  async function _initWeb(): Promise<void> {
    const token = getWebToken();
    const cached = getCachedUser();

    // Optimistic: render with cached user immediately, never show loading screen
    // when we already know who the user is.
    if (cached) {
      state = { user: cached, isLoading: false };
    }

    if (token) {
      setInMemoryToken(token);
      try {
        const verified = await apiClient.get<User>('/api/auth/me', true);
        if (
          verified.id !== cached?.id ||
          verified.avatarUrl !== cached?.avatarUrl ||
          verified.name !== cached?.name ||
          verified.bio !== cached?.bio
        ) {
          state = { user: verified, isLoading: false };
          setCachedUser(verified);
        } else if (state.isLoading) {
          state = { user: verified, isLoading: false };
        }
      } catch (err) {
        // Only clear the session on a real auth failure (401/403). Network blips,
        // 5xx, timeouts must NOT log the user out — keep the cached user and let
        // a subsequent authenticated request trigger a real logout if needed.
        const status = (err as { status?: number } | null)?.status;
        if (status === 401 || status === 403) {
          setInMemoryToken(null);
          setWebToken(null);
          setCachedUser(null);
          state = { user: null, isLoading: false };
        } else if (cached) {
          state = { user: cached, isLoading: false };
        } else {
          state = { user: null, isLoading: false };
        }
      }
      return;
    }

    // No token — try cookie session (best-effort, don't show loading flash if we had cache).
    try {
      const verified = await apiClient.get<User>('/api/auth/me', false);
      state = { user: verified, isLoading: false };
      setCachedUser(verified);
    } catch {
      // Only clear if there was no cached user; otherwise keep optimistic state
      // and let a real 401 from a subsequent request trigger logout.
      if (!cached) {
        setCachedUser(null);
        state = { user: null, isLoading: false };
      }
    }
  }

  // ── public API ────────────────────────────────────────────────────────────

  async function login(user: User, token: string): Promise<void> {
    setInMemoryToken(token);

    if (isTelegramMiniApp()) {
      // Parallel writes to CloudStorage.
      await Promise.all([
        cloudStorage.set('auth_token', token),
        cloudStorage.set('auth_user', JSON.stringify(user)),
      ]);
    } else {
      setWebToken(token);
    }

    setCachedUser(user);
    state = { user, isLoading: false };
  }

  async function logout(): Promise<void> {
    setInMemoryToken(null);

    if (isTelegramMiniApp()) {
      await Promise.allSettled([
        cloudStorage.remove('auth_token'),
        cloudStorage.remove('auth_user'),
      ]);
    } else {
      setWebToken(null);
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
