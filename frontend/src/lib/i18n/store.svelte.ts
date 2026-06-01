import { ru } from './locales/ru';
import { uk } from './locales/uk';
import { en } from './locales/en';
import { DEFAULT_LOCALE, LOCALES, type Locale } from './types';
import type { Dict } from './locales/ru';

const STORAGE_KEY = 'critiqal:lang';

// All three dictionaries are bundled eagerly. They're plain string maps (a few
// KB each) and loading them up front is what makes the active locale correct on
// the very first render — the previous lazy scheme left `dict` stuck on `ru`
// whenever the stored locale (uk/en) hadn't been fetched yet, so the UI showed
// Russian even though the switcher said otherwise.
const DICTS: Record<Locale, Dict> = { ru, uk, en };

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

// Top-level $state — read synchronously by t() in any component. `dict` is
// always kept in lockstep with `current`, so there is never a window where the
// locale and the strings disagree.
let current = $state<Locale>(pickInitial());
let dict = $state<Dict>(DICTS[current]);
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

  // Trigger a language switch with the star-burst overlay. The dict is flipped
  // at the burst peak (~360 ms in) so the user sees the new language emerge
  // from under the closing star — no flash of the wrong language.
  async setLocale(next: Locale): Promise<void> {
    if (next === current || switching) return;
    switching = true;
    writeStored(next);

    await new Promise((r) => setTimeout(r, 360));
    current = next;
    dict = DICTS[next];

    await new Promise((r) => setTimeout(r, 420));
    switching = false;
  },

  // Synchronous variant (no overlay) — used wherever an instant switch is wanted.
  setLocaleImmediate(next: Locale): void {
    if (next === current) return;
    writeStored(next);
    current = next;
    dict = DICTS[next];
  },
};
