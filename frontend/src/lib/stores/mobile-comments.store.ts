import { writable } from 'svelte/store';

interface MobileCommentsSheetState {
	open: boolean;
	postId: number | null;
}

const initialState: MobileCommentsSheetState = {
	open: false,
	postId: null
};

const store = writable<MobileCommentsSheetState>(initialState);

export const mobileComments = {
	subscribe: store.subscribe,

	open(postId: number): void {
		if (!Number.isFinite(postId) || postId <= 0) return;
		store.set({
			open: true,
			postId
		});
	},

	close(): void {
		store.set(initialState);
	}
};

export function openMobileComments(postId: number): void {
	mobileComments.open(postId);
}

export function closeMobileComments(): void {
	mobileComments.close();
}
