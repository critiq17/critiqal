import { writable, derived } from 'svelte/store';

const openCount = writable(0);

export const anySheetOpen = derived(openCount, ($c) => $c > 0);

export function openSheet(): () => void {
	openCount.update((c) => c + 1);
	return () => openCount.update((c) => Math.max(0, c - 1));
}
