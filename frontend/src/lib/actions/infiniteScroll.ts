/**
 * Svelte action: use:infiniteScroll
 *
 * Calls `onTrigger` when the element scrolls into view.
 * Automatically disconnects on element destroy.
 *
 * @example
 * <div use:infiniteScroll={{ onTrigger: loadMore, disabled: isLoadingMore }}></div>
 */
interface InfiniteScrollOptions {
	onTrigger: () => void;
	disabled?: boolean;
	threshold?: number;
}

export function infiniteScroll(
	el: HTMLElement,
	options: InfiniteScrollOptions
): { update: (opts: InfiniteScrollOptions) => void; destroy: () => void } {
	let currentOptions = options;

	const obs = new IntersectionObserver(
		([entry]) => {
			if (entry?.isIntersecting && !currentOptions.disabled) {
				currentOptions.onTrigger();
			}
		},
		{ threshold: options.threshold ?? 0.1 }
	);

	obs.observe(el);

	return {
		update(newOptions: InfiniteScrollOptions): void {
			currentOptions = newOptions;
		},
		destroy(): void {
			obs.disconnect();
		}
	};
}
