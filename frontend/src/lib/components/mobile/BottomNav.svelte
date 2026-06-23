<script lang="ts">
  import { onMount } from 'svelte';
  import { tabStore } from '$lib/stores/mobile-tab.store.svelte';
  import type { MobileTab } from '$lib/stores/mobile-tab.store.svelte';
  import { openCompose } from '$lib/stores/compose.store.svelte';
  import { getTelegramWebApp } from '$lib/telegram';
  import { elasticDrag } from '$lib/actions/elasticDrag';
  import { reducedMotion } from '$lib/ui/reducedMotion.svelte';
  import { t } from '$lib/i18n';

  let navEl: HTMLElement | undefined = $state();
  let indEl: HTMLSpanElement | undefined = $state();
  const tabEls: Partial<Record<MobileTab, HTMLButtonElement>> = {};

  const REF_W = 100;

  // Spring physics for the active-tab indicator. Target values come from
  // the measured button; the spring integrates toward them and clamps
  // against the nav's inner walls so overshoot can never leave the pill —
  // any energy past a wall is absorbed (soft thud), the rest springs back.
  const K = 280; // stiffness
  const C = 18;  // damping
  const WALL_ABSORB = 0.28; // 0 = perfectly inelastic, 1 = full bounce

  let ready = $state(false);
  let curX = 0, curY = 0, curSx = 1, curH = 0;
  let velX = 0, velY = 0, velSx = 0;
  let tgtX = 0, tgtY = 0, tgtSx = 1, tgtH = 0;
  let lastT = 0;
  let raf = 0;

  function applyTransform(): void {
    if (!indEl) return;
    indEl.style.transform = `translate(${curX}px, ${curY}px) scaleX(${curSx})`;
    indEl.style.height = `${curH}px`;
  }

  function step(now: number): void {
    if (!indEl || !navEl) {
      raf = 0;
      return;
    }
    const dt = Math.min((now - lastT) / 1000, 0.05);
    lastT = now;

    velX += (-K * (curX - tgtX) - C * velX) * dt;
    velY += (-K * (curY - tgtY) - C * velY) * dt;
    velSx += (-K * (curSx - tgtSx) - C * velSx) * dt;

    curX += velX * dt;
    curY += velY * dt;
    curSx += velSx * dt;
    curH += (tgtH - curH) * Math.min(dt * 14, 1);

    // Wall collision: keep the indicator strictly within the nav padding-box.
    // offsetLeft already starts at the padding edge, so x=0 is the inner-left
    // wall and clientWidth - visualW is the inner-right wall.
    const visualW = curSx * REF_W;
    const minX = 0;
    const maxX = navEl.clientWidth - visualW;
    if (curX < minX) {
      curX = minX;
      if (velX < 0) velX = -velX * WALL_ABSORB;
    } else if (curX > maxX) {
      curX = maxX;
      if (velX > 0) velX = -velX * WALL_ABSORB;
    }

    applyTransform();

    const dist = Math.abs(curX - tgtX) + Math.abs(curY - tgtY) + Math.abs(curSx - tgtSx) * REF_W;
    const vel = Math.abs(velX) + Math.abs(velY) + Math.abs(velSx) * REF_W;
    if (dist > 0.4 || vel > 2) {
      raf = requestAnimationFrame(step);
    } else {
      curX = tgtX; curY = tgtY; curSx = tgtSx; curH = tgtH;
      velX = velY = velSx = 0;
      applyTransform();
      raf = 0;
    }
  }

  function measureIndicator(): void {
    const el = tabEls[tabStore.active];
    if (!el) return;
    tgtX = el.offsetLeft;
    tgtY = el.offsetTop;
    tgtSx = el.offsetWidth / REF_W;
    tgtH = el.offsetHeight;
    if (!ready) {
      curX = tgtX; curY = tgtY; curSx = tgtSx; curH = tgtH;
      velX = velY = velSx = 0;
      applyTransform();
      ready = true;
      return;
    }
    if (reducedMotion.value) {
      curX = tgtX; curY = tgtY; curSx = tgtSx; curH = tgtH;
      velX = velY = velSx = 0;
      applyTransform();
      return;
    }
    if (!raf) {
      lastT = performance.now();
      raf = requestAnimationFrame(step);
    }
  }

  $effect(() => {
    void tabStore.active;
    measureIndicator();
  });

  onMount(() => {
    measureIndicator();
    const ro = new ResizeObserver(() => measureIndicator());
    if (navEl) ro.observe(navEl);
    return () => {
      ro.disconnect();
      if (raf) cancelAnimationFrame(raf);
    };
  });

  function selectTab(tab: MobileTab): void {
    tabStore.active = tab;
    getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
  }

  function handleCompose(): void {
    getTelegramWebApp()?.HapticFeedback.impactOccurred('medium');
    openCompose();
  }
</script>

<nav
  bind:this={navEl}
  class="nav-pill glass glass-soft"
  aria-label={t('nav.feed')}
  use:elasticDrag={{
    axis: 'free',
    stretch: 0.34,
    pinned: true,
    stretchOrigin: 'center',
    stiffness: 180,
    damping: 10
  }}
