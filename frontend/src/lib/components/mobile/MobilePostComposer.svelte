<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import type { Post } from '$lib/types';
	import { getTelegramWebApp } from '$lib/telegram';
	import { showBackButton, showMainButton, setMainButtonLoading, setMainButtonEnabled } from '$lib/tma/buttons';
	import { UseComposer } from '$lib/features/posts/useComposer.svelte';

	interface Props {
		open: boolean;
		onClose: () => void;
		onPosted: (post: Post) => void;
	}

	let { open, onClose, onPosted }: Props = $props();

	const composer = new UseComposer();

	let textareaEl: HTMLTextAreaElement | null = null;
	let fileInputEl: HTMLInputElement | null = null;
	let footerEl: HTMLElement | null = null;
	let viewingPhotoUrl = $state<string | null>(null);
	let hasTgMainButton = $state(false);

	$effect(() => {
		if (hasTgMainButton) setMainButtonEnabled(composer.canPost);
	});

	function autoGrow(el: HTMLTextAreaElement): void {
		el.style.height = 'auto';
		el.style.height = el.scrollHeight + 'px';
	}

	function handleInput(e: Event): void {
		autoGrow(e.target as HTMLTextAreaElement);
	}

	function triggerFileInput(): void {
		fileInputEl?.click();
	}

	function handleFileChange(e: Event): void {
		const input = e.target as HTMLInputElement;
		if (!input.files?.length) return;
		composer.addFiles(Array.from(input.files));
		input.value = '';
	}

	function removePhoto(index: number): void {
		composer.removePhoto(index);
		getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
	}

	async function submitPost(): Promise<void> {
		if (hasTgMainButton) setMainButtonLoading(true);
		const post = await composer.submit();
		if (post) {
			getTelegramWebApp()?.HapticFeedback.notificationOccurred('success');
			onPosted(post);
		} else if (hasTgMainButton) {
			setMainButtonLoading(false);
		}
	}

	let cleanupBackButton: (() => void) | null = null;
	let cleanupMainButton: (() => void) | null = null;

	onMount(() => {
		const tg = getTelegramWebApp();

		if (tg?.platform && tg.platform !== 'unknown') {
			cleanupBackButton = showBackButton(onClose);
			cleanupMainButton = showMainButton('Post', submitPost);
			setMainButtonEnabled(false);
			hasTgMainButton = true;
		}

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

	onDestroy(() => {
		cleanupBackButton?.();
		cleanupMainButton?.();
	});
</script>

<div class="composer" class:open role="dialog" aria-modal="true" aria-label="New post">
	<div class="composer-header">
		<button class="cancel-btn" onclick={onClose} disabled={composer.loading} type="button">
			Cancel
		</button>
		<span class="hdr-title">New post</span>
		{#if !hasTgMainButton}
			<button
				class="post-btn"
				class:ready={composer.canPost}
				onclick={submitPost}
				disabled={!composer.canPost}
				type="button"
			>
				{composer.loading ? '…' : 'Post'}
			</button>
		{:else}
			<span class="hdr-space" aria-hidden="true"></span>
		{/if}
	</div>

	{#if composer.errorMessage}
		<p class="error-msg" role="alert">{composer.errorMessage}</p>
	{/if}

	<div class="composer-body">
		<textarea
			class="composer-textarea"
			bind:this={textareaEl}
			bind:value={composer.text}
			oninput={handleInput}
			placeholder="What's on your mind?"
			disabled={composer.loading}
			aria-label="Post content"
			autocomplete="off"
			spellcheck="true"
		></textarea>

		{#if composer.previewUrls.length > 0}
			<div class="photo-grid" class:single={composer.previewUrls.length === 1}>
				{#each composer.previewUrls as url, i (url)}
					<div class="photo-card">
						<button class="photo-tap" onclick={() => (viewingPhotoUrl = url)} aria-label="View photo {i + 1}" type="button">
							<img src={url} alt="Photo {i + 1}" />
						</button>
						<button class="remove-btn" onclick={() => removePhoto(i)} type="button" aria-label="Remove photo {i + 1}">
							<svg viewBox="0 0 12 12" width="10" height="10" fill="none" aria-hidden="true">
								<path d="M1 1l10 10M11 1L1 11" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
							</svg>
						</button>
					</div>
				{/each}
			</div>
		{/if}
	</div>

	<div class="composer-footer" bind:this={footerEl}>
		<div class="footer-left">
			{#if composer.selectedFiles.length < composer.maxPhotos}
				<button class="footer-icon-btn" onclick={triggerFileInput} disabled={composer.loading} type="button" aria-label="Add photo">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" width="22" height="22" aria-hidden="true">
						<path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
						<circle cx="12" cy="13" r="4"/>
					</svg>
				</button>
			{/if}
		</div>
		<span class="char-counter" class:warn={composer.charsLeft <= 20} class:over={composer.overLimit}>
			{composer.charsLeft}
		</span>
	</div>

	<input
		bind:this={fileInputEl}
		type="file"
		accept="image/*"
		multiple
		class="sr-only"
		onchange={handleFileChange}
		disabled={composer.loading}
		aria-hidden="true"
		tabindex="-1"
	/>
</div>

{#if viewingPhotoUrl}
	<div class="photo-viewer" role="dialog" aria-label="Photo preview">
		<button class="viewer-backdrop" onclick={() => (viewingPhotoUrl = null)} aria-label="Close photo viewer" type="button"></button>
		<img src={viewingPhotoUrl} alt="Full size preview" class="viewer-img" />
		<button class="viewer-close" onclick={() => (viewingPhotoUrl = null)} aria-label="Close" type="button">
			<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20" aria-hidden="true">
				<line x1="18" y1="6" x2="6" y2="18"/>
				<line x1="6" y1="6" x2="18" y2="18"/>
			</svg>
		</button>
	</div>
{/if}

<style>
	.sr-only {
		position: absolute;
		width: 1px;
		height: 1px;
		padding: 0;
		margin: -1px;
		overflow: hidden;
		clip: rect(0, 0, 0, 0);
		white-space: nowrap;
		border: 0;
	}

	.composer {
		position: fixed;
		inset: 0;
		z-index: 160;
		background: var(--tg-bg, #111);
		display: flex;
		flex-direction: column;
		transform: translateY(100%);
		transition: transform 0.32s cubic-bezier(0.32, 0.72, 0, 1);
		pointer-events: none;
	}

	.composer.open {
		transform: translateY(0);
		pointer-events: auto;
	}

	.composer-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding-top: max(
			var(--tg-content-safe-area-inset-top, 0px),
			calc(env(safe-area-inset-top, 20px) + 44px)
		);
		padding-left: 16px;
		padding-right: 16px;
		padding-bottom: 10px;
		border-bottom: 1px solid rgba(255, 255, 255, 0.06);
		flex-shrink: 0;
	}

	.hdr-title {
		font-size: 15px;
		font-weight: 600;
		color: var(--tg-text, #f0f0f0);
	}

	.cancel-btn {
		background: none;
		border: none;
		cursor: pointer;
		font-size: 15px;
		font-family: inherit;
		color: var(--tg-hint, rgba(255, 255, 255, 0.5));
		padding: 6px 0;
		min-width: 64px;
		text-align: left;
		-webkit-tap-highlight-color: transparent;
	}

	.cancel-btn:active { opacity: 0.6; }
	.cancel-btn:disabled { cursor: default; }

	.post-btn {
		background: none;
		border: none;
		cursor: pointer;
		font-size: 15px;
		font-weight: 600;
		font-family: inherit;
		color: var(--tg-hint, rgba(255, 255, 255, 0.25));
		padding: 6px 0;
		min-width: 64px;
		text-align: right;
		transition: color 0.15s ease;
		-webkit-tap-highlight-color: transparent;
	}

	.post-btn.ready { color: var(--tg-accent, #e05252); }
	.post-btn:disabled { cursor: default; }
	.hdr-space { min-width: 64px; }

	.error-msg {
		font-size: 13px;
		color: #e05252;
		padding: 8px 16px 0;
		flex-shrink: 0;
	}

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

	.composer-textarea {
		background: none;
		border: none;
		outline: none;
		font-size: 17px;
		line-height: 1.55;
		color: var(--tg-text, #f0f0f0);
		width: 100%;
		resize: none;
		overflow-y: hidden;
		min-height: 120px;
		font-family: inherit;
		box-sizing: border-box;
		caret-color: var(--tg-accent, #e05252);
	}

	.composer-textarea::placeholder { color: var(--tg-hint, rgba(255, 255, 255, 0.3)); }
	.composer-textarea:disabled { opacity: 0.6; }

	.photo-grid {
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 6px;
	}

	.photo-grid.single { grid-template-columns: 1fr; }
	.photo-grid.single .photo-card { aspect-ratio: 4 / 3; }

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

	.photo-tap:active img { opacity: 0.8; }

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

	.remove-btn:active { transform: scale(0.85); }

	.composer-footer {
		flex-shrink: 0;
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 8px 16px calc(var(--safe-bottom, 0px) + 8px);
		border-top: 1px solid rgba(255, 255, 255, 0.06);
		transition: padding-bottom 0.1s ease;
	}

	.footer-left { display: flex; align-items: center; gap: 4px; }

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

	.footer-icon-btn:active { color: var(--tg-text, #f0f0f0); }
	.footer-icon-btn:disabled { opacity: 0.35; cursor: not-allowed; }

	.char-counter {
		font-size: 13px;
		color: var(--tg-hint, rgba(255, 255, 255, 0.3));
		min-width: 32px;
		text-align: right;
		transition: color 0.15s ease;
		font-variant-numeric: tabular-nums;
	}

	.char-counter.warn { color: rgba(255, 165, 0, 0.8); }
	.char-counter.over { color: #e05252; font-weight: 600; }

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
		to { opacity: 1; }
	}
</style>
