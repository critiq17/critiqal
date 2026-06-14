<script lang="ts">
	import type { UserBadge } from '$lib/types';
	import { portal } from '$lib/actions/portal';
	import { clickOutside } from '$lib/actions/clickOutside';
	import { longpress } from '$lib/actions/longpress';
	import { cubicIn, backOut } from 'svelte/easing';
	import type { TransitionConfig } from 'svelte/transition';
	import Sheet from '$lib/ui/Sheet.svelte';
	import BadgeDetail from './BadgeDetail.svelte';
	import BadgeIcon from './BadgeIcon.svelte';
	import { badgeMeta, tierStyle } from './badgeMeta';

	// Spring-pop entry: scales from 0.92 and floats up slightly.
	function popoverIn(_node: HTMLElement): TransitionConfig {
		return {
			duration: 220,
			easing: backOut,
			css: (t, u) =>
				`opacity: ${t}; transform: translateX(-50%) translateY(${-7 * u}px) scale(${0.92 + 0.08 * t})`,
		};
	}

	// Quick fade-scale out.
	function popoverOut(_node: HTMLElement): TransitionConfig {
		return {
			duration: 140,
			easing: cubicIn,
			css: (t, u) =>
				`opacity: ${t}; transform: translateX(-50%) translateY(${-4 * u}px) scale(${0.96 + 0.04 * t})`,
		};
	}

	interface Props {
		badge: UserBadge;
	}

	let { badge }: Props = $props();

	let meta = $derived(badgeMeta(badge.code));
	let style = $derived(tierStyle(meta.tier));

	let open = $state(false);
	let isMobile = $state(false);
	let triggerEl = $state<HTMLButtonElement | null>(null);
	let top = $state(0);
	let left = $state(0);
	let closeTimer: ReturnType<typeof setTimeout> | undefined;

	$effect(() => {
		const mq = window.matchMedia('(max-width: 640px)');
		const sync = () => (isMobile = mq.matches);
		sync();
		mq.addEventListener('change', sync);
		return () => mq.removeEventListener('change', sync);
	});

	$effect(() => {
		return () => clearCloseTimer();
	});

	function clearCloseTimer(): void {
		if (closeTimer !== undefined) {
			clearTimeout(closeTimer);
			closeTimer = undefined;
		}
	}

	function positionPopover(): void {
		if (!triggerEl) return;
		const rect = triggerEl.getBoundingClientRect();
		top = rect.bottom + 8;
		left = Math.min(window.innerWidth - 148, Math.max(148, rect.left + rect.width / 2));
	}

	function openDetail(): void {
		clearCloseTimer();
		if (!isMobile) positionPopover();
		open = true;
	}

	function close(): void {
		clearCloseTimer();
		open = false;
	}

	function scheduleClose(): void {
		if (isMobile) return;
		clearCloseTimer();
		closeTimer = setTimeout(() => {
			open = false;
			closeTimer = undefined;
		}, 130);
	}

	function onPointerEnter(event: PointerEvent): void {
		if (event.pointerType === 'mouse') openDetail();
	}

	function onKeydown(event: KeyboardEvent): void {
		if (open && event.key === 'Escape') close();
	}
</script>

<svelte:window onkeydown={onKeydown} onresize={() => open && !isMobile && positionPopover()} />

<button
	bind:this={triggerEl}
	type="button"
	class="badge-pill"
	style:--badge-accent={style.accent}
	style:--badge-glow={style.glow}
	onclick={openDetail}
	onpointerenter={onPointerEnter}
	onpointerleave={scheduleClose}
	onfocus={openDetail}
	onblur={scheduleClose}
	use:longpress={{ onlongpress: openDetail }}
	aria-haspopup="dialog"
	aria-expanded={open}
	aria-label={badge.name}
	title={badge.name}
>
	<span class="icon-shell" aria-hidden="true">
		<BadgeIcon glyph={meta.glyph} size={18} />
	</span>
	<span class="badge-name">{badge.name}</span>
</button>

