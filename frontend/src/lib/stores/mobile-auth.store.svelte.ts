// In Telegram mini-app the router can't navigate to /login or /register
// because MobileLayout owns the viewport and ignores the URL. This store
// lets any feature open the in-app auth screen and preselect login vs
// register without leaving the mini-app shell.

export type AuthMode = 'login' | 'register';

interface State {
  open: boolean;
  initialMode: AuthMode;
}

function createStore() {
  let state = $state<State>({ open: false, initialMode: 'register' });

  function openWith(mode: AuthMode): void {
    state = { open: true, initialMode: mode };
  }

  function close(): void {
    state = { ...state, open: false };
  }

  return {
    get isOpen() {
      return state.open;
    },
    get initialMode() {
      return state.initialMode;
    },
    openWith,
    close,
  };
}

export const mobileAuth = createStore();
