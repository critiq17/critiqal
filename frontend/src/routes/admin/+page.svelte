<script lang="ts">
  import { adminService } from '$lib/services';
  import type { AdminUser, AdminBadge, Post } from '$lib/types';
  import AdminUserRow from '$lib/components/admin/AdminUserRow.svelte';
  import AdminPostRow from '$lib/components/admin/AdminPostRow.svelte';
  import GrantBadgePanel from '$lib/components/admin/GrantBadgePanel.svelte';
  import AdminToast from '$lib/components/admin/AdminToast.svelte';

  const DEBOUNCE_MS = 300;

  type Tab = 'people' | 'posts';

  let activeTab = $state<Tab>('people');
  let query = $state('');

  let users = $state<AdminUser[]>([]);
  let posts = $state<Post[]>([]);
  let loading = $state(false);
  let errored = $state(false);

  let allBadges = $state<AdminBadge[]>([]);
  let selected = $state<AdminUser | null>(null);

  let toastMessage = $state<string | null>(null);
  let toastTone = $state<'success' | 'error'>('success');
  let toastTimer: ReturnType<typeof setTimeout> | undefined;

  let debounceTimer: ReturnType<typeof setTimeout> | undefined;
  let loadedKey: string | null = null;

  // Grantable badge list is static for the session; fetch once.
  $effect(() => {
    adminService
      .listBadges()
      .then((b) => {
        allBadges = b;
      })
      .catch(() => {
        /* non-fatal: panel simply shows no options */
      });
  });

  // Debounced fetch keyed by tab+query so switching tabs refetches correctly.
  $effect(() => {
    const tab = activeTab;
    const q = query;
    const key = `${tab}:${q}`;
    if (key === loadedKey) return;
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
      void fetchResults(tab, q);
      loadedKey = key;
    }, DEBOUNCE_MS);
  });

  async function fetchResults(tab: Tab, q: string): Promise<void> {
    loading = true;
    errored = false;
    try {
      if (tab === 'people') {
        const page = await adminService.searchUsers(q);
        users = page.content;
      } else {
        const page = await adminService.searchPosts(q);
        posts = page.content;
      }
    } catch {
      errored = true;
    } finally {
      loading = false;
    }
  }

  function selectUser(user: AdminUser): void {
    selected = user;
  }

  function onUserChanged(updated: AdminUser): void {
    // Reflect badge changes in the list without a full refetch.
    users = users.map((u) => (u.id === updated.id ? updated : u));
    selected = updated;
  }

  function showToast(message: string, tone: 'success' | 'error'): void {
    toastMessage = message;
    toastTone = tone;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      toastMessage = null;
    }, 2600);
  }

  function retry(): void {
    loadedKey = null;
    void fetchResults(activeTab, query);
    loadedKey = `${activeTab}:${query}`;
  }
</script>

<svelte:head><title>critiqal admin — dashboard</title></svelte:head>

<div class="dashboard" class:with-panel={selected !== null && activeTab === 'people'}>
  <section class="main-col">
    <div class="tab-bar" role="tablist">
      <button
        class="tab"
        class:active={activeTab === 'people'}
        role="tab"
        aria-selected={activeTab === 'people'}
        onclick={() => (activeTab = 'people')}
      >
        People
      </button>
      <button
        class="tab"
        class:active={activeTab === 'posts'}
        role="tab"
        aria-selected={activeTab === 'posts'}
        onclick={() => (activeTab = 'posts')}
      >
        Posts
      </button>
    </div>

    <div class="search">
      <input
        type="search"
        bind:value={query}
        placeholder={activeTab === 'people' ? 'Search people…' : 'Search posts…'}
        aria-label="Search"
        autocomplete="off"
        spellcheck="false"
      />
    </div>

    <div class="results" role="tabpanel">
      {#if loading}
        <p class="state">Loading…</p>
      {:else if errored}
        <p class="state">
          Something went wrong. <button class="link" onclick={retry}>Retry</button>
        </p>
      {:else if activeTab === 'people'}
        {#if users.length === 0}
          <p class="state">No people found.</p>
        {:else}
          <ul class="list">
            {#each users as user (user.id)}
              <li><AdminUserRow {user} onselect={selectUser} /></li>
            {/each}
          </ul>
        {/if}
      {:else if posts.length === 0}
        <p class="state">No posts found.</p>
      {:else}
        <div class="post-list">
          {#each posts as post (post.id)}
            <AdminPostRow {post} />
          {/each}
        </div>
      {/if}
    </div>
  </section>

  {#if selected !== null && activeTab === 'people'}
    <GrantBadgePanel
      user={selected}
      badges={allBadges}
      onclose={() => (selected = null)}
      onchanged={onUserChanged}
      onfeedback={showToast}
    />
  {/if}
</div>

<AdminToast message={toastMessage} tone={toastTone} />

<style>
  .dashboard {
    display: grid;
    grid-template-columns: 1fr;
    height: 100%;
  }
  .dashboard.with-panel {
    grid-template-columns: minmax(0, 1fr) minmax(320px, 380px);
  }
  .main-col {
    display: flex;
    flex-direction: column;
    min-width: 0;
    max-width: 42rem;
    width: 100%;
    margin: 0 auto;
    padding: 1.5rem;
    overflow-y: auto;
  }
  .tab-bar {
    display: flex;
    gap: 0.5rem;
    border-bottom: 1px solid var(--color-border);
    margin-bottom: 1rem;
  }
  .tab {
    background: none;
    border: none;
    border-bottom: 2px solid transparent;
    color: var(--color-text-secondary);
    padding: 0.625rem 0.25rem;
    margin-bottom: -1px;
    cursor: pointer;
    font-size: 0.9375rem;
    font-weight: 500;
    font-family: inherit;
  }
  .tab.active {
    color: var(--color-text-primary);
    border-bottom-color: var(--color-accent);
    font-weight: 600;
  }
  .search {
    margin-bottom: 1rem;
  }
  .search input {
    width: 100%;
    background: var(--color-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-full);
    padding: 0.5625rem 1rem;
    color: var(--color-text-primary);
    font-size: 0.9375rem;
    font-family: inherit;
    outline: none;
  }
  .search input:focus {
    border-color: var(--color-accent);
  }
  .results {
    flex: 1;
  }
  .list {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 0.125rem;
  }
  .post-list {
    display: flex;
    flex-direction: column;
  }
  .state {
    color: var(--color-text-secondary);
    font-size: 0.875rem;
    padding: 1rem 0.25rem;
  }
  .link {
    background: none;
    border: none;
    color: var(--color-accent);
    cursor: pointer;
    font: inherit;
    padding: 0;
  }

  @media (max-width: 720px) {
    .dashboard.with-panel {
      grid-template-columns: 1fr;
    }
  }
</style>
