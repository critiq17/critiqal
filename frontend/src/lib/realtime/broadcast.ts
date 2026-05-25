// Cross-tab event bus over the BroadcastChannel API. Lets a post/like/comment
// created in one tab show up live in the others without hitting the backend.
// Falls back to a no-op in environments without BroadcastChannel (older
// Safari, some in-app webviews) — the rest of the SWR stack still keeps
// state consistent on the next revalidate.

import type { Post, Comment } from '$lib/types';

export type RealtimeEvent =
  | { type: 'post:created'; post: Post; origin: string }
  | { type: 'post:deleted'; postId: string; origin: string }
  | { type: 'comment:created'; postId: string; comment: Comment; origin: string }
  | { type: 'comment:deleted'; postId: string; commentId: string; origin: string }
  | { type: 'like:toggled'; postId: string; liked: boolean; count: number; origin: string }
  | { type: 'follow:toggled'; targetUserId: string; following: boolean; origin: string };

const CHANNEL = 'critiqal';
const ORIGIN = Math.random().toString(36).slice(2);

type Listener = (event: RealtimeEvent) => void;
const listeners = new Set<Listener>();

let channel: BroadcastChannel | null = null;

function ensureChannel(): BroadcastChannel | null {
  if (channel) return channel;
  if (typeof BroadcastChannel === 'undefined') return null;
  try {
    channel = new BroadcastChannel(CHANNEL);
    channel.onmessage = (e: MessageEvent<RealtimeEvent>) => {
      const evt = e.data;
      // Ignore our own echo — actions in this tab already mutated state.
      if (!evt || evt.origin === ORIGIN) return;
      for (const l of listeners) l(evt);
    };
    return channel;
  } catch {
    return null;
  }
}

// Distributes Omit across the union so each variant's discriminant + payload
// is preserved (a plain Omit<Union,'origin'> collapses to the intersection).
type WithoutOrigin<T> = T extends unknown ? Omit<T, 'origin'> : never;

export function emit(event: WithoutOrigin<RealtimeEvent>): void {
  const ch = ensureChannel();
  if (!ch) return;
  try {
    ch.postMessage({ ...event, origin: ORIGIN } as RealtimeEvent);
  } catch {
    // disconnected / closed — ignore
  }
}

export function subscribe(listener: Listener): () => void {
  ensureChannel();
  listeners.add(listener);
  return () => listeners.delete(listener);
}
