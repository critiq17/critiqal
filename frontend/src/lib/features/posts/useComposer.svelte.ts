import { postService } from '$lib/services/post.service';
import { mediaService } from '$lib/services/media.service';
import type { Post, PostPhoto } from '$lib/types';

const MAX_PHOTOS = 3;
const MAX_CHARS = 500;

export class UseComposer {
	text = $state('');
	selectedFiles = $state<File[]>([]);
	previewUrls = $state<string[]>([]);
	loading = $state(false);
	errorMessage = $state('');

	get hasContent(): boolean { return this.text.trim().length > 0; }
	get charsLeft(): number { return MAX_CHARS - this.text.length; }
	get overLimit(): boolean { return this.charsLeft < 0; }
	get canPost(): boolean { return this.hasContent && !this.overLimit && !this.loading; }
	get maxPhotos(): number { return MAX_PHOTOS; }

	addFiles(files: File[]): void {
		const remaining = MAX_PHOTOS - this.selectedFiles.length;
		const toAdd = files.slice(0, remaining);
		const newUrls = toAdd.map((f) => URL.createObjectURL(f));
		this.selectedFiles = [...this.selectedFiles, ...toAdd];
		this.previewUrls = [...this.previewUrls, ...newUrls];
	}

	removePhoto(index: number): void {
		const url = this.previewUrls[index];
		if (url) URL.revokeObjectURL(url);
		this.selectedFiles = this.selectedFiles.filter((_, i) => i !== index);
		this.previewUrls = this.previewUrls.filter((_, i) => i !== index);
	}

	reset(): void {
		this.previewUrls.forEach((url) => URL.revokeObjectURL(url));
		this.text = '';
		this.selectedFiles = [];
		this.previewUrls = [];
		this.errorMessage = '';
		this.loading = false;
	}

	async submit(): Promise<Post | null> {
		const content = this.text.trim();
		if (!content || this.loading || this.overLimit) return null;

		this.loading = true;
		this.errorMessage = '';

		try {
			const newPost = await postService.create({ content });

			const uploadedPhotos: PostPhoto[] = [];
			for (const file of this.selectedFiles) {
				try {
					const photo = await mediaService.uploadPostPhoto(newPost.id, file);
					uploadedPhotos.push(photo);
				} catch {
					// skip failed uploads
				}
			}

			const finalPost: Post =
				uploadedPhotos.length > 0 ? { ...newPost, photos: uploadedPhotos } : newPost;

			this.reset();
			return finalPost;
		} catch (err: unknown) {
			this.errorMessage = err instanceof Error ? err.message : 'Failed to post. Try again.';
			this.loading = false;
			return null;
		}
	}
}
