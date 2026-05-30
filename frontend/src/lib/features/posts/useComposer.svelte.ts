import { postService } from '$lib/services/post.service';
import { mediaService } from '$lib/services/media.service';
import { authStore } from '$lib/stores/auth.store.svelte';
import { authGate } from '$lib/stores/auth-gate.store.svelte';
import { ApiError } from '$lib/types';
import type { Post, PostPhoto } from '$lib/types';
import { normalizeContent } from '$lib/utils/normalizeContent';

interface SubmitOptions {
  /**
   * Fires the moment the server confirms the bare post, BEFORE photo uploads
   * complete. Lets the feed insert the post immediately for instant feedback.
   */
  onCreated?: (post: Post) => void;
  /**
   * Fires once all photo uploads have settled (succeeded or failed) so the
   * feed can patch the inserted post with its photos.
   */
  onPhotosReady?: (postId: string, photos: PostPhoto[]) => void;
}

const MAX_PHOTOS = 3;
const MAX_CHARS = 500;

export class UseComposer {
  text = $state('');
  selectedFiles = $state<File[]>([]);
  previewUrls = $state<string[]>([]);
  loading = $state(false);
  errorMessage = $state('');

  get normalizedText(): string {
    return normalizeContent(this.text);
  }
  get hasContent(): boolean {
    return this.normalizedText.length > 0;
  }
  get charsLeft(): number {
    return MAX_CHARS - this.normalizedText.length;
  }
  get overLimit(): boolean {
    return this.charsLeft < 0;
  }
  get canPost(): boolean {
    return this.hasContent && !this.overLimit && !this.loading;
  }
  get maxPhotos(): number {
    return MAX_PHOTOS;
  }
  get maxChars(): number {
    return MAX_CHARS;
  }

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

  async submit(options: SubmitOptions = {}): Promise<Post | null> {
    if (!authStore.isAuthenticated) {
      authGate.open('compose');
      return null;
    }

    const content = this.normalizedText;
    if (!content || this.loading || this.overLimit) return null;

    this.loading = true;
    this.errorMessage = '';

    let newPost: Post;
    try {
      newPost = await postService.create({ content });
    } catch (err: unknown) {
      if (err instanceof ApiError && (err.status === 401 || err.status === 403)) {
        // Auth lapsed between opening composer and submit, or email not
        // verified. Swallow the raw status and steer the user to the right
        // recovery surface instead of dumping "401" onto the UI.
        if (err.status === 401) authGate.open('compose');
        this.loading = false;
        return null;
      }
      this.errorMessage = err instanceof Error ? err.message : 'Failed to post. Try again.';
      this.loading = false;
      return null;
    }

    // Fire the optimistic callback BEFORE photo upload so the feed can render
    // the post instantly. Snapshot files so reset() can clear the composer state.
    const filesToUpload = [...this.selectedFiles];
    options.onCreated?.(newPost);
    this.reset();

    if (filesToUpload.length === 0) return newPost;

    const uploadedPhotos: PostPhoto[] = [];
    for (const file of filesToUpload) {
      try {
        const photo = await mediaService.uploadPostPhoto(newPost.id, file);
        uploadedPhotos.push(photo);
      } catch {
        // skip failed uploads
      }
    }

    options.onPhotosReady?.(newPost.id, uploadedPhotos);
    return uploadedPhotos.length > 0 ? { ...newPost, photos: uploadedPhotos } : newPost;
  }
}
