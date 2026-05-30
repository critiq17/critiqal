import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { longpress } from './longpress';

function pointer(type: string, x = 0, y = 0): Event {
	// MouseEvent carries clientX/clientY and exists in the jsdom test env.
	return new MouseEvent(type, { clientX: x, clientY: y, button: 0, bubbles: true });
}

describe('longpress', () => {
	beforeEach(() => vi.useFakeTimers());
	afterEach(() => vi.useRealTimers());

	it('fires after the hold duration', () => {
		const node = document.createElement('div');
		const onlongpress = vi.fn();
		const action = longpress(node, { onlongpress, duration: 400 });

		node.dispatchEvent(pointer('pointerdown'));
		vi.advanceTimersByTime(399);
		expect(onlongpress).not.toHaveBeenCalled();
		vi.advanceTimersByTime(1);
		expect(onlongpress).toHaveBeenCalledTimes(1);

		action.destroy();
	});

	it('cancels when released before the duration', () => {
		const node = document.createElement('div');
		const onlongpress = vi.fn();
		const action = longpress(node, { onlongpress, duration: 400 });

		node.dispatchEvent(pointer('pointerdown'));
		vi.advanceTimersByTime(200);
		node.dispatchEvent(pointer('pointerup'));
		vi.advanceTimersByTime(400);
		expect(onlongpress).not.toHaveBeenCalled();

		action.destroy();
	});

	it('cancels when the pointer moves past the tolerance', () => {
		const node = document.createElement('div');
		const onlongpress = vi.fn();
		const action = longpress(node, { onlongpress, duration: 400 });

		node.dispatchEvent(pointer('pointerdown', 0, 0));
		node.dispatchEvent(pointer('pointermove', 40, 40));
		vi.advanceTimersByTime(400);
		expect(onlongpress).not.toHaveBeenCalled();

		action.destroy();
	});
});
