import { getTelegramWebApp } from '$lib/telegram';

/**
 * Shows the TG BackButton and wires a click handler.
 * Returns a cleanup function to call on component destroy.
 */
export function showBackButton(handler: () => void): () => void {
	const tg = getTelegramWebApp();
	if (!tg) return () => {};
	tg.BackButton.show();
	tg.BackButton.onClick(handler);
	return () => {
		tg.BackButton.offClick(handler);
		tg.BackButton.hide();
	};
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
