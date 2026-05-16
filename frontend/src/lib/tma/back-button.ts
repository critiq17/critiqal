import { getTelegramWebApp } from '$lib/telegram';

// Single owner of the Telegram BackButton.
//
// Telegram's BackButton.onClick registers *additional* listeners that all
// fire — so multiple overlays/sheets each calling onClick caused a single
// back press to trigger every handler at once ("flies to the wrong place").
//
// Here exactly one dispatcher is wired for the app's lifetime. Screens push
// their handler while active; a back press invokes ONLY the top of the stack.
// The button is shown while the stack is non-empty and hidden when it drains.

type BackHandler = () => void;

const stack: BackHandler[] = [];
let wired = false;

function ensureWired(): void {
	if (wired) return;
	const tg = getTelegramWebApp();
	if (!tg) return;
	tg.BackButton.onClick(() => {
		stack[stack.length - 1]?.();
	});
	wired = true;
}

function syncVisibility(): void {
	const tg = getTelegramWebApp();
	if (!tg) return;
	if (stack.length > 0) tg.BackButton.show();
	else tg.BackButton.hide();
}

/**
 * Push a back handler while a screen is active. Returns a disposer that
 * removes it (call on close/destroy). Only the most recently pushed handler
 * responds to a back press, so nested overlays/sheets unwind one level at a
 * time in the exact reverse order they were opened.
 */
export function pushBackHandler(handler: BackHandler): () => void {
	const tg = getTelegramWebApp();
	if (!tg) return () => {};
	ensureWired();
	stack.push(handler);
	syncVisibility();
	return () => {
		const i = stack.lastIndexOf(handler);
		if (i >= 0) stack.splice(i, 1);
		syncVisibility();
	};
}
