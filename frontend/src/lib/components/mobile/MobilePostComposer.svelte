<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import type { Post } from '$lib/types';
	import { getTelegramWebApp } from '$lib/telegram';
	import { showBackButton, showMainButton, setMainButtonLoading, setMainButtonEnabled } from '$lib/tma/buttons';
	import { UseComposer } from '$lib/features/posts/useComposer.svelte';
	import ComposerTextarea from '$lib/components/composer/ComposerTextarea.svelte';
	import ComposerPhotoPicker from '$lib/components/composer/ComposerPhotoPicker.svelte';
	import ComposerPhotoPreview from '$lib/components/composer/ComposerPhotoPreview.svelte';
	import { registerSheet } from '$lib/actions/registerSheet';

	interface Props {
		open: boolean;
		onClose: () => void;
		onPosted: (post: Post) => void;
	}

	let { open, onClose, onPosted }: Props = $props();

	const composer = new UseComposer();

	let textareaEl: HTMLTextAreaElement | null = null;
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

<div class="composer" class:open use:registerSheet role="dialog" aria-modal="true" aria-label="New post">
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
		<ComposerTextarea
			value={composer.text}
			disabled={composer.loading}
			onValueChange={(v) => { composer.text = v; }}
			onInput={autoGrow}
			bindEl={(el) => { textareaEl = el; }}
		/>

		<ComposerPhotoPreview
			previewUrls={composer.previewUrls}
			onRemove={removePhoto}
			onView={(url) => { viewingPhotoUrl = url; }}
		/>
	</div>

	<div class="composer-footer" bind:this={footerEl}>
		<div class="footer-left">
			{#if composer.selectedFiles.length < composer.maxPhotos}
				<ComposerPhotoPicker
					disabled={composer.loading}
					onFiles={(files) => composer.addFiles(files)}
				/>
			{/if}
		</div>
		<span
			class="char-counter"
			class:warn={composer.charsLeft <= 20}
			class:over={composer.overLimit}
		>
			{composer.charsLeft}
		</span>
	</div>
</div>

{#if viewingPhotoUrl}
	<div class="photo-viewer" role="dialog" aria-label="Photo preview">
		<button
			class="viewer-backdrop"
			onclick={() => { viewingPhotoUrl = null; }}
			aria-label="Close photo viewer"
			type="button"
		></button>
		<img src={viewingPhotoUrl} alt="Full size preview" class="viewer-img" />
		<button
			class="viewer-close"
			onclick={() => { viewingPhotoUrl = null; }}
			aria-label="Close"
			type="button"
		>
			<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20" aria-hidden="true">
				<line x1="18" y1="6" x2="6" y2="18" />
				<line x1="6" y1="6" x2="18" y2="18" />
			</svg>
		</button>
	</div>
{/if}

<style>
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
