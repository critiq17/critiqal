interface SafeAreaInset {
  readonly top: number;
  readonly bottom: number;
  readonly left: number;
  readonly right: number;
}

interface TelegramWebApp {
  ready(): void;
  expand(): void;
  requestFullscreen?(): void;
  exitFullscreen?(): void;
  setHeaderColor(color: string): void;
  isFullscreen: boolean;
  isExpanded: boolean;
  platform: string;
  initData: string;
  initDataUnsafe?: {
    start_param?: string;
    [key: string]: unknown;
  };
  viewportHeight: number;
  viewportStableHeight?: number;
  safeAreaInset: SafeAreaInset;
  contentSafeAreaInset: SafeAreaInset;
  onEvent(event: string, handler: (params?: unknown) => void): void;
  offEvent(event: string, handler: (params?: unknown) => void): void;
  colorScheme: 'light' | 'dark';
  backgroundColor: string;
  themeParams: {
    text_color: string;
    hint_color: string;
    button_color: string;
    button_text_color: string;
    bg_color?: string;
    secondary_bg_color?: string;
  };
  openLink(url: string): void;
  openTelegramLink(url: string): void;
  disableVerticalSwipes?(): void;
  isVerticalSwipesEnabled?: boolean;
  BackButton: {
    readonly isVisible: boolean;
    show(): void;
    hide(): void;
    onClick(fn: () => void): void;
    offClick(fn: () => void): void;
  };
  MainButton: {
    text: string;
    isVisible: boolean;
    isActive: boolean;
    isProgressVisible: boolean;
    setText(text: string): void;
    show(): void;
    hide(): void;
    enable(): void;
    disable(): void;
    showProgress(leaveActive: boolean): void;
    hideProgress(): void;
    onClick(fn: () => void): void;
    offClick(fn: () => void): void;
  };
  HapticFeedback: {
    impactOccurred(style: 'light' | 'medium' | 'heavy' | 'rigid' | 'soft'): void;
    notificationOccurred(type: 'error' | 'success' | 'warning'): void;
  };
  CloudStorage: {
    setItem(key: string, value: string, callback?: (err: unknown, stored: boolean) => void): void;
    getItem(key: string, callback: (err: unknown, value: string) => void): void;
    removeItem(key: string, callback?: (err: unknown, removed: boolean) => void): void;
  };
}

declare global {
  interface Window {
    Telegram?: {
      WebApp: TelegramWebApp;
    };
  }
}

export function isTelegramMiniApp(): boolean {
  if (typeof window === 'undefined') return false;
  // Explicit launch param (web TMA / deep links) — always a mini app.
  if (window.location.hash.includes('tgWebApp')) return true;
  const wa = window.Telegram?.WebApp;
  if (!wa) return false;
  // telegram-web-app.js now loads in EVERY browser and always creates a
  // WebApp object — so its mere presence no longer means "in Telegram".
  // A real client provides initData and a concrete platform; the script in
  // a plain desktop/mobile browser reports platform 'unknown' + empty
  // initData. Distinguishing these is what keeps the normal desktop site.
  if (wa.initData && wa.initData.length > 0) return true;
  if (wa.platform && wa.platform !== 'unknown') return true;
  return false;
}

export function getTelegramWebApp(): TelegramWebApp | null {
  if (!isTelegramMiniApp()) return null;
  return window.Telegram?.WebApp ?? null;
}

// The `startapp` payload a deep link launched the mini app with (null on a
// normal open or outside Telegram). telegram-web-app.js parses it out of the
// launch hash into initDataUnsafe for us.
export function getStartParam(): string | null {
  const param = getTelegramWebApp()?.initDataUnsafe?.start_param;
  return typeof param === 'string' && param.length > 0 ? param : null;
}

function applyThemeVars(tg: TelegramWebApp): void {
  const root = document.documentElement;
  root.style.setProperty('--tg-bg', tg.backgroundColor);
  root.style.setProperty('--tg-text', tg.themeParams.text_color);
  root.style.setProperty('--tg-hint', tg.themeParams.hint_color);
  root.style.setProperty('--tg-accent', tg.themeParams.button_color);
  root.style.setProperty('--tg-btn-text', tg.themeParams.button_text_color);

  // Drive the app's semantic theme tokens off Telegram's colorScheme. The
  // [data-theme="light"] rules in +layout.svelte override --color-*, --glass-*,
  // --surface-tint-*, --text-* etc. for the entire mini-app subtree. Removing
  // the attribute restores the dark defaults.
  if (tg.colorScheme === 'light') {
    root.setAttribute('data-theme', 'light');
  } else {
    root.removeAttribute('data-theme');
  }

  // Keep header color in sync with app background on theme changes.
  // Using backgroundColor (not themeParams.bg_color) ensures it matches --tg-bg exactly.
  tg.setHeaderColor(tg.backgroundColor || (tg.colorScheme === 'light' ? '#ffffff' : '#0f0f0f'));
}

