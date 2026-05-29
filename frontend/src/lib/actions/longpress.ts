// Svelte action: fires `onlongpress` after the pointer is held still for
// `duration` ms. Cancels on move beyond a small tolerance, release, or leave.
// Used for press-and-hold affordances (e.g. revealing badge details on touch).
export interface LongpressParams {
	onlongpress: (event: PointerEvent) => void;
	duration?: number;
}

const DEFAULT_DURATION = 450;
const MOVE_TOLERANCE = 10;

export function longpress(node: HTMLElement, params: LongpressParams) {
	let current = params;
	let timer: ReturnType<typeof setTimeout> | undefined;
	let startX = 0;
	let startY = 0;

	function clear() {
		if (timer !== undefined) {
			clearTimeout(timer);
			timer = undefined;
		}
		node.removeEventListener('pointermove', onMove);
		node.removeEventListener('pointerup', clear);
		node.removeEventListener('pointercancel', clear);
		node.removeEventListener('pointerleave', clear);
	}

	function onMove(event: PointerEvent) {
		if (Math.hypot(event.clientX - startX, event.clientY - startY) > MOVE_TOLERANCE) {
			clear();
		}
	}

	function onDown(event: PointerEvent) {
		// Ignore secondary mouse buttons; allow touch/pen.
		if (event.pointerType === 'mouse' && event.button !== 0) return;
		startX = event.clientX;
		startY = event.clientY;
		const duration = current.duration ?? DEFAULT_DURATION;
		timer = setTimeout(() => {
			clear();
			current.onlongpress(event);
		}, duration);
		node.addEventListener('pointermove', onMove);
		node.addEventListener('pointerup', clear);
		node.addEventListener('pointercancel', clear);
		node.addEventListener('pointerleave', clear);
	}

	node.addEventListener('pointerdown', onDown);

	return {
		update(next: LongpressParams) {
			current = next;
		},
		destroy() {
			clear();
			node.removeEventListener('pointerdown', onDown);
		}
	};
}
