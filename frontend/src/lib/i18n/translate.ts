import { i18n } from './store.svelte';
import type { DictPath } from './types';

// Resolve a dot-path against the active dictionary.
// Reactive: reading i18n.dict subscribes the calling component, so a
// setLocale() call re-renders every t() consumer automatically.
export function t(path: DictPath): string {
	const segments = path.split('.');
	// eslint-disable-next-line @typescript-eslint/no-explicit-any
	let node: any = i18n.dict;
	for (const seg of segments) {
		if (node == null || typeof node !== 'object') return path;
		node = node[seg];
	}
	return typeof node === 'string' ? node : path;
}
