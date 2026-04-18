import { writable } from 'svelte/store';

export const composeOpen = writable(false);

export function openCompose(): void {
	composeOpen.set(true);
}

export function closeCompose(): void {
	composeOpen.set(false);
}