{#if isMobile}
	<Sheet {open} onclose={close} maxHeight="72vh">
		<BadgeDetail {badge} />
	</Sheet>
{:else if open}
	<div
		class="badge-popover"
		use:portal
		use:clickOutside={{ onClickOutside: close }}
		in:popoverIn
		out:popoverOut
		role="dialog"
		tabindex="-1"
		aria-label={badge.name}
		style:top="{top}px"
		style:left="{left}px"
		onpointerenter={() => clearCloseTimer()}
		onpointerleave={scheduleClose}
	>
		<BadgeDetail {badge} />
	</div>
{/if}

<style>
	.badge-pill {
		--badge-accent: var(--color-accent);
		--badge-glow: rgba(255, 255, 255, 0.1);
		position: relative;
		isolation: isolate;
		display: inline-grid;
		grid-template-columns: 18px minmax(0, auto);
		align-items: center;
		gap: 6px;
		max-width: min(176px, 100%);
		height: 28px;
		min-width: 0;
		padding: 0 10px 0 6px;
		border-radius: var(--radius-full);
		border: 1px solid color-mix(in srgb, var(--badge-accent) 26%, var(--glass-border));
		background:
			linear-gradient(180deg, rgba(255, 255, 255, 0.09), rgba(255, 255, 255, 0) 45%),
			color-mix(in srgb, var(--glass-bg-soft) 86%, var(--badge-accent) 9%);
		backdrop-filter: blur(var(--glass-blur-md)) saturate(var(--glass-saturate-soft));
		-webkit-backdrop-filter: blur(var(--glass-blur-md)) saturate(var(--glass-saturate-soft));
		color: var(--color-text-primary);
		box-shadow:
			inset 0 1px 0 color-mix(in srgb, var(--glass-highlight) 86%, white 16%),
			inset 0 -1px 0 rgba(0, 0, 0, 0.14),
			0 5px 18px color-mix(in srgb, var(--badge-glow) 64%, transparent);
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
		touch-action: manipulation;
		transition:
			border-color 140ms ease,
			background 140ms ease,
			box-shadow 140ms ease,
			transform 140ms ease;
	}

	.badge-pill::before {
		content: '';
		position: absolute;
		inset: 1px;
		z-index: -1;
		border-radius: inherit;
		background: radial-gradient(circle at 28% 12%, rgba(255, 255, 255, 0.2), transparent 42%);
		pointer-events: none;
	}

	.badge-pill:hover,
	.badge-pill:focus-visible,
	.badge-pill[aria-expanded='true'] {
		border-color: color-mix(in srgb, var(--badge-accent) 42%, var(--glass-border));
		background:
			linear-gradient(180deg, rgba(255, 255, 255, 0.13), rgba(255, 255, 255, 0) 48%),
			color-mix(in srgb, var(--glass-bg-soft) 78%, var(--badge-accent) 14%);
		box-shadow:
			inset 0 1px 0 color-mix(in srgb, var(--glass-highlight) 90%, white 24%),
			inset 0 -1px 0 rgba(0, 0, 0, 0.16),
			0 7px 24px color-mix(in srgb, var(--badge-glow) 86%, transparent);
		transform: translateY(-1px);
	}

	.badge-pill:focus-visible {
		outline: 2px solid color-mix(in srgb, var(--badge-accent) 72%, white);
		outline-offset: 2px;
	}

	.icon-shell {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		width: 18px;
		height: 18px;
		color: color-mix(in srgb, var(--color-text-primary) 82%, #05070a 18%);
		flex-shrink: 0;
	}

	.badge-name {
		min-width: 0;
		overflow: hidden;
		text-overflow: ellipsis;
		font-size: 0.78rem;
		font-weight: 680;
		letter-spacing: 0;
		line-height: 1;
		white-space: nowrap;
	}

	.badge-popover {
		position: fixed;
		z-index: 1000;
		transform: translateX(-50%);
		width: min(288px, calc(100vw - 24px));
		box-sizing: border-box;
		padding: 14px;
		border-radius: var(--radius-lg, 16px);
		border: 1px solid var(--glass-border);
		background: color-mix(in srgb, var(--glass-bg-strong) 86%, transparent);
		backdrop-filter: blur(calc(var(--glass-blur) + 4px)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(calc(var(--glass-blur) + 4px)) saturate(var(--glass-saturate));
		box-shadow:
			var(--glass-shadow),
			inset 0 1px 0 var(--glass-highlight),
			0 14px 44px rgba(0, 0, 0, 0.34);
	}

	@media (prefers-reduced-motion: reduce) {
		.badge-pill {
			transition: none;
		}
	}
</style>
