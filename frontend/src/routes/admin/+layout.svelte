<script lang="ts">
  import { onMount } from 'svelte';
  import { page } from '$app/stores';
  import { goto } from '$app/navigation';
  import { adminAuthStore } from '$lib/stores/admin-auth.store.svelte';
  import type { Snippet } from 'svelte';

  interface Props {
    children: Snippet;
  }

  let { children }: Props = $props();

  const isLoginRoute = $derived($page.url.pathname.startsWith('/admin/login'));
  let checking = $state(true);

  onMount(async () => {
    const status = await adminAuthStore.load();
    checking = false;
    if (status === 'anon' && !isLoginRoute) {
      void goto('/admin/login');
    }
  });

  async function handleLogout(): Promise<void> {
    await adminAuthStore.logout();
    void goto('/admin/login');
  }
</script>

<!-- Full-viewport overlay: desktop admin shell, independent of the mobile app chrome. -->
<div class="admin-root">
  {#if isLoginRoute}
    {@render children()}
  {:else if checking || adminAuthStore.status === 'unknown'}
    <div class="center"><span class="loader">Loading…</span></div>
  {:else if adminAuthStore.status === 'authed'}
    <header class="admin-header">
      <span class="brand">critiqal <span class="brand-dim">admin</span></span>
      <button class="logout" onclick={handleLogout}>Log out</button>
    </header>
    <main class="admin-main">
      {@render children()}
    </main>
  {:else}
    <div class="center"><span class="loader">Redirecting…</span></div>
  {/if}
</div>

<style>
  .admin-root {
    position: fixed;
    inset: 0;
    z-index: 1000;
    background: var(--color-bg);
    color: var(--color-text-primary);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    font-family: inherit;
  }
  .admin-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0.875rem 1.5rem;
    border-bottom: 1px solid var(--color-border);
    background: var(--color-surface);
    flex-shrink: 0;
  }
  .brand {
    font-weight: 700;
    font-size: 1rem;
    letter-spacing: 0.01em;
  }
  .brand-dim {
    color: var(--color-text-secondary);
    font-weight: 500;
  }
  .logout {
    background: transparent;
    border: 1px solid var(--color-border);
    color: var(--color-text-secondary);
    padding: 0.375rem 0.875rem;
    border-radius: var(--radius-md);
    cursor: pointer;
    font-size: 0.8125rem;
    transition: color 0.15s ease, border-color 0.15s ease;
  }
  .logout:hover {
    color: var(--color-text-primary);
    border-color: var(--color-accent);
  }
  .admin-main {
    flex: 1;
    overflow-y: auto;
  }
  .center {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  .loader {
    color: var(--color-text-secondary);
    font-size: 0.875rem;
  }
</style>
