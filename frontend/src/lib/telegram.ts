interface TelegramWebApp {
  ready(): void;
  expand(): void;
  requestFullscreen?(): void;
  colorScheme: 'light' | 'dark';
  backgroundColor: string;
  themeParams: {
    text_color: string;
    hint_color: string;
    button_color: string;
    button_text_color: string;
  };
  BackButton: {
    show(): void;
    hide(): void;
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
  return typeof window !== 'undefined' && !!window.Telegram?.WebApp;
}

export function getTelegramWebApp(): TelegramWebApp | null {
  if (!isTelegramMiniApp()) return null;
  return window.Telegram?.WebApp ?? null;
}

export function initTelegram(): void {
  const tg = getTelegramWebApp();
  if (!tg) return;

  tg.ready();
  tg.expand();

  try {
    tg.requestFullscreen?.();
  } catch {
    // requestFullscreen is optional and may not be supported in all clients
  }

  const root = document.documentElement;
  root.style.setProperty('--tg-bg', tg.backgroundColor);
  root.style.setProperty('--tg-text', tg.themeParams.text_color);
  root.style.setProperty('--tg-hint', tg.themeParams.hint_color);
  root.style.setProperty('--tg-accent', tg.themeParams.button_color);
  root.style.setProperty('--tg-btn-text', tg.themeParams.button_text_color);
  root.style.setProperty('--bottom-nav-height', '72px');
  root.style.setProperty('--safe-bottom', 'env(safe-area-inset-bottom, 0px)');
  root.style.setProperty('--safe-top', 'env(safe-area-inset-top, 0px)');
  root.style.setProperty(
    '--content-bottom-padding',
    'calc(var(--bottom-nav-height) + var(--safe-bottom) + 16px)'
  );
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
