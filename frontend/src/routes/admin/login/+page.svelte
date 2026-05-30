<script lang="ts">
  import { adminService } from '$lib/services';
  import { adminAuthStore } from '$lib/stores/admin-auth.store.svelte';
  import { goto } from '$app/navigation';
  import { ApiError } from '$lib/types';

  let username = $state('');
  let password = $state('');
  let challengeToken = $state<string | null>(null);
  let code = $state('');
  let error = $state<string | null>(null);
  let loading = $state(false);

  async function submitCredentials(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    error = null;
    loading = true;
    try {
      const result = await adminService.login(username, password);
      challengeToken = result.challengeToken;
    } catch (e) {
      // 404 hides whether the account is an admin; show a generic message.
      error = e instanceof ApiError && e.isNotFound ? 'Invalid credentials' : 'Something went wrong';
    } finally {
      loading = false;
    }
  }

  async function submitCode(event: SubmitEvent): Promise<void> {
    event.preventDefault();
    if (challengeToken === null) return;
    error = null;
    loading = true;
    try {
      await adminService.verifyTwoFactor(challengeToken, code);
      adminAuthStore.setAuthed();
      void goto('/admin');
    } catch (e) {
      error = e instanceof ApiError && e.isUnauthorized ? 'Invalid code' : 'Invalid credentials';
    } finally {
      loading = false;
    }
  }
</script>

<svelte:head><title>critiqal admin</title></svelte:head>

<div class="admin-login">
  <div class="card">
    <h1>critiqal <span class="dim">admin</span></h1>

    {#if error}
      <p class="error" role="alert">{error}</p>
    {/if}

    {#if challengeToken === null}
      <form onsubmit={submitCredentials}>
        <input bind:value={username} placeholder="Username" autocomplete="username" aria-label="Username" />
        <input
          bind:value={password}
          type="password"
          placeholder="Password"
          autocomplete="current-password"
          aria-label="Password"
        />
        <button type="submit" disabled={loading || !username || !password}>
          {loading ? 'Checking…' : 'Continue'}
        </button>
      </form>
    {:else}
      <form onsubmit={submitCode}>
        <p class="hint">Enter the 6-digit code from your authenticator.</p>
        <input
          bind:value={code}
          placeholder="2FA code"
          inputmode="numeric"
          autocomplete="one-time-code"
          aria-label="Two-factor code"
        />
        <button type="submit" disabled={loading || !code}>
          {loading ? 'Verifying…' : 'Verify'}
        </button>
      </form>
    {/if}
  </div>
</div>

<style>
  .admin-login {
    min-height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 2rem;
  }
  .card {
    width: 100%;
    max-width: 340px;
    display: flex;
    flex-direction: column;
    gap: 1.25rem;
    padding: 2rem;
    background: var(--color-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
  }
  h1 {
    margin: 0;
    font-size: 1.25rem;
    font-weight: 700;
  }
  .dim {
    color: var(--color-text-secondary);
    font-weight: 500;
  }
  form {
    display: flex;
    flex-direction: column;
    gap: 0.75rem;
  }
  .hint {
    margin: 0;
    font-size: 0.8125rem;
    color: var(--color-text-secondary);
  }
  input {
    padding: 0.75rem 1rem;
    background: var(--color-bg);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    color: var(--color-text-primary);
    font-size: 0.9375rem;
    font-family: inherit;
  }
  input:focus {
    outline: none;
    border-color: var(--color-accent);
  }
  button {
    padding: 0.75rem;
    background: var(--color-accent);
    border: none;
    border-radius: var(--radius-md);
    color: #fff;
    cursor: pointer;
    font-weight: 600;
    font-family: inherit;
  }
  button:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }
  .error {
    margin: 0;
    color: var(--color-accent);
    font-size: 0.875rem;
  }
</style>
