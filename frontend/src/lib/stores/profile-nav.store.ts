import { writable } from 'svelte/store';

// Holds the username of the profile being viewed in the overlay.
// null = overlay is closed.
export const profileNav = writable<string | null>(null);

export function openProfile(username: string): void {
	profileNav.set(username);
}

export function closeProfile(): void {
	profileNav.set(null);
}
