/**
 * Reparents the node to a target element (default: document.body) for as long
 * as it is mounted. Use on fixed-position overlays so they escape any
 * ancestor with `transform`, `filter`, `perspective`, `contain`, or
 * `will-change`, which would otherwise become the containing block for
 * `position: fixed` and trap the overlay inside that ancestor.
 *
 *   <div class="overlay" use:portal> … </div>
 */
export function portal(
	node: HTMLElement,
	target: HTMLElement | string = 'body'
): { destroy: () => void } {
	const host =
		typeof target === 'string'
			? (document.querySelector(target) as HTMLElement | null) ?? document.body
			: target;

	host.appendChild(node);

	return {
		destroy(): void {
			node.parentNode?.removeChild(node);
		},
	};
}
