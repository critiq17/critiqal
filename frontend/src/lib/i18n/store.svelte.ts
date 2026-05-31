import { ru } from './locales/ru';
import { DEFAULT_LOCALE, LOCALES, type Locale } from './types';
import type { Dict } from './locales/ru';

const STORAGE_KEY = 'critiqal:lang';

// Only `ru` (DEFAULT_LOCALE) is loaded eagerly so the initial bundle stays lean.
// `uk` and `en` are fetched on first use and cached here.
const DICTS: Partial<Record<Locale, Dict>> = { ru };

async function loadLocale(locale: Locale): Promise<Dict> {
  if (DICTS[locale]) return DICTS[locale]!;
  const mod = locale === 'uk' ? await import('./locales/uk') : await import('./locales/en');
  DICTS[locale] = (mod as Record<string, Dict>)[locale];
  return DICTS[locale]!;
}

function readStored(): Locale | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (raw && (LOCALES as readonly string[]).includes(raw)) return raw as Locale;
  } catch {
    // localStorage blocked — fall through
  }
  return null;
}

function detectFromBrowser(): Locale {
  if (typeof navigator === 'undefined') return DEFAULT_LOCALE;
  const langs = navigator.languages ?? [navigator.language];
  for (const raw of langs) {
    const base = (raw ?? '').toLowerCase().split('-')[0];
    if (base === 'uk') return 'uk';
    if (base === 'ru' || base === 'be' || base === 'kk') return 'ru';
    if (base === 'en') return 'en';
  }
  return DEFAULT_LOCALE;
}

function pickInitial(): Locale {
  return readStored() ?? detectFromBrowser();
}

// Top-level $state — read synchronously by t() in any component.
// Initialised at module load so the very first render already has the
// right strings: no FOUC, no async, no flicker.
let current = $state<Locale>(pickInitial());
// Fall back to `ru` synchronously; if the stored locale is uk/en it will be
// loaded asynchronously by setLocaleImmediate called shortly after boot.
let dict = $state<Dict>(DICTS[current] ?? ru);
let switching = $state<boolean>(false);

function writeStored(value: Locale): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, value);
  } catch {
    // quota / private mode — best effort, in-memory state still updates
  }
}

export const i18n = {
  get locale(): Locale {
    return current;
  },
  get dict(): Dict {
    return dict;
  },
  get switching(): boolean {
    return switching;
  },
  get isReady(): boolean {
    return !switching;
  },

  // Trigger a language switch with the star-burst overlay.
  // The overlay component watches `switching` and renders accordingly.
  // We flip the dict at the burst peak (~360ms in) so the user sees the new
  // language under the closing star — no flash of the wrong language.
  async setLocale(next: Locale): Promise<void> {
    if (next === current || switching) return;
    switching = true;
    writeStored(next);

    // Pre-fetch the locale dict in parallel with the burst animation so it's
    // ready by the time we flip at the peak (~360 ms in).
    const nextDict = await Promise.all([
      loadLocale(next),
      new Promise<void>((r) => setTimeout(r, 360)),
    ]).then(([d]) => d);

    current = next;
    dict = nextDict;

    await new Promise((r) => setTimeout(r, 420));
    switching = false;
  },

  // Synchronous variant used during boot when the overlay isn't mounted.
  // If the locale hasn't been fetched yet, show `ru` for the current render
  // and silently switch once the import resolves (imperceptible on first paint).
  setLocaleImmediate(next: Locale): void {
    if (next === current) return;
    writeStored(next);
    if (DICTS[next]) {
      current = next;
      dict = DICTS[next]!;
    } else {
      loadLocale(next).then((d) => {
        current = next;
        dict = d;
      });
    }
  },
};