>
  <span
    bind:this={indEl}
    class="indicator"
    class:ready
    aria-hidden="true"
    style:width="{REF_W}px"
  ></span>

  <button
    bind:this={tabEls.feed}
    class="tab-btn"
    class:active={tabStore.active === 'feed'}
    aria-label={t('nav.feed')}
    aria-current={tabStore.active === 'feed' ? 'page' : undefined}
    onclick={() => selectTab('feed')}
  >
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"/>
      <polyline points="9 22 9 12 15 12 15 22"/>
    </svg>
  </button>

  <button
    bind:this={tabEls.explore}
    class="tab-btn"
    class:active={tabStore.active === 'explore'}
    aria-label={t('nav.explore')}
    aria-current={tabStore.active === 'explore' ? 'page' : undefined}
    onclick={() => selectTab('explore')}
  >
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <circle cx="11" cy="11" r="8"/>
      <line x1="21" y1="21" x2="16.65" y2="16.65"/>
    </svg>
  </button>

  <button
    bind:this={tabEls.events}
    class="tab-btn"
    class:active={tabStore.active === 'events'}
    aria-label={t('nav.events')}
    aria-current={tabStore.active === 'events' ? 'page' : undefined}
    onclick={() => selectTab('events')}
  >
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
      <line x1="16" y1="2" x2="16" y2="6"/>
      <line x1="8" y1="2" x2="8" y2="6"/>
      <line x1="3" y1="10" x2="21" y2="10"/>
    </svg>
  </button>

  <button class="compose-btn" aria-label={t('post.composeTitle')} onclick={handleCompose}>
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M12 20h9"/>
      <path d="M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/>
    </svg>
  </button>

  <button
    bind:this={tabEls.profile}
    class="tab-btn"
    class:active={tabStore.active === 'profile'}
    aria-label={t('nav.profile')}
    aria-current={tabStore.active === 'profile' ? 'page' : undefined}
    onclick={() => selectTab('profile')}
  >
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
      <circle cx="12" cy="7" r="4"/>
    </svg>
  </button>
</nav>

<style>
  .nav-pill {
    position: fixed;
    bottom: calc(16px + var(--tg-content-bottom, var(--tg-content-safe-area-inset-bottom, env(safe-area-inset-bottom, 0px))));
    left: 0;
    right: 0;
    margin-inline: auto;
    width: fit-content;
    min-width: 200px;
    border-radius: 32px;
    /* More transparent than default glass-soft — the menu reads as a thin
       frosted film, not a panel. backdrop-filter inherited from .glass. */
    background: rgba(20, 20, 22, 0.32);
    backdrop-filter: blur(calc(var(--glass-blur) + 4px)) saturate(var(--glass-saturate));
    -webkit-backdrop-filter: blur(calc(var(--glass-blur) + 4px)) saturate(var(--glass-saturate));
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.08),
      0 12px 32px -8px rgba(0, 0, 0, 0.45),
      0 2px 6px rgba(0, 0, 0, 0.25);
    padding: 9px 16px;
    z-index: 100;
    display: flex;
    gap: 10px;
    align-items: center;
    touch-action: none;
    transform-origin: center;
  }

  /* Sliding pill behind the active tab. Spring-driven via rAF in the script:
     CSS transition only animates the opacity fade-in. left/top stay 0 —
     offsetLeft/offsetTop already include the nav's padding, so adding any
     here would double-count and shift the indicator off the icon. */
  .indicator {
    position: absolute;
    top: 0;
    left: 0;
    border-radius: 20px;
    background: rgba(255, 255, 255, 0.22);
    box-shadow:
      inset 0 1px 0 rgba(255, 255, 255, 0.32),
      inset 0 0 0 0.5px rgba(255, 255, 255, 0.12),
      0 2px 6px rgba(0, 0, 0, 0.22);
    opacity: 0;
    pointer-events: none;
    z-index: 0;
    transform-origin: left center;
    will-change: transform;
    transition: opacity var(--duration-micro) var(--ease-out-quart);
  }

  .indicator.ready {
    opacity: 1;
  }

  .tab-btn {
    position: relative;
    z-index: 1;
    background: none;
    border: none;
    cursor: pointer;
    padding: 13px 22px;
    border-radius: 20px;
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 48px;
    min-height: 48px;
    transform-origin: center;
    transition: transform var(--duration-press) var(--ease-out-quart);
    -webkit-tap-highlight-color: transparent;
  }

  .tab-btn svg {
    width: 26px;
    height: 26px;
    transition: stroke var(--duration-micro) var(--ease-out-quart);
  }

  .tab-btn.active svg {
    stroke: var(--color-text-primary);
  }

  .tab-btn:not(.active) svg {
    stroke: var(--text-quaternary);
  }

  .tab-btn:active {
    transform: scale(0.92);
  }

  @media (prefers-reduced-motion: reduce) {
    .nav-pill {
      transform: none !important;
    }
    .indicator {
      transition: opacity var(--duration-micro) var(--ease-out-quart);
    }
    .tab-btn,
    .compose-btn {
      transition: none;
    }
    .tab-btn:active,
    .compose-btn:active {
      transform: none;
    }
  }

  /* Reduced transparency: drop the blur and settle on an opaque dark panel
     that matches the menu's normal dark-frost read (it is intentionally dark
     in both themes). */
  @media (prefers-reduced-transparency: reduce) {
    .nav-pill {
      backdrop-filter: none;
      -webkit-backdrop-filter: none;
      background: #1f1f23;
    }
  }

  .compose-btn {
    background: none;
    border: none;
    cursor: pointer;
    padding: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    min-width: 48px;
    min-height: 48px;
    justify-content: center;
    flex-shrink: 0;
    color: var(--text-secondary-2);
    transform-origin: center;
    transition:
      transform var(--duration-press) var(--ease-out-quart),
      color var(--duration-micro) var(--ease-out-quart);
    -webkit-tap-highlight-color: transparent;
  }

  .compose-btn:active {
    transform: scale(0.92);
    color: var(--color-text-primary);
  }
</style>
