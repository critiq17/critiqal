// Holds the pending email-verification state across page reloads and Telegram
// mini-app minimize/restore. The TTL mirrors the backend code expiry (15 min).
// We store in sessionStorage so a real "close" of the tab/mini-app discards it,
// while a simple minimize-to-mail-app round-trip preserves the form.

const STORAGE_KEY = 'verify_email_state_v1';
const TTL_MS = 15 * 60 * 1000;

export interface VerifyEmailState {
  email: string;
  sentAt: number;
  expiresAt: number;
}

function readStorage(): VerifyEmailState | null {
  if (typeof sessionStorage === 'undefined') return null;
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<VerifyEmailState>;
    if (
      typeof parsed?.email !== 'string' ||
      typeof parsed?.sentAt !== 'number' ||
      typeof parsed?.expiresAt !== 'number'
    ) {
      return null;
    }
    if (parsed.expiresAt < Date.now()) {
      sessionStorage.removeItem(STORAGE_KEY);
      return null;
    }
    return parsed as VerifyEmailState;
  } catch {
    return null;
  }
}

function writeStorage(state: VerifyEmailState | null): void {
  if (typeof sessionStorage === 'undefined') return;
  try {
    if (state === null) sessionStorage.removeItem(STORAGE_KEY);
    else sessionStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch {
    // ignore — sessionStorage unavailable
  }
}

function createStore() {
  let state = $state<VerifyEmailState | null>(readStorage());

  function start(email: string): void {
    const now = Date.now();
    const next: VerifyEmailState = { email, sentAt: now, expiresAt: now + TTL_MS };
    state = next;
    writeStorage(next);
  }

  function refresh(): void {
    if (!state) return;
    const now = Date.now();
    const next: VerifyEmailState = { ...state, sentAt: now, expiresAt: now + TTL_MS };
    state = next;
    writeStorage(next);
  }

  function clear(): void {
    state = null;
    writeStorage(null);
  }

  function reload(): void {
    state = readStorage();
  }

  return {
    get current() {
      return state;
    },
    get isPending() {
      return state !== null;
    },
    start,
    refresh,
    clear,
    reload,
  };
}

export const verifyEmailStore = createStore();
