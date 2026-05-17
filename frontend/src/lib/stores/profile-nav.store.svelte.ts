// Backward-compatible facade over the unified navStack. Existing call sites
// (feed, profile, post author taps) keep using openProfile/closeProfile;
// the actual history + rendering now lives in navStack + OverlayHost.
import { navStack } from './nav-stack.store.svelte';

export function openProfile(username: string): void {
	navStack.pushProfile(username);
}

export function closeProfile(): void {
	navStack.pop();
}
