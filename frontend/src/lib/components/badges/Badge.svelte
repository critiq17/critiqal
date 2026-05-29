<script lang="ts">
	import type { UserBadge } from '$lib/types';
	import { portal } from '$lib/actions/portal';
	import { clickOutside } from '$lib/actions/clickOutside';
	import { longpress } from '$lib/actions/longpress';
	import Sheet from '$lib/ui/Sheet.svelte';
	import BadgeMedallion from './BadgeMedallion.svelte';
	import BadgeDetail from './BadgeDetail.svelte';

	type Size = 'sm' | 'md' | 'lg';

	interface Props {
		badge: UserBadge;
		size?: Size;
	}

	let { badge, size = 'md' }: Props = $props();

	let open = $state(false);
	let isMobile = $state(false);
	let triggerEl = $state<HTMLButtonElement | null>(null);
	let top = $state(0);
	let left = $state(0);

	$effect(() => {
		const mq = window.matchMedia('(max-width: 640px)');
		const sync = () => (isMobile = mq.matches);
		sync();
		mq.addEventListener('change', sync);
		return () => mq.removeEventListener('change', sync);
	});

	function openDetail() {
		if (!isMobile && triggerEl) {
			const rect = triggerEl.getBoundingClientRect();
			top = rect.bottom + 8;
			left = rect.left + rect.width / 2;
		}
		open = true;
	}

	function close() {
		open = false;
	}

	function onKeydown(event: KeyboardEvent) {
		if (open && event.key === 'Escape') close();
	}
</script>

<svelte:window onkeydown={onKeydown} />

<button
	bind:this={triggerEl}
	type="button"
	class="badge-trigger"
	onclick={openDetail}
	use:longpress={{ onlongpress: openDetail }}
	aria-haspopup="dialog"
	aria-expanded={open}
	aria-label={badge.name}
>
	<BadgeMedallion code={badge.code} name={badge.name} {size} />
</button>

{#if open && isMobile}
	<Sheet {open} onclose={close} title={badge.name}>
		<BadgeDetail {badge} />
	</Sheet>
{:else if open}
	<div
		class="badge-popover"
		use:portal
		use:clickOutside={{ onClickOutside: close }}
		role="dialog"
		aria-label={badge.name}
		style:top="{top}px"
		style:left="{left}px"
	>
		<BadgeDetail {badge} />
	</div>
{/if}

<style>
	.badge-trigger {
		display: inline-flex;
		padding: 0;
		border: none;
		background: none;
		cursor: pointer;
		border-radius: var(--radius-full);
		-webkit-tap-highlight-color: transparent;
		touch-action: manipulation;
	}
	.badge-trigger:focus-visible {
		outline: 2px solid var(--color-accent);
		outline-offset: 2px;
	}

	.badge-popover {
		position: fixed;
		z-index: 1000;
		transform: translateX(-50%);
		max-width: min(280px, calc(100vw - 24px));
		padding: 14px;
		border-radius: var(--radius-lg, 16px);
		border: 1px solid var(--color-border);
		background: color-mix(in srgb, var(--color-surface-raised, #1b1b22) 78%, transparent);
		backdrop-filter: blur(16px) saturate(140%);
		-webkit-backdrop-filter: blur(16px) saturate(140%);
		box-shadow: 0 12px 40px rgba(0, 0, 0, 0.35);
		animation: badge-pop 140ms ease-out;
	}

	@keyframes badge-pop {
		from {
			opacity: 0;
			transform: translateX(-50%) translateY(-4px) scale(0.97);
		}
		to {
			opacity: 1;
			transform: translateX(-50%) translateY(0) scale(1);
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.badge-popover {
			animation: none;
		}
	}
</style>
