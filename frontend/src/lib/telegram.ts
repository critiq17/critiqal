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
  viewportHeight: number;
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
  disableVerticalSwipes?(): void;
  BackButton: {
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
  if (window.Telegram?.WebApp) return true;
  if (window.location.hash.includes('tgWebApp')) return true;
  return false;
}

export function getTelegramWebApp(): TelegramWebApp | null {
  if (!isTelegramMiniApp()) return null;
  return window.Telegram?.WebApp ?? null;
}

function applyThemeVars(tg: TelegramWebApp): void {
  const root = document.documentElement;
  root.style.setProperty('--tg-bg', tg.backgroundColor);
  root.style.setProperty('--tg-text', tg.themeParams.text_color);
  root.style.setProperty('--tg-hint', tg.themeParams.hint_color);
  root.style.setProperty('--tg-accent', tg.themeParams.button_color);
  root.style.setProperty('--tg-btn-text', tg.themeParams.button_text_color);

  // Keep header color in sync with app background on theme changes.
  // Using backgroundColor (not themeParams.bg_color) ensures it matches --tg-bg exactly.
  tg.setHeaderColor(tg.backgroundColor || '#0f0f0f');
}

function applyViewportVars(tg: TelegramWebApp): void {
  const root = document.documentElement;
  root.style.setProperty(
    '--tg-viewport-height',
    (tg.viewportHeight || window.innerHeight) + 'px'
  );

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

  tg.onEvent('viewportChanged', () => applyViewportVars(tg));
  tg.onEvent('themeChanged', () => applyThemeVars(tg));
  tg.onEvent('fullscreenChanged', () => applyViewportVars(tg));
  // safeAreaChanged / contentSafeAreaChanged can fire after fullscreenChanged
  // with the actual non-zero inset values — must listen to both.
  tg.onEvent('safeAreaChanged', () => applyViewportVars(tg));
  tg.onEvent('contentSafeAreaChanged', () => applyViewportVars(tg));

  tg.disableVerticalSwipes?.();
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
