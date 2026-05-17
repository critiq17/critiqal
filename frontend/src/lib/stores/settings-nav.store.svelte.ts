// Backward-compatible facade over the unified navStack. Settings is just
// another entry on the overlay history stack.
import { navStack } from './nav-stack.store.svelte';

export function openSettings(): void {
	navStack.pushSettings();
}

export function closeSettings(): void {
	navStack.pop();
}
