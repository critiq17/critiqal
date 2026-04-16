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
	const MAX_CHARS = 500;

	let text = $state('');
	let selectedFiles = $state<File[]>([]);
	let previewUrls = $state<string[]>([]);
	let loading = $state(false);
	let errorMessage = $state('');
	let viewingPhotoUrl = $state<string | null>(null);

	let textareaEl: HTMLTextAreaElement | null = null;
	let fileInputEl: HTMLInputElement | null = null;
	let footerEl: HTMLElement | null = null;

	let hasContent = $derived(text.trim().length > 0);
	let charsLeft = $derived(MAX_CHARS - text.length);
	let overLimit = $derived(charsLeft < 0);

	// ── textarea auto-grow ───────────────────────────────────────────────────────
	function autoGrow(el: HTMLTextAreaElement): void {
		el.style.height = 'auto';
		el.style.height = el.scrollHeight + 'px';
	}

	function handleInput(e: Event): void {
		autoGrow(e.target as HTMLTextAreaElement);
	}

	// ── photo helpers ────────────────────────────────────────────────────────────
	function triggerFileInput(): void {
		fileInputEl?.click();
	}

	function handleFileChange(e: Event): void {
		const input = e.target as HTMLInputElement;
		if (!input.files?.length) return;

		const remaining = MAX_PHOTOS - selectedFiles.length;
		const toAdd = Array.from(input.files).slice(0, remaining);
		const newUrls = toAdd.map((f) => URL.createObjectURL(f));

		selectedFiles = [...selectedFiles, ...toAdd];
		previewUrls = [...previewUrls, ...newUrls];
		input.value = '';
	}

	function removePhoto(index: number): void {
		URL.revokeObjectURL(previewUrls[index]);
		selectedFiles = selectedFiles.filter((_, i) => i !== index);
		previewUrls = previewUrls.filter((_, i) => i !== index);
		getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
	}

	function openPhotoViewer(url: string): void {
		viewingPhotoUrl = url;
	}

	function closePhotoViewer(): void {
		viewingPhotoUrl = null;
	}

	// ── state reset ──────────────────────────────────────────────────────────────
	function resetState(): void {
		text = '';
		previewUrls.forEach((url) => URL.revokeObjectURL(url));
		selectedFiles = [];
		previewUrls = [];
		errorMessage = '';
		loading = false;
		if (textareaEl) textareaEl.style.height = 'auto';
	}

	// ── submit ───────────────────────────────────────────────────────────────────
	async function submitPost(): Promise<void> {
		const content = text.trim();
		if (!content || loading || overLimit) return;

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
			resetState();
			onPosted(finalPost);
		} catch (err: unknown) {
			errorMessage = err instanceof Error ? err.message : 'Failed to post. Try again.';
			loading = false;
		}
	}

	// ── keyboard avoidance ───────────────────────────────────────────────────────
	// Push the footer above the software keyboard when it appears.
	onMount(() => {
		// Small delay so the open animation settles before keyboard appears
		const focusTimer = setTimeout(() => {
			if (textareaEl) {
				textareaEl.focus();
				autoGrow(textareaEl);
			}
		}, 350);

		const vpHandler = (): void => {
			if (!window.visualViewport || !footerEl) return;
			const kbHeight =
				window.innerHeight - window.visualViewport.height - window.visualViewport.offsetTop;
			footerEl.style.paddingBottom =
				kbHeight > 0 ? `${kbHeight + 8}px` : 'calc(var(--safe-bottom, 0px) + 8px)';
		};

		window.visualViewport?.addEventListener('resize', vpHandler);
		window.visualViewport?.addEventListener('scroll', vpHandler);

		return () => {
			clearTimeout(focusTimer);
			window.visualViewport?.removeEventListener('resize', vpHandler);
			window.visualViewport?.removeEventListener('scroll', vpHandler);
		};
	});
</script>

<!-- Full-screen overlay — slides up from bottom, same pattern as UserProfileOverlay -->
<div
	class="composer"
	class:open
	role="dialog"
	aria-modal="true"
	aria-label="New post"
