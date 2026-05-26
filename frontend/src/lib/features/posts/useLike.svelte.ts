import { authStore } from '$lib/stores/auth.store.svelte';
import { authGate } from '$lib/stores/auth-gate.store.svelte';
import type { LikeResponse } from '$lib/types';

const POP_MS = 360;

/**
 * Reusable optimistic like state for any likeable entity (post or comment).
 * The toggle request is injected so this class stays decoupled from the API
 * surface — the same logic drives post likes and comment likes.
 */
export class UseLike {
  liked = $state(false);
  count = $state(0);
  pending = $state(false);
  popping = $state(false);

  private readonly toggleRequest: () => Promise<LikeResponse>;

  constructor(
    toggleRequest: () => Promise<LikeResponse>,
    initialLiked: boolean,
    initialCount: number
  ) {
    this.toggleRequest = toggleRequest;
    this.liked = initialLiked;
    this.count = Math.max(0, initialCount);
  }

  async toggle(): Promise<void> {
    if (!authStore.isAuthenticated) {
      authGate.open('like');
      return;
    }
    if (this.pending) return;

    const prevLiked = this.liked;
    const prevCount = this.count;
    const nextLiked = !prevLiked;

    // Optimistic update
    this.liked = nextLiked;
    this.count = Math.max(0, prevCount + (nextLiked ? 1 : -1));
    if (nextLiked) {
      this.popping = true;
      setTimeout(() => {
        this.popping = false;
      }, POP_MS);
    }

    this.pending = true;
    try {
      const res = await this.toggleRequest();
      // Reconcile with the server's authoritative state.
      this.liked = res.likedByMe;
      this.count = Math.max(0, res.count);
    } catch {
      // Rollback
      this.liked = prevLiked;
      this.count = prevCount;
    } finally {
      this.pending = false;
    }
  }
}
