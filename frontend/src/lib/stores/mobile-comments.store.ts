import { writable } from 'svelte/store';
import type { Post } from '$lib/types';

interface MobileCommentsSheetState {
	open: boolean;
	postId: string | null;
	// The full post is carried so the sheet can keep post.commentCount in sync
	// as comments are added or removed (the feed card reads the same object).
	post: Post | null;
}

const initialState: MobileCommentsSheetState = {
	open: false,
	postId: null,
	post: null
};

const store = writable<MobileCommentsSheetState>(initialState);

export const mobileComments = {
	subscribe: store.subscribe,

	open(post: Post): void {
		if (post.id.trim().length === 0) return;
		store.set({
			open: true,
			postId: post.id,
			post
		});
	},

	close(): void {
		store.set(initialState);
	}
};

export function openMobileComments(post: Post): void {
	mobileComments.open(post);
}

export function closeMobileComments(): void {
	mobileComments.close();
}
