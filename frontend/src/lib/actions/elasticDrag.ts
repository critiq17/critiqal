import { reducedMotion } from '$lib/ui/reducedMotion.svelte';

// One physics implementation for every draggable surface (menu, sheets,
// edit/settings panels). Drag tracks the pointer with rubber-band resistance;
// on release it either dismisses (single-axis, past distance/velocity) or
// springs back with real inertia — a single rAF spring, transforms only.
//
// axis 'y' | 'x' = constrained (sheets, dismiss). axis 'free' = pulls in any
// direction and bounces back (liquid-glass menu).

type Axis = 'x' | 'y' | 'free';

export interface ElasticDragOptions {
  /** Drag axis. 'free' = omnidirectional. Default 'y'. */
  axis?: Axis;
  /** Element that actually moves. Default: the node the action is on. */
  target?: HTMLElement | (() => HTMLElement | null | undefined);
  /** Past this many px in the dismiss direction → onDismiss (single-axis only). */
  dismissDistance?: number;
  /** Or past this release velocity (px/ms). */
  dismissVelocity?: number;
  /** Resolves to a dismiss. Omit for pure stretch (always springs back). */
  onDismiss?: () => void;
  /** 0..1 progress toward dismiss while dragging (e.g. fade a backdrop). */
  onProgress?: (progress: number) => void;
  /** Scale-stretch while pulling. ~0.06 lively, 0 off. */
  stretch?: number;
  /** transform-origin for the stretch (e.g. 'center', 'bottom center'). */
  stretchOrigin?: string;
  /** Spring constants. Lower damping = bouncier "jump". Default 180 / 22. */
  stiffness?: number;
  damping?: number;
  /** Only engage when this returns true (e.g. scroller is at the top). */
  canStart?: () => boolean;
  /** Only react to positive movement (down/right); negative is left to native scroll. */
  positiveOnly?: boolean;
  /** Disable interaction (still cleans up). */
  disabled?: boolean;
}

const RUBBER = 0.55; // iOS-style resistance coefficient
const ENGAGE_PX = 4; // movement before a touch counts as a drag (taps pass through)

// Diminishing-returns resistance: small pulls track ~1:1, large pulls choke.
function rubberband(delta: number, dim: number): number {
  const sign = Math.sign(delta);
  const x = Math.abs(delta);
  return sign * (1 - 1 / ((x * RUBBER) / dim + 1)) * dim;
}

