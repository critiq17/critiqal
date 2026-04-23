import { postService } from '$lib/services';

export class UsePostMutations {
	deleting = $state(false);
	deleted = $state(false);

	constructor(
		private postId: number,
		private onDeleted?: (id: number) => void
	) {}

	async deletePost(): Promise<void> {
		if (this.deleting) return;
		this.deleting = true;
		try {
			await postService.delete(this.postId);
			this.deleted = true;
			this.onDeleted?.(this.postId);
		} catch {
			// silent
		} finally {
			this.deleting = false;
		}
	}
}
