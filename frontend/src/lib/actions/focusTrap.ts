/**
 * Svelte action: use:focusTrap
 *
 * Traps keyboard focus within the element. Useful for modals and sheets.
 * Releases focus when the element is destroyed.
 *
 * @example
 * <div use:focusTrap role="dialog"></div>
 */

const FOCUSABLE_SELECTORS = [
	'a[href]',
	'button:not([disabled])',
	'input:not([disabled])',
	'select:not([disabled])',
	'textarea:not([disabled])',
	'[tabindex]:not([tabindex="-1"])'
].join(', ');

function getFocusableElements(el: HTMLElement): HTMLElement[] {
	return Array.from(el.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTORS)).filter(
		(node) => !node.closest('[inert]') && getComputedStyle(node).display !== 'none'
	);
}

export function focusTrap(
	el: HTMLElement
): { destroy: () => void } {
	const previouslyFocused = document.activeElement as HTMLElement | null;

	// Focus the first focusable element inside
	const focusable = getFocusableElements(el);
	focusable[0]?.focus();

	function handleKeydown(e: KeyboardEvent): void {
		if (e.key !== 'Tab') return;

		const elements = getFocusableElements(el);
		if (elements.length === 0) {
			e.preventDefault();
			return;
		}

		const first = elements[0];
		const last = elements[elements.length - 1];

		if (e.shiftKey) {
			if (document.activeElement === first) {
				e.preventDefault();
				last?.focus();
			}
		} else {
			if (document.activeElement === last) {
				e.preventDefault();
				first?.focus();
			}
		}
	}

	el.addEventListener('keydown', handleKeydown);

	return {
		destroy(): void {
			el.removeEventListener('keydown', handleKeydown);
			previouslyFocused?.focus();
		}
	};
}
