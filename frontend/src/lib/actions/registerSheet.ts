import { sheetStore } from '$lib/stores/sheet.store.svelte';

/**
 * Marks an element as a bottom sheet / modal surface for as long as it is in
 * the DOM. While any registered surface is mounted, `sheetStore.anyOpen` is
 * true, which the mobile layout uses to hide the bottom navigation bar.
 *
 * Apply to the root element of a sheet that is conditionally rendered
 * (e.g. inside `{#if open}`), so mount/destroy maps to open/close:
 *
 *   <div class="panel" use:registerSheet> … </div>
 */
export function registerSheet(_node: HTMLElement): { destroy: () => void } {
	const release = sheetStore.open();
	return { destroy: release };
}
