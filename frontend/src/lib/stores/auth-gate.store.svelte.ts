// Drives the "sign up to continue" prompt shown when a guest tries a write
// action (like, follow, comment, search). Single global instance so any
// feature can trigger it without prop-drilling.

export type GateReason = 'like' | 'follow' | 'comment' | 'search' | 'compose' | 'generic';

interface GateState {
  open: boolean;
  reason: GateReason;
}

function createStore() {
  let state = $state<GateState>({ open: false, reason: 'generic' });

  function open(reason: GateReason = 'generic'): void {
    state = { open: true, reason };
  }

  function close(): void {
    state = { ...state, open: false };
  }

  return {
    get isOpen() {
      return state.open;
    },
    get reason() {
      return state.reason;
    },
    open,
    close,
  };
}

export const authGate = createStore();
