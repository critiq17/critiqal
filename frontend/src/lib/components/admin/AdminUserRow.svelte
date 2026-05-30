<script lang="ts">
  import type { AdminUser } from '$lib/types';
  import Avatar from '$lib/ui/Avatar.svelte';
  import BadgePillRow from '$lib/components/badges/BadgePillRow.svelte';

  interface Props {
    user: AdminUser;
    onselect?: (user: AdminUser) => void;
  }

  let { user, onselect }: Props = $props();
</script>

<button class="row" onclick={() => onselect?.(user)} aria-label={`Manage ${user.username}`}>
  <Avatar src={user.avatarUrl} name={user.name ?? user.username} size={44} />
  <div class="meta">
    <span class="name">{user.name ?? user.username}</span>
    <span class="username">@{user.username}</span>
    {#if user.badges.length > 0}
      <div class="badges"><BadgePillRow badges={user.badges} max={4} /></div>
    {/if}
  </div>
  <span class="chevron" aria-hidden="true">›</span>
</button>

<style>
  .row {
    display: flex;
    align-items: center;
    gap: 0.75rem;
    width: 100%;
    padding: 0.75rem;
    background: transparent;
    border: none;
    border-radius: var(--radius-md);
    cursor: pointer;
    text-align: left;
    font-family: inherit;
  }
  .row:hover {
    background: var(--color-surface-raised);
  }
  .meta {
    display: flex;
    flex-direction: column;
    gap: 0.2rem;
    min-width: 0;
    flex: 1;
  }
  .name {
    color: var(--color-text-primary);
    font-weight: 600;
  }
  .username {
    color: var(--color-text-secondary);
    font-size: 0.8125rem;
  }
  .badges {
    margin-top: 0.25rem;
  }
  .chevron {
    color: var(--color-text-muted);
    font-size: 1.25rem;
    flex-shrink: 0;
  }
</style>
