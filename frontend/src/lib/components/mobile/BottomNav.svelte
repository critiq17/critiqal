<script lang="ts">
  import { activeTab } from '$lib/stores/mobile-tab.store';
  import type { MobileTab } from '$lib/stores/mobile-tab.store';
  import { getTelegramWebApp } from '$lib/telegram';

  let currentTab = $derived($activeTab);

  function selectTab(tab: MobileTab): void {
    activeTab.set(tab);
    getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
  }
</script>

<nav class="nav-pill" aria-label="Main navigation">
  <button
    class="tab-btn"
    class:active={currentTab === 'feed'}
    aria-label="Feed"
    aria-current={currentTab === 'feed' ? 'page' : undefined}
    onclick={() => selectTab('feed')}
  >
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M3 9l9-7 9 7v11a2 2 0 01-2 2H5a2 2 0 01-2-2V9z"/>
      <polyline points="9 22 9 12 15 12 15 22"/>
    </svg>
    <span class="dot" class:hidden={currentTab !== 'feed'}></span>
  </button>

  <button
    class="tab-btn"
    class:active={currentTab === 'explore'}
    aria-label="Explore"
    aria-current={currentTab === 'explore' ? 'page' : undefined}
    onclick={() => selectTab('explore')}
  >
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <circle cx="11" cy="11" r="8"/>
      <line x1="21" y1="21" x2="16.65" y2="16.65"/>
    </svg>
    <span class="dot" class:hidden={currentTab !== 'explore'}></span>
  </button>

  <button
    class="tab-btn"
    class:active={currentTab === 'profile'}
    aria-label="Profile"
    aria-current={currentTab === 'profile' ? 'page' : undefined}
    onclick={() => selectTab('profile')}
  >
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
      <path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/>
      <circle cx="12" cy="7" r="4"/>
    </svg>
    <span class="dot" class:hidden={currentTab !== 'profile'}></span>
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
</style>
