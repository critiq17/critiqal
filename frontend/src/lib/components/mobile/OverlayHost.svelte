<script lang="ts">
	import { untrack } from 'svelte';
	import { navStack, type NavEntry } from '$lib/stores/nav-stack.store.svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import { pushBackHandler } from '$lib/tma/back-button';
	import { registerSheet } from '$lib/actions/registerSheet';
	import ProfileOverlayView from './ProfileOverlayView.svelte';
	import ConnectionsOverlayView from './ConnectionsOverlayView.svelte';

	// Lazy-loaded: 1294 LOC — only needed when settings is pushed onto the nav stack.
	let MobileSettingsOverlay = $state<typeof import('./MobileSettingsOverlay.svelte').default | null>(null);

	// Fraction of screen width the layer *beneath* the top one is pushed left,
	// for the iOS-style parallax depth cue.
	const PUSH = 0.24;
	const ENTER_MS = 280;
	const EXIT_MS = 260;
	const SNAP_MS = 360;
	const DISMISS_RATIO = 0.32;
	const VELOCITY_PX_MS = 0.45;

	// rendered = live stack, plus one transient "leaving" entry kept mounted
	// while it animates out (so a programmatic back / swipe has an exit anim).
	let leaving = $state<NavEntry | null>(null);
	const rendered = $derived(leaving ? [...navStack.entries, leaving] : navStack.entries);

	const els = new Map<number, HTMLElement>();
	let animating = false;

	function register(node: HTMLElement, key: number) {
		els.set(key, node);
		// A freshly mounted layer is always the new top — start it off-screen
		// (no transition) so the push effect can slide it in without a flash
		// of it appearing at rest first.
		node.style.transition = 'none';
		node.style.transform = `translate3d(${sw()}px, 0, 0)`;
		return { destroy: () => els.delete(key) };
	}

	function setX(key: number, x: number, ms: number): void {
		const el = els.get(key);
		if (!el) return;
		el.style.transition = ms > 0 ? `transform ${ms}ms cubic-bezier(0.32, 0.72, 0, 1)` : 'none';
		// translate3d forces a compositor layer → the slide runs on the GPU,
		// not the main thread, which is what keeps it jitter-free under load.
		el.style.transform = `translate3d(${x}px, 0, 0)`;
	}

	const sw = (): number => window.innerWidth;

	// ── Layout on every stack change ───────────────────────────────────────────
	// Only a *push* needs imperative work here: slide the new top in and push
	// the previous top to its depth position. A pop is animated by dismissTop()
	// (or, for reset(), the layers just unmount).
	let prevLen = 0;
	$effect(() => {
		const len = navStack.entries.length;
		untrack(() => {
			if (len > prevLen && !animating) {
				const entries = navStack.entries;
				const topKey = entries[len - 1]!.key;
				const belowKey = len >= 2 ? entries[len - 2]!.key : null;
				// Deep layers (already parked) snap to the depth offset.
				for (let i = 0; i < len - 2; i++) setX(entries[i]!.key, -sw() * PUSH, 0);
				// register() placed the new top off-screen — slide it in while
				// the previous top parallaxes back to the depth offset.
				requestAnimationFrame(() => {
					setX(topKey, 0, ENTER_MS);
					if (belowKey != null) setX(belowKey, -sw() * PUSH, ENTER_MS);
				});
			}
			prevLen = len;
		});
	});

	// ── Dismiss the top entry (back button / chevron / swipe release) ───────────
	function dismissTop(fromX: number): void {
		const entries = navStack.entries;
		if (entries.length === 0 || animating) return;
		animating = true;
		const top = entries[entries.length - 1]!;
		const below = entries.length >= 2 ? entries[entries.length - 2]! : null;

		leaving = top; // keep it mounted through the exit animation
		navStack.pop();

		// Continue from the current (possibly dragged) position to off-screen.
		setX(top.key, fromX, 0);
		requestAnimationFrame(() => {
			setX(top.key, sw(), EXIT_MS);
			if (below) setX(below.key, 0, EXIT_MS);
		});
		window.setTimeout(() => {
			leaving = null;
			animating = false;
		}, EXIT_MS + 20);
	}

	function backByButton(): void {
		if (navStack.depth === 0 || animating) return;
		getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
		dismissTop(0);
	}

	// ── Telegram BackButton ────────────────────────────────────────────────────
	// One handler is pushed for the whole stack; it always pops the current
	// top, so each press unwinds exactly one level. Sheets opened on top
	// (comments, composer, edit) push their own handler above this one.
	$effect(() => {
		if (navStack.depth === 0) return;
		const dispose = pushBackHandler(backByButton);
		return dispose;
	});

	// Trigger import the first time a settings entry appears in the rendered stack.
	$effect(() => {
		if (!MobileSettingsOverlay && rendered.some((e) => e.kind === 'settings')) {
			import('./MobileSettingsOverlay.svelte').then((m) => { MobileSettingsOverlay = m.default; });
		}
	});

	// ── Edge-swipe back (drives top + the layer beneath together) ───────────────
	let startX = 0;
	let startY = 0;
	let dir: 'h' | 'v' | null = null;
	let lastX = 0;
	let lastT = 0;
	let vel = 0;
	let curX = 0;
	let dragKeys: { top: number; below: number | null } | null = null;
	let moveRaf = 0;

	// DOM writes are coalesced into a single rAF per frame — touchmove can fire
	// faster than the display refresh, and writing transforms on every event is
	// what makes a drag feel like it stutters. One write per frame = buttery.
	function applyDrag(): void {
		moveRaf = 0;
		if (!dragKeys) return;
		setX(dragKeys.top, curX, 0);
		if (dragKeys.below != null) {
			const w = sw();
			setX(dragKeys.below, -w * PUSH * (1 - Math.min(1, curX / w)), 0);
		}
	}

	function onTouchStart(e: TouchEvent): void {
		if (animating || navStack.depth === 0) return;
		const t = e.touches[0];
		if (!t) return;
		startX = t.clientX;
		startY = t.clientY;
		lastX = t.clientX;
		lastT = Date.now();
		dir = null;
		vel = 0;
		curX = 0;
		const entries = navStack.entries;
		dragKeys = {
			top: entries[entries.length - 1]!.key,
			below: entries.length >= 2 ? entries[entries.length - 2]!.key : null
		};
	}

	function onTouchMove(e: TouchEvent): void {
		if (!dragKeys) return;
		const t = e.touches[0];
		if (!t) return;
		const dx = t.clientX - startX;
		const dy = t.clientY - startY;
		if (!dir && (Math.abs(dx) > 6 || Math.abs(dy) > 6)) {
			dir = Math.abs(dx) >= Math.abs(dy) ? 'h' : 'v';
		}
		if (dir !== 'h' || dx < 0) return;
		const now = Date.now();
		const dt = now - lastT;
		if (dt > 0) vel = (t.clientX - lastX) / dt;
		lastX = t.clientX;
		lastT = now;
		curX = dx;
		// below travels from -PUSH*w (hidden) to 0 (revealed) as we drag — both
		// layers are repositioned together in the next frame (see applyDrag).
		if (!moveRaf) moveRaf = requestAnimationFrame(applyDrag);
	}

	function onTouchEnd(): void {
		if (moveRaf) {
			cancelAnimationFrame(moveRaf);
			moveRaf = 0;
		}
		if (!dragKeys) return;
		const keys = dragKeys;
		dragKeys = null;
		if (dir !== 'h' || curX <= 0) return;
		if (curX > sw() * DISMISS_RATIO || vel > VELOCITY_PX_MS) {
			getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
			dismissTop(curX);
		} else {
			// snap back
			setX(keys.top, 0, SNAP_MS);
			if (keys.below != null) setX(keys.below, -sw() * PUSH, SNAP_MS);
		}
		curX = 0;
	}

	function isTop(i: number): boolean {
		return i === rendered.length - 1;
	}