>
	<!-- ── Header ── -->
	<div class="composer-header">
		<button class="hdr-btn cancel-btn" onclick={onClose} disabled={loading} type="button">
			Cancel
		</button>
		<span class="hdr-title">New post</span>
		<button
			class="hdr-btn post-btn"
			class:ready={hasContent && !overLimit}
			onclick={submitPost}
			disabled={!hasContent || loading || overLimit}
			type="button"
		>
			{loading ? 'Posting…' : 'Post'}
		</button>
	</div>

	{#if errorMessage}
		<p class="error-msg" role="alert">{errorMessage}</p>
	{/if}

	<!-- ── Scrollable body — grows with text, user can scroll to review ── -->
	<div class="composer-body">
		<textarea
			class="composer-textarea"
			bind:this={textareaEl}
			bind:value={text}
			oninput={handleInput}
			placeholder="What's on your mind?"
			disabled={loading}
			aria-label="Post content"
			autocomplete="off"
			autocorrect="on"
			spellcheck="true"
		></textarea>

		<!-- Photo grid — full-width cards, tap to preview, ✕ to remove -->
		{#if previewUrls.length > 0}
			<div class="photo-grid" class:single={previewUrls.length === 1}>
				{#each previewUrls as url, i (url)}
					<div class="photo-card">
						<button
							class="photo-tap"
							onclick={() => openPhotoViewer(url)}
							aria-label="View photo {i + 1}"
							type="button"
						>
							<img src={url} alt="Photo {i + 1}" />
						</button>
						<button
							class="remove-btn"
							onclick={() => removePhoto(i)}
							type="button"
							aria-label="Remove photo {i + 1}"
						>
							<svg viewBox="0 0 12 12" width="10" height="10" fill="none" aria-hidden="true">
								<path d="M1 1l10 10M11 1L1 11" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
							</svg>
						</button>
					</div>
				{/each}
			</div>
		{/if}
	</div>

	<!-- ── Footer toolbar ── -->
	<div class="composer-footer" bind:this={footerEl}>
		<div class="footer-left">
			{#if selectedFiles.length < MAX_PHOTOS}
				<button
					class="footer-icon-btn"
					onclick={triggerFileInput}
					disabled={loading}
					type="button"
					aria-label="Add photo"
				>
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" width="22" height="22" aria-hidden="true">
						<path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
						<circle cx="12" cy="13" r="4"/>
					</svg>
				</button>
			{/if}
		</div>
		<span class="char-counter" class:warn={charsLeft <= 20} class:over={overLimit}>
			{charsLeft}
		</span>
	</div>

	<input
		bind:this={fileInputEl}
		type="file"
		accept="image/*"
		multiple
		class="sr-only"
		onchange={handleFileChange}
		disabled={loading}
		aria-hidden="true"
		tabindex="-1"
	/>
</div>

<!-- Full-screen photo viewer -->
{#if viewingPhotoUrl}
	<div class="photo-viewer" role="dialog" aria-label="Photo preview">
		<button
			class="viewer-backdrop"
			onclick={closePhotoViewer}
			aria-label="Close photo viewer"
			type="button"
		></button>
		<img src={viewingPhotoUrl} alt="Full size preview" class="viewer-img" />
		<button
			class="viewer-close"
			onclick={closePhotoViewer}
			aria-label="Close"
			type="button"
		>
			<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20" aria-hidden="true">
				<line x1="18" y1="6" x2="6" y2="18"/>
				<line x1="6" y1="6" x2="18" y2="18"/>
			</svg>
		</button>
	</div>
{/if}

<style>
	/* ── Full-screen overlay ────────────────────────────────────────────────────── */
	.composer {
		position: fixed;
		inset: 0;
		z-index: 160;
		background: var(--tg-bg, #111);
		display: flex;
		flex-direction: column;
		transform: translateY(100%);
		transition: transform 0.32s cubic-bezier(0.32, 0.72, 0, 1);
		/* Prevent interaction with content behind while animating */
		pointer-events: none;
	}

	.composer.open {
		transform: translateY(0);
		pointer-events: auto;
	}

	/* ── Header ─────────────────────────────────────────────────────────────────── */
	.composer-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: calc(var(--tg-content-top, var(--tg-content-safe-area-inset-top, env(safe-area-inset-top, 0px))) + 12px) 16px 12px;
		border-bottom: 1px solid rgba(255, 255, 255, 0.06);
		flex-shrink: 0;
	}

	.hdr-title {
		font-size: 15px;
		font-weight: 600;
		color: var(--tg-text, #f0f0f0);
	}

	.hdr-btn {
		background: none;
		border: none;
		cursor: pointer;
		font-size: 15px;
		font-family: inherit;
		padding: 4px 0;
		min-width: 64px;
		-webkit-tap-highlight-color: transparent;
	}

	.cancel-btn {
		color: var(--tg-hint, rgba(255, 255, 255, 0.5));
		text-align: left;
	}

	.cancel-btn:active {
		opacity: 0.6;
	}

	.post-btn {
		color: var(--tg-hint, rgba(255, 255, 255, 0.25));
		font-weight: 600;
		text-align: right;
		transition: color 0.15s ease;
	}

	.post-btn.ready {
		color: var(--tg-accent, #e05252);
	}

	.post-btn:disabled {
		cursor: default;
	}

	/* ── Error ───────────────────────────────────────────────────────────────────── */
	.error-msg {
		font-size: 13px;
		color: #e05252;
		padding: 8px 16px 0;
		flex-shrink: 0;
	}

	/* ── Scrollable body ─────────────────────────────────────────────────────────── */
	.composer-body {
		flex: 1;
		overflow-y: auto;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior: contain;
		padding: 16px;
		display: flex;
		flex-direction: column;
		gap: 16px;
	}

	/* ── Textarea ────────────────────────────────────────────────────────────────── */
	.composer-textarea {
		background: none;
		border: none;
		outline: none;
		font-size: 17px;
		line-height: 1.55;
		color: var(--tg-text, #f0f0f0);
		width: 100%;
		resize: none;
		/* overflow: hidden so autoGrow drives height, not internal scroll */
		overflow-y: hidden;
		min-height: 120px;
		font-family: inherit;
		box-sizing: border-box;
		caret-color: var(--tg-accent, #e05252);
	}

	.composer-textarea::placeholder {
		color: var(--tg-hint, rgba(255, 255, 255, 0.3));
	}

	.composer-textarea:disabled {
		opacity: 0.6;
	}

	/* ── Photo grid ──────────────────────────────────────────────────────────────── */
	.photo-grid {
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 6px;
	}

	/* Single photo: wider aspect ratio, more prominent */
	.photo-grid.single {
		grid-template-columns: 1fr;
	}

	.photo-grid.single .photo-card {
		aspect-ratio: 4 / 3;
	}

	.photo-card {
		position: relative;
		aspect-ratio: 1;
		border-radius: 12px;
		overflow: visible;
	}

	.photo-tap {
		display: block;
		width: 100%;
		height: 100%;
		padding: 0;
		border: none;
		background: none;
		cursor: pointer;
		border-radius: 12px;
		overflow: hidden;
		-webkit-tap-highlight-color: transparent;
	}

	.photo-tap img {
		width: 100%;
		height: 100%;
		object-fit: cover;
		display: block;
		border-radius: 12px;
		transition: opacity 0.15s ease;
	}

	.photo-tap:active img {
		opacity: 0.8;
	}

	.remove-btn {
		position: absolute;
		top: -8px;
		right: -8px;
		width: 24px;
		height: 24px;
		border-radius: 50%;
		background: rgba(20, 20, 20, 0.9);
		border: 1.5px solid rgba(255, 255, 255, 0.15);
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		color: rgba(255, 255, 255, 0.85);
		padding: 0;
		z-index: 1;
		-webkit-tap-highlight-color: transparent;
		transition: transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1);
	}

	.remove-btn:active {
		transform: scale(0.85);
	}

	/* ── Footer toolbar ──────────────────────────────────────────────────────────── */
	.composer-footer {
		flex-shrink: 0;
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 8px 16px calc(var(--safe-bottom, 0px) + 8px);
		border-top: 1px solid rgba(255, 255, 255, 0.06);
		transition: padding-bottom 0.1s ease;
	}

	.footer-left {
		display: flex;
		align-items: center;
		gap: 4px;
	}

	.footer-icon-btn {
		background: none;
		border: none;
		cursor: pointer;
		padding: 8px;
		color: var(--tg-hint, rgba(255, 255, 255, 0.5));
		display: flex;
		align-items: center;
		border-radius: 8px;
		-webkit-tap-highlight-color: transparent;
		transition: color 0.15s ease;
	}

	.footer-icon-btn:active {
		color: var(--tg-text, #f0f0f0);
	}

	.footer-icon-btn:disabled {
		opacity: 0.35;
		cursor: not-allowed;
	}

	.char-counter {
		font-size: 13px;
		color: var(--tg-hint, rgba(255, 255, 255, 0.3));
		min-width: 32px;
		text-align: right;
		transition: color 0.15s ease;
		font-variant-numeric: tabular-nums;
	}

	.char-counter.warn {
		color: rgba(255, 165, 0, 0.8);
	}

	.char-counter.over {
		color: #e05252;
		font-weight: 600;
	}

	/* ── Photo viewer ─────────────────────────────────────────────────────────────── */
	.photo-viewer {
		position: fixed;
		inset: 0;
		z-index: 300;
		display: flex;
		align-items: center;
		justify-content: center;
		animation: fadeIn 0.18s ease;
	}

	.viewer-backdrop {
		position: absolute;
		inset: 0;
		background: rgba(0, 0, 0, 0.93);
		border: none;
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
	}

	.viewer-img {
		position: relative;
		z-index: 1;
		max-width: 100%;
		max-height: 90dvh;
		object-fit: contain;
		border-radius: 8px;
		pointer-events: none;
	}

	.viewer-close {
		position: absolute;
		top: calc(var(--tg-content-top, env(safe-area-inset-top, 0px)) + 12px);
		right: 16px;
		z-index: 2;
		background: rgba(255, 255, 255, 0.1);
		backdrop-filter: blur(8px);
		-webkit-backdrop-filter: blur(8px);
		border: 1px solid rgba(255, 255, 255, 0.15);
		cursor: pointer;
		width: 36px;
		height: 36px;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		color: white;
		-webkit-tap-highlight-color: transparent;
	}

	@keyframes fadeIn {
		from { opacity: 0; }
		to   { opacity: 1; }
	}

	.sr-only {
		display: none;
	}
</style>
