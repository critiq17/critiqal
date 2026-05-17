// Single source of truth for the mobile overlay navigation history.
//
// Every full-screen overlay (public profile, followers/following list,
// settings) is an entry on this stack. The OverlayHost renders the stack as
// layered screens, owns the Telegram BackButton, and runs one shared
// swipe-back gesture. Pushing/popping here is the ONLY way navigation state
// changes — components never touch the BackButton or each other directly.

export type NavEntry =
  | { key: number; kind: 'profile'; username: string }
  | { key: number; kind: 'connections'; username: string; tab: 'followers' | 'following' }
  | { key: number; kind: 'settings' };

// Distributes Omit over the union so each member is checked individually
// (a plain Omit<Union,'key'> would reject member-specific fields).
type WithoutKey<T> = T extends unknown ? Omit<T, 'key'> : never;

class NavStack {
  entries = $state<NavEntry[]>([]);
  private seq = 0;

  get depth(): number {
    return this.entries.length;
  }

  get top(): NavEntry | null {
    return this.entries.at(-1) ?? null;
  }

  private push(entry: WithoutKey<NavEntry>): void {
    this.entries = [...this.entries, { ...entry, key: ++this.seq } as NavEntry];
  }

  pushProfile(username: string): void {
    const t = this.top;
    // Don't stack the same profile twice in a row (e.g. tapping your own
    // post author while already on your profile).
    if (t?.kind === 'profile' && t.username === username) return;
    this.push({ kind: 'profile', username });
  }

  pushConnections(username: string, tab: 'followers' | 'following'): void {
    this.push({ kind: 'connections', username, tab });
  }

  pushSettings(): void {
    if (this.top?.kind === 'settings') return;
    this.push({ kind: 'settings' });
  }

  // Pop one level — the unit of the BackButton and one swipe-back gesture.
  pop(): void {
    if (this.entries.length === 0) return;
    this.entries = this.entries.slice(0, -1);
  }

  // Tear the whole stack down (e.g. switching bottom-nav tab).
  reset(): void {
    if (this.entries.length === 0) return;
    this.entries = [];
  }
}

export const navStack = new NavStack();
