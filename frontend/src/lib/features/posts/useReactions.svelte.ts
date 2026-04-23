import { postService } from '$lib/services';
import { DEFAULT_REACTIONS } from '$lib/reactions';
import { authStore } from '$lib/stores/auth.store.svelte';
import type { ReactionType, ReactionsMap } from '$lib/types';

export class UseReactions {
	reactions = $state<ReactionsMap>({ ...DEFAULT_REACTIONS });
	myReaction = $state<ReactionType | null>(null);
	loaded = $state(false);
	reacting = $state(false);
	poppingType = $state<ReactionType | null>(null);

	constructor(private postId: number) {}

	async load(): Promise<void> {
		if (this.loaded) return;
		try {
			const [data, mine] = await Promise.all([
				postService.getReactions(this.postId),
				authStore.isAuthenticated
					? postService.getMyReaction(this.postId).catch(() => undefined)
					: Promise.resolve(undefined)
			]);
			this.reactions = { ...DEFAULT_REACTIONS, ...data };
			this.myReaction = mine ?? null;
		} catch {
			// silent fail — show defaults
		} finally {
			this.loaded = true;
		}
	}

	async react(type: ReactionType): Promise<void> {
		if (!authStore.isAuthenticated || this.reacting) return;
		await this.load();

		const prev = { reactions: { ...this.reactions }, myReaction: this.myReaction };
		const isSame = this.myReaction === type;

		// Optimistic update
		if (isSame) {
			this.reactions = { ...this.reactions, [type]: Math.max(0, (this.reactions[type] ?? 0) - 1) };
			this.myReaction = null;
		} else {
			if (this.myReaction) {
				this.reactions = {
					...this.reactions,
					[this.myReaction]: Math.max(0, (this.reactions[this.myReaction] ?? 0) - 1)
				};
			}
			this.reactions = { ...this.reactions, [type]: (this.reactions[type] ?? 0) + 1 };
			this.myReaction = type;
			this.poppingType = type;
			setTimeout(() => {
				this.poppingType = null;
			}, 400);
		}

		this.reacting = true;
		try {
			if (isSame) {
				await postService.removeReaction(this.postId);
			} else {
				await postService.react(this.postId, type);
			}
		} catch {
			// Rollback
			this.reactions = prev.reactions;
			this.myReaction = prev.myReaction;
		} finally {
			this.reacting = false;
		}
	}
}
