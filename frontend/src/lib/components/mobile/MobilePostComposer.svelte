<script lang="ts">
	import { onMount } from 'svelte';
	import type { Post } from '$lib/types';
	import { postService } from '$lib/services/post.service';
	import { mediaService } from '$lib/services/media.service';
	import { getTelegramWebApp } from '$lib/telegram';

	interface Props {
		open: boolean;
		onClose: () => void;
		onPosted: (post: Post) => void;
	}

	let { open, onClose, onPosted }: Props = $props();

	const MAX_PHOTOS = 3;

	let text = $state('');
	let selectedFiles = $state<File[]>([]);
	let previewUrls = $state<string[]>([]);
	let loading = $state(false);
	let errorMessage = $state('');

	let sheetEl = $state<HTMLElement | undefined>(undefined);
	let textareaEl = $state<HTMLTextAreaElement | undefined>(undefined);
	let fileInputEl = $state<HTMLInputElement | undefined>(undefined);

	let hasContent = $derived(text.trim().length > 0);

	function autoGrow(el: HTMLTextAreaElement): void {
		el.style.height = 'auto';
		el.style.height = el.scrollHeight + 'px';
	}

	function handleInput(e: Event): void {
		const el = e.target as HTMLTextAreaElement;
		autoGrow(el);
	}

	function triggerFileInput(): void {
		fileInputEl?.click();
	}

	function handleFileChange(e: Event): void {
		const input = e.target as HTMLInputElement;
		const files = input.files;
		if (!files || files.length === 0) return;

		const remaining = MAX_PHOTOS - selectedFiles.length;
		const toAdd = Array.from(files).slice(0, remaining);

		const newUrls = toAdd.map((f) => URL.createObjectURL(f));
		selectedFiles = [...selectedFiles, ...toAdd];
		previewUrls = [...previewUrls, ...newUrls];

		// Reset so the same file can be re-selected after removal
		input.value = '';
	}

	function removePhoto(index: number): void {
		URL.revokeObjectURL(previewUrls[index]);
		selectedFiles = selectedFiles.filter((_, i) => i !== index);
		previewUrls = previewUrls.filter((_, i) => i !== index);
	}

	function resetState(): void {
		text = '';
		previewUrls.forEach((url) => URL.revokeObjectURL(url));
		selectedFiles = [];
		previewUrls = [];
		errorMessage = '';
		loading = false;
		if (textareaEl) {
			textareaEl.style.height = 'auto';
		}
	}

	async function submitPost(): Promise<void> {
		const content = text.trim();
		if (!content || loading) return;

		loading = true;
		errorMessage = '';

		try {
			const newPost = await postService.create({ content });

			const uploadedPhotos: import('$lib/types').PostPhoto[] = [];
			for (const file of selectedFiles) {
				try {
					const photo = await mediaService.uploadPostPhoto(newPost.id, file);
					uploadedPhotos.push(photo);
				} catch {
					// skip failed uploads, continue with the rest
				}
			}

			const finalPost: Post =
				uploadedPhotos.length > 0 ? { ...newPost, photos: uploadedPhotos } : newPost;

			getTelegramWebApp()?.HapticFeedback.notificationOccurred('success');
			// Reset state BEFORE notifying parent — parent may destroy this component
			// synchronously via composerOpen = false, which would leave loading = true
			resetState();
			onPosted(finalPost);
		} catch (err: unknown) {
			errorMessage = err instanceof Error ? err.message : 'Failed to post. Please try again.';
		} finally {
			loading = false;
		}
	}

	// Keyboard handling via visualViewport
	onMount(() => {
		const handler = (): void => {
			if (!window.visualViewport || !sheetEl) return;
			const keyboardHeight =
				window.innerHeight - window.visualViewport.height - window.visualViewport.offsetTop;
			if (keyboardHeight > 0) {
				sheetEl.style.transform = `translateY(${-keyboardHeight}px)`;
			} else {
				sheetEl.style.transform = open ? 'translateY(0)' : 'translateY(100%)';
			}
		};

		window.visualViewport?.addEventListener('resize', handler);
		return () => window.visualViewport?.removeEventListener('resize', handler);
	});
</script>

