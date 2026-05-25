// Tiny persistent cache layer for SWR stores. Survives reloads (sessionStorage
// on desktop, localStorage in TMA WebView) so the first paint after a cold
// start can show the last known feed/profile instantly while a network
// revalidate runs in the background. JSON-serialised, schema-versioned to
// invalidate on shape changes.

const SCHEMA_VERSION = 1;
const KEY_PREFIX = 'critiqal:v' + SCHEMA_VERSION + ':';

function isTMA(): boolean {
	if (typeof window === 'undefined') return false;
	const wa = (window as unknown as { Telegram?: { WebApp?: { platform?: string } } }).Telegram?.WebApp;
	return !!wa && wa.platform !== undefined && wa.platform !== 'unknown';
}

function storage(): Storage | null {
	if (typeof window === 'undefined') return null;
	try {
		return isTMA() ? window.localStorage : window.sessionStorage;
	} catch {
		return null;
	}
}

interface Envelope<T> {
	v: number;
	t: number;
	data: T;
}

export function readCache<T>(key: string): { data: T; loadedAt: number } | null {
	const s = storage();
	if (!s) return null;
	try {
		const raw = s.getItem(KEY_PREFIX + key);
		if (!raw) return null;
		const env = JSON.parse(raw) as Envelope<T>;
		if (env.v !== SCHEMA_VERSION || typeof env.t !== 'number') return null;
		return { data: env.data, loadedAt: env.t };
	} catch {
		return null;
	}
}

export function writeCache<T>(key: string, data: T): void {
	const s = storage();
	if (!s) return;
	try {
		const env: Envelope<T> = { v: SCHEMA_VERSION, t: Date.now(), data };
		s.setItem(KEY_PREFIX + key, JSON.stringify(env));
	} catch {
		// quota exceeded or private mode — ignore
	}
}

export function clearCacheKey(key: string): void {
	const s = storage();
	if (!s) return;
	try {
		s.removeItem(KEY_PREFIX + key);
	} catch {
		// ignore
	}
}

export function clearAllCache(): void {
	const s = storage();
	if (!s) return;
	try {
		const toRemove: string[] = [];
		for (let i = 0; i < s.length; i++) {
			const k = s.key(i);
			if (k && k.startsWith(KEY_PREFIX)) toRemove.push(k);
		}
		toRemove.forEach((k) => s.removeItem(k));
	} catch {
		// ignore
	}
}
