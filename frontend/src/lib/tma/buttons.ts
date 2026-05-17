import { getTelegramWebApp } from '$lib/telegram';
import { pushBackHandler } from './back-button';

/**
 * Shows the TG BackButton wired to `handler` while the caller is active.
 * Routed through the single BackButton owner so nested screens unwind one
 * level per press, in reverse order. Returns a disposer for destroy.
 */
export function showBackButton(handler: () => void): () => void {
  return pushBackHandler(handler);
}

/** Alias kept for callers that opened over another back-nav screen. */
export function pushBackButton(handler: () => void): () => void {
  return pushBackHandler(handler);
}

/** Trigger a light haptic impact, no-op outside Telegram. */
export function hapticLight(): void {
  const tg = getTelegramWebApp();
  tg?.HapticFeedback.impactOccurred('light');
}

/**
 * Shows the TG MainButton with the given text and click handler.
 * Returns a cleanup function to call on component destroy.
 */
export function showMainButton(text: string, handler: () => void): () => void {
  const tg = getTelegramWebApp();
  if (!tg) return () => {};
  tg.MainButton.setText(text);
  tg.MainButton.show();
  tg.MainButton.onClick(handler);
  return () => {
    tg.MainButton.offClick(handler);
    tg.MainButton.hide();
  };
}

/** Sets MainButton loading state: spinner + disabled while loading. */
export function setMainButtonLoading(loading: boolean): void {
  const tg = getTelegramWebApp();
  if (!tg) return;
  if (loading) {
    tg.MainButton.showProgress(true);
    tg.MainButton.disable();
  } else {
    tg.MainButton.hideProgress();
  }
}

/** Enables or disables the MainButton. */
export function setMainButtonEnabled(enabled: boolean): void {
  const tg = getTelegramWebApp();
  if (!tg) return;
  if (enabled) tg.MainButton.enable();
  else tg.MainButton.disable();
}
