<script lang="ts">
  import { onMount } from 'svelte';
  import { tabStore } from '$lib/stores/mobile-tab.store.svelte';
  import type { MobileTab } from '$lib/stores/mobile-tab.store.svelte';
  import { openCompose } from '$lib/stores/compose.store.svelte';
  import { getTelegramWebApp } from '$lib/telegram';
  import { elasticDrag } from '$lib/actions/elasticDrag';

  let navEl: HTMLElement | undefined = $state();
  // Refs to the three real tab buttons, keyed by tab. The compose button is
  // an action, not a tab, so it never owns the indicator.
  const tabEls: Partial<Record<MobileTab, HTMLButtonElement>> = {};

  // Geometry of the sliding glass pill, measured from the active tab button
  // relative to the nav (which is the offset parent: position: fixed).
  let ind = $state({ x: 0, y: 0, w: 0, h: 0, ready: false });

  function measureIndicator(): void {
    const el = tabEls[tabStore.active];
    if (!el) return;
    ind = {
      x: el.offsetLeft,
      y: el.offsetTop,
      w: el.offsetWidth,
      h: el.offsetHeight,
      ready: true
    };
  }

  // Re-measure whenever the active tab changes (effects run post-DOM-update).
  $effect(() => {
    void tabStore.active;
    measureIndicator();
  });

  onMount(() => {
    measureIndicator();
    const ro = new ResizeObserver(() => measureIndicator());
    if (navEl) ro.observe(navEl);
    return () => ro.disconnect();
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
  aria-label="Main navigation"
  use:elasticDrag={{
    axis: 'free',
    stretch: 0.17,
    stretchOrigin: 'center',
    stiffness: 240,
    damping: 13
  }}
>
  <span
    class="indicator"
    class:ready={ind.ready}
    aria-hidden="true"
    style:width="{ind.w}px"
    style:height="{ind.h}px"
    style:transform="translate({ind.x}px, {ind.y}px)"
  ></span>

  <button
    bind:this={tabEls.feed}
    class="tab-btn"
    class:active={tabStore.active === 'feed'}
    aria-label="Feed"
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
    aria-label="Explore"
    aria-current={tabStore.active === 'explore' ? 'page' : undefined}
    onclick={() => selectTab('explore')}
  >
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <circle cx="11" cy="11" r="8"/>
      <line x1="21" y1="21" x2="16.65" y2="16.65"/>
    </svg>
  </button>

  <!-- Centre compose button — not a tab, an action -->
  <button class="compose-btn" aria-label="Create post" onclick={handleCompose}>
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M12 20h9"/>
      <path d="M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/>
    </svg>
  </button>

  <button
    bind:this={tabEls.profile}
    class="tab-btn"
    class:active={tabStore.active === 'profile'}
    aria-label="Profile"
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
    /* In fullscreen mode use --tg-content-bottom for Telegram UI overlap at bottom.
       Falls back to Telegram SDK CSS var, then device safe area. */
    bottom: calc(16px + var(--tg-content-bottom, var(--tg-content-safe-area-inset-bottom, env(safe-area-inset-bottom, 0px))));
    /* Transform-free centering — elasticDrag owns `transform`. */
    left: 0;
    right: 0;
    margin-inline: auto;
    width: fit-content;
    min-width: 200px;
    border-radius: 32px;
    /* A touch more transparent than the default glass-soft so the dock reads
       like the Telegram example. Blur still comes from .glass-soft. Uses the
       theme-aware glass token so it inverts cleanly in light theme. */
    background: var(--glass-bg-soft);
    padding: 9px 16px;
    touch-action: none;
    display: flex;
    gap: 10px;
    align-items: center;
    z-index: 100;
  }

  /* Single shared glass pill that slides between tabs. Positioned relative to
     the nav padding box and moved via transform for a smooth spring glide. */
  .indicator {
    position: absolute;
    top: 0;
    left: 0;
    border-radius: 20px;
    background: var(--surface-tint-medium);
    box-shadow:
      inset 0 1px 0 var(--surface-tint-strong),
      inset 0 0 0 0.5px var(--surface-tint-soft);
    opacity: 0;
    pointer-events: none;
    z-index: 0;
    will-change: transform, width;
    transition:
      transform 0.42s cubic-bezier(0.34, 1.4, 0.5, 1),
      width 0.42s cubic-bezier(0.34, 1.4, 0.5, 1),
      height 0.42s cubic-bezier(0.34, 1.4, 0.5, 1),
      opacity 0.2s ease;
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
    -webkit-tap-highlight-color: transparent;
  }

  .tab-btn svg {
    width: 26px;
    height: 26px;
    transition:
      stroke 0.18s ease,
      transform 0.18s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  .tab-btn.active svg {
    stroke: var(--color-text-primary);
  }

  .tab-btn:not(.active) svg {
    stroke: var(--text-quaternary);
  }

  .tab-btn:active svg {
    transform: scale(0.9);
  }

  @media (prefers-reduced-motion: reduce) {
    .indicator {
      transition: opacity 0.2s ease;
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
    /* Slightly brighter than inactive tabs to give it subtle prominence
       without being the jarring accent-colour button it was before */
    color: var(--text-secondary-2);
    transition: transform 0.18s cubic-bezier(0.34, 1.56, 0.64, 1),
                color 0.15s ease;
    -webkit-tap-highlight-color: transparent;
  }

  .compose-btn:active {
    transform: scale(0.88);
    color: var(--color-text-primary);
  }
</style>