</script>

{#each rendered as entry, i (entry.key)}
	<div
		class="layer"
		class:interactive={isTop(i) && entry !== leaving}
		style="z-index: {200 + i};"
		use:register={entry.key}
		use:registerSheet
		ontouchstart={isTop(i) && entry !== leaving ? onTouchStart : undefined}
		ontouchmove={isTop(i) && entry !== leaving ? onTouchMove : undefined}
		ontouchend={isTop(i) && entry !== leaving ? onTouchEnd : undefined}
	>
		{#if entry.kind === 'profile'}
			<ProfileOverlayView username={entry.username} onBack={backByButton} />
		{:else if entry.kind === 'connections'}
			<ConnectionsOverlayView
				username={entry.username}
				tab={entry.tab}
				onBack={backByButton}
			/>
		{:else if entry.kind === 'settings' && MobileSettingsOverlay}
			<MobileSettingsOverlay onBack={backByButton} />
		{/if}
	</div>
{/each}

<style>
	.layer {
		position: fixed;
		inset: 0;
		z-index: 200;
		background: var(--tg-bg, var(--color-bg, #0f0f0f));
		will-change: transform;
		transform: translate3d(0, 0, 0);
		display: flex;
		flex-direction: column;
		overflow: hidden;
		pointer-events: none;
	}

	.layer.interactive {
		pointer-events: auto;
	}
</style>
