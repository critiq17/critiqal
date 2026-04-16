/**
 * Thin pub/sub for overlay ↔ background swipe coordination.
 *
 * A direct callback (not a Svelte store) is used intentionally — we need to
 * fire on every touchmove frame without going through Svelte's reactivity
 * scheduler, which would add a render-cycle delay and kill the 60fps feel.
 */

export type SwipePhase = 'drag' | 'dismiss' | 'cancel';

type SwipeListener = (x: number, screenWidth: number, phase: SwipePhase) => void;

let _listener: SwipeListener | null = null;

export function registerOverlaySwipeListener(fn: SwipeListener): () => void {
	_listener = fn;
	return () => {
		if (_listener === fn) _listener = null;
	};
}

export function notifyOverlaySwipe(x: number, screenWidth: number, phase: SwipePhase): void {
	_listener?.(x, screenWidth, phase);
}
