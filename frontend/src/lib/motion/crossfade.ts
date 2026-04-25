/**
 * Crossfade transition pair for skeleton → content transitions.
 * Uses svelte/transition crossfade with sensible defaults.
 *
 * @example
 * import { skeletonCrossfade } from '$lib/motion/crossfade';
 * const [send, receive] = skeletonCrossfade;
 *
 * {#if loading}
 *   <div in:receive={{ key: 'card' }} out:send={{ key: 'card' }}>
 *     <Skeleton />
 *   </div>
 * {:else}
 *   <div in:receive={{ key: 'card' }} out:send={{ key: 'card' }}>
 *     <Content />
 *   </div>
 * {/if}
 */
import { crossfade } from 'svelte/transition';
import { quintOut } from 'svelte/easing';

export const skeletonCrossfade = crossfade({
	duration: 220,
	easing: quintOut,
	fallback(node) {
		return {
			duration: 180,
			css: (t: number) => `opacity: ${t}`
		};
	}
});
