import type { Post } from '$lib/types';

// Telegram-style long-press focus: one post is lifted above a blurred feed.
// The post object is carried here so the overlay renders instantly without
// a refetch. Only one post can be focused at a time.
class MobilePostFocusStore {
	#post = $state<Post | null>(null);

	get post(): Post | null {
		return this.#post;
	}

	get isOpen(): boolean {
		return this.#post !== null;
	}

	open(post: Post): void {
		this.#post = post;
	}

	close(): void {
		this.#post = null;
	}
}

export const mobilePostFocus = new MobilePostFocusStore();