function applyViewportVars(tg: TelegramWebApp): void {
  const root = document.documentElement;
  root.style.setProperty('--tg-viewport-height', (tg.viewportHeight || window.innerHeight) + 'px');

  // contentSafeAreaInset is the space taken by transparent Telegram UI in fullscreen mode.
  // Telegram SDK sets --tg-content-safe-area-inset-* CSS vars automatically (Bot API 8.0+),
  // but we also set our own vars as fallback for older clients.
  const content = tg.contentSafeAreaInset;
  if (content && content.top > 0) {
    root.style.setProperty('--tg-content-top', content.top + 'px');
  }
  if (content && content.bottom > 0) {
    root.style.setProperty('--tg-content-bottom', content.bottom + 'px');
  }
}

// Full re-assert: keep the app expanded and the vertical-swipe (drag to
// minimize/close) gesture disabled. Used on init and on every Telegram
// viewport transition, which is when Telegram silently resets both.
export function lockTelegramViewport(): void {
  const tg = getTelegramWebApp();
  if (!tg) return;
  tg.expand();
  tg.disableVerticalSwipes?.();
}

export function initTelegram(): void {
  const tg = getTelegramWebApp();
  if (!tg) return;

  // app.html already calls ready(), expand(), setHeaderColor(), and requestFullscreen()
  // before SvelteKit boots. Re-apply vars here in case SvelteKit hydration reset them,
  // and register Svelte-managed event listeners.
  applyThemeVars(tg);
  applyViewportVars(tg);

  const root = document.documentElement;
  root.style.setProperty('--bottom-nav-height', '72px');
  root.style.setProperty('--safe-bottom', 'env(safe-area-inset-bottom, 0px)');
  root.style.setProperty('--safe-top', 'env(safe-area-inset-top, 0px)');
  root.style.setProperty(
    '--content-bottom-padding',
    'calc(var(--bottom-nav-height) + var(--safe-bottom) + 16px)'
  );

  // Re-assert the expanded / vertical-swipe lock on every Telegram viewport
  // transition so scrolling or swiping inside the app can never minimize/close
  // the mini-app — it can only be closed via the native header button.
  const lockViewport = lockTelegramViewport;

  tg.onEvent('viewportChanged', () => {
    applyViewportVars(tg);
    lockViewport();
  });
  tg.onEvent('themeChanged', () => applyThemeVars(tg));
  tg.onEvent('fullscreenChanged', () => {
    applyViewportVars(tg);
    lockViewport();
  });
  // safeAreaChanged / contentSafeAreaChanged can fire after fullscreenChanged
  // with the actual non-zero inset values — must listen to both.
  tg.onEvent('safeAreaChanged', () => applyViewportVars(tg));
  tg.onEvent('contentSafeAreaChanged', () => applyViewportVars(tg));
  // Fullscreen not supported — fall back to expand() so content fills available height.
  tg.onEvent('fullscreenFailed', () => tg.expand());

  // Global swipe guard — the actual fix for "app still closes on some pages".
  //
  // Telegram resets isVerticalSwipesEnabled on tab switches, route changes and
  // DOM-only overlays opening — none of which fire a Telegram event, so the
  // listeners above never re-assert there. Instead of wiring every screen
  // (patchwork, easy to forget), re-assert once at the start of every touch
  // gesture, app-wide: one passive capture listener on the document. It runs
  // synchronously on touchstart — before Telegram interprets the drag as a
  // close — so the lock is guaranteed active for the gesture about to happen.
  //
  // Cost is negligible: fires once per gesture start (not per move), only
  // calls the SDK when the gesture was actually re-enabled, and never touches
  // the viewport (no expand()), so it can't cause scroll/layout jank. Pages
  // still scroll and rubber-band natively — only the close gesture is killed.
  document.addEventListener(
    'touchstart',
    () => {
      if (tg.isVerticalSwipesEnabled !== false) tg.disableVerticalSwipes?.();
    },
    { passive: true, capture: true }
  );

  lockViewport();
}

export const cloudStorage = {
  get(key: string): Promise<string | null> {
    if (typeof window === 'undefined') return Promise.resolve(null);

    const tg = getTelegramWebApp();
    if (!tg) {
      return Promise.resolve(localStorage.getItem(key));
    }

    return new Promise<string | null>((resolve) => {
      tg.CloudStorage.getItem(key, (err, value) => {
        if (err || !value) {
          resolve(null);
        } else {
          resolve(value);
        }
      });
    });
  },

  set(key: string, value: string): Promise<void> {
    if (typeof window === 'undefined') return Promise.resolve();

    const tg = getTelegramWebApp();
    if (!tg) {
      localStorage.setItem(key, value);
      return Promise.resolve();
    }

    return new Promise<void>((resolve, reject) => {
      tg.CloudStorage.setItem(key, value, (err, stored) => {
        if (err || !stored) {
          reject(new Error(err ? String(err) : `Failed to store key "${key}"`));
        } else {
          resolve();
        }
      });
    });
  },

  remove(key: string): Promise<void> {
    if (typeof window === 'undefined') return Promise.resolve();

    const tg = getTelegramWebApp();
    if (!tg) {
      localStorage.removeItem(key);
      return Promise.resolve();
    }

    return new Promise<void>((resolve, reject) => {
      tg.CloudStorage.removeItem(key, (err, removed) => {
        if (err || !removed) {
          reject(new Error(err ? String(err) : `Failed to remove key "${key}"`));
        } else {
          resolve();
        }
      });
    });
  },
};
