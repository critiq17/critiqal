<script lang="ts">
  import { tabStore } from '$lib/stores/mobile-tab.store.svelte';
  import type { MobileTab } from '$lib/stores/mobile-tab.store.svelte';
  import { openCompose } from '$lib/stores/compose.store.svelte';
  import { getTelegramWebApp } from '$lib/telegram';

  function selectTab(tab: MobileTab): void {
    tabStore.active = tab;
    getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
  }

  function handleCompose(): void {
    getTelegramWebApp()?.HapticFeedback.impactOccurred('medium');
    openCompose();
  }
</script>

<nav class="nav-pill" aria-label="Main navigation">
  <button
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
    <span class="dot" class:hidden={tabStore.active !== 'feed'}></span>
  </button>

  <button
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
    <span class="dot" class:hidden={tabStore.active !== 'explore'}></span>
  </button>

  <!-- Centre compose button — not a tab, an action -->
  <button class="compose-btn" aria-label="Create post" onclick={handleCompose}>
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M12 20h9"/>
      <path d="M16.5 3.5a2.121 2.121 0 013 3L7 19l-4 1 1-4L16.5 3.5z"/>
    </svg>
  </button>

  <button
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
    <span class="dot" class:hidden={tabStore.active !== 'profile'}></span>
  </button>
</nav>

<style>
  .nav-pill {
    position: fixed;
    /* In fullscreen mode use --tg-content-bottom for Telegram UI overlap at bottom.
       Falls back to Telegram SDK CSS var, then device safe area. */
    bottom: calc(16px + var(--tg-content-bottom, var(--tg-content-safe-area-inset-bottom, env(safe-area-inset-bottom, 0px))));
    left: 50%;
    transform: translateX(-50%);
    width: fit-content;
    min-width: 200px;
    background: rgba(18, 18, 18, 0.85);
    backdrop-filter: blur(20px) saturate(180%);
    -webkit-backdrop-filter: blur(20px) saturate(180%);
    border: 1px solid rgba(255, 255, 255, 0.08);
    border-radius: 28px;
    box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
    padding: 10px 24px;
    display: flex;
    gap: 32px;
    align-items: center;
    z-index: 100;
  }

  .tab-btn {
    background: none;
    border: none;
    cursor: pointer;
    padding: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 4px;
    min-width: 44px;
    min-height: 44px;
    justify-content: center;
    transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  .tab-btn svg {
    transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  }

  .tab-btn.active svg {
    stroke: #f0f0f0;
  }

  .tab-btn:not(.active) svg {
    stroke: rgba(255, 255, 255, 0.4);
  }

  .dot {
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--tg-accent, #e05252);
    transition: opacity 0.2s ease;
  }

  .dot.hidden {
    opacity: 0;
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
    min-width: 44px;
    min-height: 44px;
    justify-content: center;
    flex-shrink: 0;
    /* Slightly brighter than inactive tabs to give it subtle prominence
       without being the jarring accent-colour button it was before */
    color: rgba(255, 255, 255, 0.75);
    transition: transform 0.18s cubic-bezier(0.34, 1.56, 0.64, 1),
                color 0.15s ease;
    -webkit-tap-highlight-color: transparent;
  }

  .compose-btn:active {
    transform: scale(0.88);
    color: #fff;
  }
</style>
