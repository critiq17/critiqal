import { writable } from 'svelte/store';

interface MobileCommentsSheetState {
	open: boolean;
	postId: string | null;
}

const initialState: MobileCommentsSheetState = {
	open: false,
	postId: null
};

const store = writable<MobileCommentsSheetState>(initialState);

export const mobileComments = {
	subscribe: store.subscribe,

	open(postId: string): void {
		if (postId.trim().length === 0) return;
		store.set({
			open: true,
			postId
		});
	},

	close(): void {
		store.set(initialState);
	}
};

export function openMobileComments(postId: string): void {
	mobileComments.open(postId);
}

export function closeMobileComments(): void {
	mobileComments.close();
}
