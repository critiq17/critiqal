import type { Dict } from './locales/ru';

export type Locale = 'ru' | 'uk' | 'en';

export const LOCALES: readonly Locale[] = ['ru', 'uk', 'en'] as const;

export const DEFAULT_LOCALE: Locale = 'ru';

// Build the dot-path key union from the dictionary shape.
// Allows t('settings.language.title') with full IDE autocomplete and
// compile-time errors on typos / missing keys.
type Join<K, P> = K extends string
	? P extends string
		? `${K}.${P}`
		: never
	: never;

type Paths<T, D extends number = 6> = [D] extends [never]
	? never
	: T extends object
	? {
			[K in keyof T]-?: K extends string
				? T[K] extends string
					? K
					: Join<K, Paths<T[K], Prev[D]>>
				: never;
	  }[keyof T]
	: never;

type Prev = [never, 0, 1, 2, 3, 4, 5, 6];

export type DictPath = Paths<Dict>;
