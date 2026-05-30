<script lang="ts">
  import { fly } from 'svelte/transition';

  interface Props {
    message: string | null;
    tone?: 'success' | 'error';
  }

  let { message, tone = 'success' }: Props = $props();
</script>

{#if message}
  <div class="toast" class:error={tone === 'error'} role="status" transition:fly={{ y: 12, duration: 180 }}>
    {message}
  </div>
{/if}

<style>
  .toast {
    position: fixed;
    bottom: 1.5rem;
    left: 50%;
    transform: translateX(-50%);
    z-index: 1100;
    padding: 0.625rem 1.125rem;
    border-radius: var(--radius-full);
    background: var(--color-surface-raised);
    border: 1px solid var(--color-border);
    color: var(--color-text-primary);
    font-size: 0.875rem;
    box-shadow: var(--shadow-md);
  }
  .toast.error {
    border-color: var(--color-accent);
    color: var(--color-accent);
  }
</style>
