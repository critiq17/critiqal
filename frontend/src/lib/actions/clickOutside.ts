/**
 * Svelte action: use:clickOutside
 *
 * Calls `onClickOutside` when a click occurs outside the element.
 *
 * @example
 * <div use:clickOutside={{ onClickOutside: closeMenu }}></div>
 */
interface ClickOutsideOptions {
	onClickOutside: () => void;
	enabled?: boolean;
}

export function clickOutside(
	el: HTMLElement,
	options: ClickOutsideOptions
): { update: (opts: ClickOutsideOptions) => void; destroy: () => void } {
	let currentOptions = options;

	function handleClick(e: MouseEvent): void {
		if (!currentOptions.enabled && currentOptions.enabled !== undefined) return;
		if (!el.contains(e.target as Node)) {
			currentOptions.onClickOutside();
		}
	}

	document.addEventListener('click', handleClick, true);

	return {
		update(newOptions: ClickOutsideOptions): void {
			currentOptions = newOptions;
		},
		destroy(): void {
			document.removeEventListener('click', handleClick, true);
		}
	};
}