export function elasticDrag(node: HTMLElement, options: ElasticDragOptions = {}) {
  let opts = options;
  const axis = (): Axis => opts.axis ?? 'y';
  const resolveTarget = (): HTMLElement => {
    const t = typeof opts.target === 'function' ? opts.target() : opts.target;
    return t ?? node;
  };
  const usesX = (): boolean => axis() === 'x' || axis() === 'free';
  const usesY = (): boolean => axis() === 'y' || axis() === 'free';

  let down = false;
  let engaged = false;
  let activePointer = 0;
  let startX = 0;
  let startY = 0;
  let ox = 0;
  let oy = 0;
  let lastX = 0;
  let lastY = 0;
  let lastT = 0;
  let vx = 0; // px/ms
  let vy = 0;
  let raf = 0;

  function paint(x: number, y: number): void {
    const el = resolveTarget();
    let transform = `translate3d(${x}px, ${y}px, 0)`;
    if (opts.stretch) {
      const w = el.offsetWidth || 1;
      const h = el.offsetHeight || 1;
      const s = opts.stretch;
      // Directional liquid stretch: the menu elongates along the pull and
      // thins slightly on the cross axis (Poisson-like), so it deforms
      // like a rubber band rather than just zooming.
      const px = Math.min(Math.abs(x) / w, 1);
      const py = Math.min(Math.abs(y) / h, 1);
      const sx = 1 + px * s - py * s * 0.35;
      const sy = 1 + py * s - px * s * 0.35;
      transform += ` scale(${sx}, ${sy})`;
    }
    el.style.transform = transform;
    if (opts.onProgress && opts.dismissDistance) {
      const d = axis() === 'x' ? x : y;
      opts.onProgress(Math.min(Math.max(d / opts.dismissDistance, 0), 1));
    }
  }

  // Vector spring — integrates X and Y together so a free drag bounces back
  // along the exact line it was thrown, with overshoot (the "jump").
  function spring(): void {
    const el = resolveTarget();
    el.style.transition = 'none';
    el.style.willChange = 'transform';
    const k = opts.stiffness ?? 180;
    const c = opts.damping ?? 22;
    let x = ox;
    let y = oy;
    let velX = vx * 1000; // px/s
    let velY = vy * 1000;
    let prev = performance.now();
    const step = (now: number): void => {
      const dt = Math.min((now - prev) / 1000, 0.064);
      prev = now;
      velX += (-k * x - c * velX) * dt;
      velY += (-k * y - c * velY) * dt;
      x += velX * dt;
      y += velY * dt;
      const settled = Math.hypot(x, y) < 0.4 && Math.hypot(velX, velY) < 4;
      if (settled) {
        ox = oy = 0;
        el.style.transform = '';
        el.style.willChange = '';
        opts.onProgress?.(0);
        raf = 0;
        return;
      }
      ox = x;
      oy = y;
      paint(x, y);
      raf = requestAnimationFrame(step);
    };
    raf = requestAnimationFrame(step);
  }

  function settle(): void {
    if (reducedMotion.value) {
      ox = oy = 0;
      resolveTarget().style.transform = '';
      opts.onProgress?.(0);
      return;
    }
    spring();
  }

  function onPointerDown(e: PointerEvent): void {
    if (opts.disabled || (e.button != null && e.button !== 0)) return;
    if (raf) {
      cancelAnimationFrame(raf);
      raf = 0;
    }
    down = true;
    engaged = false;
    activePointer = e.pointerId;
    startX = e.clientX - ox; // resume from current offset if grabbed mid-spring
    startY = e.clientY - oy;
    lastX = e.clientX;
    lastY = e.clientY;
    lastT = performance.now();
    vx = vy = 0;
  }

  function onPointerMove(e: PointerEvent): void {
    if (!down || e.pointerId !== activePointer) return;
    let rawX = usesX() ? e.clientX - startX : 0;
    let rawY = usesY() ? e.clientY - startY : 0;

    // Until the finger has clearly moved this is still a tap — let it fall
    // through to buttons/links underneath (no capture, no transform).
    if (!engaged) {
      if (Math.hypot(rawX, rawY) < ENGAGE_PX) return;
      // Pull-down-only surfaces (e.g. a scroller at its top): a downward
      // pull elasticises, anything else stays native scroll.
      const primary = axis() === 'x' ? rawX : rawY;
      if (opts.positiveOnly && primary <= 0) return;
      if (opts.canStart && !opts.canStart()) return;
      engaged = true;
      // Re-anchor at the engage point so the offset grows from 0 — without
      // this the surface jumps by the ENGAGE_PX threshold on first frame
      // (the "abrupt jerk" when the pull-down starts).
      startX = e.clientX - ox;
      startY = e.clientY - oy;
      rawX = usesX() ? e.clientX - startX : 0;
      rawY = usesY() ? e.clientY - startY : 0;
      node.setPointerCapture(e.pointerId);
      resolveTarget().style.transition = 'none';
      resolveTarget().style.willChange = 'transform';
    }

    const el = resolveTarget();
    const w = el.offsetWidth || 1;
    const h = el.offsetHeight || 1;

    // Dismiss-direction pull tracks the finger; everything else (and all
    // movement on free/stretch surfaces) gets rubber-band resistance.
    const dismissable = !!opts.onDismiss;
    ox = dismissable && axis() === 'x' && rawX > 0 ? rawX : rubberband(rawX, w);
    oy = dismissable && axis() === 'y' && rawY > 0 ? rawY : rubberband(rawY, h);
    if (opts.positiveOnly) {
      ox = Math.max(0, ox);
      oy = Math.max(0, oy);
    }

    const now = performance.now();
    const dt = now - lastT;
    if (dt > 0) {
      vx = (e.clientX - lastX) / dt;
      vy = (e.clientY - lastY) / dt;
    }
    lastX = e.clientX;
    lastY = e.clientY;
    lastT = now;
    paint(ox, oy);
  }

  function onPointerUp(e: PointerEvent): void {
    if (!down) return;
    down = false;
    if (!engaged) return; // it was a tap — nothing moved, let the click happen
    engaged = false;
    try {
      node.releasePointerCapture(e.pointerId);
    } catch {
      /* already released */
    }

    const dist = opts.dismissDistance ?? Infinity;
    const vMax = opts.dismissVelocity ?? Infinity;
    const d = axis() === 'x' ? ox : oy;
    const v = axis() === 'x' ? vx : vy;
    const shouldDismiss = !!opts.onDismiss && (d > dist || v > vMax);

    if (shouldDismiss) {
      opts.onProgress?.(1);
      opts.onDismiss?.();
      return;
    }
    settle();
  }

  node.addEventListener('pointerdown', onPointerDown);
  node.addEventListener('pointermove', onPointerMove);
  node.addEventListener('pointerup', onPointerUp);
  node.addEventListener('pointercancel', onPointerUp);
  if (opts.stretchOrigin) resolveTarget().style.transformOrigin = opts.stretchOrigin;

  return {
    update(next: ElasticDragOptions) {
      opts = next;
      if (opts.stretchOrigin) resolveTarget().style.transformOrigin = opts.stretchOrigin;
    },
    destroy() {
      if (raf) cancelAnimationFrame(raf);
      node.removeEventListener('pointerdown', onPointerDown);
      node.removeEventListener('pointermove', onPointerMove);
      node.removeEventListener('pointerup', onPointerUp);
      node.removeEventListener('pointercancel', onPointerUp);
    },
  };
}