{#if open}
	<div
		class="composer-backdrop"
		role="presentation"
		onclick={onClose}
	></div>
{/if}

<div
	class="composer-sheet"
	class:open
	bind:this={sheetEl}
	role="dialog"
	aria-modal="true"
	aria-label="New post composer"
>
	<div class="drag-handle"></div>

	<div class="composer-header">
		<span class="header-label">New post</span>
		<button
			class="post-btn"
			class:has-content={hasContent}
			onclick={submitPost}
			disabled={!hasContent || loading}
			type="button"
		>
			{#if loading}
				Posting…
			{:else}
				Post
			{/if}
		</button>
	</div>

	{#if errorMessage}
		<p class="error-message" role="alert">{errorMessage}</p>
	{/if}

	<textarea
		class="composer-textarea"
		bind:this={textareaEl}
		bind:value={text}
		oninput={handleInput}
		placeholder="What's on your mind?"
		disabled={loading}
		rows={3}
		aria-label="Post content"
	></textarea>

	{#if previewUrls.length > 0 || selectedFiles.length < MAX_PHOTOS}
		<div class="attachments-row">
			{#each previewUrls as url, i (url)}
				<div class="photo-preview">
					<img src={url} alt="Photo {i + 1}" />
					<button
						class="remove-btn"
						onclick={() => removePhoto(i)}
						type="button"
						aria-label="Remove photo {i + 1}"
					>
						<svg viewBox="0 0 12 12" fill="none" width="10" height="10" aria-hidden="true">
							<path
								d="M1 1l10 10M11 1L1 11"
								stroke="currentColor"
								stroke-width="2"
								stroke-linecap="round"
							/>
						</svg>
					</button>
				</div>
			{/each}

			{#if selectedFiles.length < MAX_PHOTOS}
				<button
					class="add-photo-btn"
					onclick={triggerFileInput}
					type="button"
					disabled={loading}
					aria-label="Add photo"
				>
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="24" height="24" aria-hidden="true">
						<path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
						<circle cx="12" cy="13" r="4" />
					</svg>
				</button>
			{/if}
		</div>
	{/if}

	<input
		bind:this={fileInputEl}
		type="file"
		accept="image/*"
		multiple
		class="hidden-file-input"
		onchange={handleFileChange}
		disabled={loading}
		aria-hidden="true"
		tabindex="-1"
	/>
</div>

<style>
	.composer-backdrop {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.5);
		z-index: 159;
	}

	.composer-sheet {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		height: 85dvh;
		border-radius: 24px 24px 0 0;
		background: var(--color-surface, #1a1a1a);
		z-index: 160;
		transform: translateY(100%);
		transition: transform 350ms cubic-bezier(0.32, 0.72, 0, 1);
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.composer-sheet.open {
		transform: translateY(0);
	}

	.drag-handle {
		width: 36px;
		height: 4px;
		border-radius: 2px;
		background: rgba(255, 255, 255, 0.2);
		margin: 12px auto 0;
		flex-shrink: 0;
	}

	.composer-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 12px 16px 8px;
		flex-shrink: 0;
	}

	.header-label {
		font-size: 15px;
		font-weight: 500;
		color: var(--tg-hint, var(--color-text-muted, rgba(255, 255, 255, 0.5)));
	}

	.post-btn {
		background: var(--tg-accent, #e05252);
		color: white;
		border: none;
		border-radius: 20px;
		padding: 6px 16px;
		font-weight: 600;
		font-size: 14px;
		font-family: inherit;
		cursor: pointer;
		opacity: 0.35;
		transition: opacity 150ms ease;
	}

	.post-btn.has-content:not(:disabled) {
		opacity: 1;
	}

	.post-btn:disabled {
		cursor: not-allowed;
	}

	.error-message {
		font-size: 13px;
		color: #e05252;
		padding: 0 16px 4px;
		flex-shrink: 0;
	}

	.composer-textarea {
		background: none;
		border: none;
		outline: none;
		font-size: 16px;
		color: var(--color-text-primary, #f0f0f0);
		width: 100%;
		resize: none;
		padding: 16px 16px 0;
		min-height: 72px;
		line-height: 1.5;
		font-family: inherit;
		flex-shrink: 0;
		box-sizing: border-box;
	}

	.composer-textarea::placeholder {
		color: var(--tg-hint, rgba(255, 255, 255, 0.35));
	}

	.composer-textarea:disabled {
		opacity: 0.6;
	}

	.attachments-row {
		display: flex;
		gap: 8px;
		padding: 12px 16px;
		overflow-x: auto;
		scrollbar-width: none;
		flex-shrink: 0;
	}

	.attachments-row::-webkit-scrollbar {
		display: none;
	}

	.photo-preview {
		position: relative;
		flex-shrink: 0;
		width: 80px;
		height: 80px;
	}

	.photo-preview img {
		width: 100%;
		height: 100%;
		object-fit: cover;
		border-radius: 12px;
		display: block;
	}

	.remove-btn {
		position: absolute;
		top: -6px;
		right: -6px;
		width: 20px;
		height: 20px;
		border-radius: 50%;
		background: rgba(0, 0, 0, 0.7);
		border: none;
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		color: white;
		font-size: 12px;
		padding: 0;
		z-index: 1;
	}

	.add-photo-btn {
		flex-shrink: 0;
		width: 80px;
		height: 80px;
		border-radius: 12px;
		border: 1.5px dashed rgba(255, 255, 255, 0.2);
		background: none;
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		color: rgba(255, 255, 255, 0.4);
	}

	.add-photo-btn:disabled {
		cursor: not-allowed;
		opacity: 0.5;
	}

	.hidden-file-input {
		display: none;
	}
</style>
