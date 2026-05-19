<script lang="ts">
	import { onMount, onDestroy } from 'svelte';
	import type { Post } from '$lib/types';
	import { getTelegramWebApp } from '$lib/telegram';
	import { showBackButton } from '$lib/tma/buttons';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { getInitials } from '$lib/utils/getInitials';
	import { UseComposer } from '$lib/features/posts/useComposer.svelte';
	import ComposerTextarea from '$lib/components/composer/ComposerTextarea.svelte';
	import ComposerPhotoPicker from '$lib/components/composer/ComposerPhotoPicker.svelte';
	import ComposerPhotoPreview from '$lib/components/composer/ComposerPhotoPreview.svelte';
	import { registerSheet } from '$lib/actions/registerSheet';
	import StarDraw from '$lib/ui/StarDraw.svelte';

	interface Props {
		open: boolean;
		onClose: () => void;
		onPosted: (post: Post) => void;
	}

	let { open, onClose, onPosted }: Props = $props();

	const composer = new UseComposer();

	let textareaEl: HTMLTextAreaElement | null = null;
	let bottomEl: HTMLElement | null = null;
	let viewingPhotoUrl = $state<string | null>(null);

	const user = $derived(authStore.user);
	const displayName = $derived(user?.name?.trim() || user?.username || 'You');

	// Circular progress ring around the character counter.
	const RING_R = 13;
	const RING_C = 2 * Math.PI * RING_R;
	const usedFraction = $derived(
		Math.min(Math.max((composer.maxChars - composer.charsLeft) / composer.maxChars, 0), 1)
	);
	const ringOffset = $derived(RING_C * (1 - usedFraction));

	function autoGrow(el: HTMLTextAreaElement): void {
		el.style.height = 'auto';
		el.style.height = el.scrollHeight + 'px';
	}

	function removePhoto(index: number): void {
		composer.removePhoto(index);
		getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
	}

	async function submitPost(): Promise<void> {
		if (!composer.canPost) return;
		const post = await composer.submit();
		if (post) {
			getTelegramWebApp()?.HapticFeedback.notificationOccurred('success');
			onPosted(post);
		}
	}

	let cleanupBackButton: (() => void) | null = null;

	onMount(() => {
		const tg = getTelegramWebApp();
		if (tg?.platform && tg.platform !== 'unknown') {
			cleanupBackButton = showBackButton(onClose);
		}

		const focusTimer = setTimeout(() => {
			if (textareaEl) {
				textareaEl.focus();
				autoGrow(textareaEl);
			}
		}, 350);

		// Keep the bottom bar docked above the soft keyboard.
		const vpHandler = (): void => {
			if (!window.visualViewport || !bottomEl) return;
			const kbHeight =
				window.innerHeight - window.visualViewport.height - window.visualViewport.offsetTop;
			bottomEl.style.transform = kbHeight > 0 ? `translateY(-${kbHeight}px)` : '';
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
	});
</script>

<div class="composer" class:open use:registerSheet role="dialog" aria-modal="true" aria-label="New post">
	<header class="composer-header">
		<button class="hbtn glass" onclick={onClose} disabled={composer.loading} type="button" aria-label="Close">
			<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16" aria-hidden="true">
				<line x1="18" y1="6" x2="6" y2="18" />
				<line x1="6" y1="6" x2="18" y2="18" />
			</svg>
			<span>Close</span>
		</button>

		<span class="hdr-title">New post</span>

		<button
			class="hbtn post-btn glass"
			class:ready={composer.canPost}
			onclick={submitPost}
			disabled={!composer.canPost}
			type="button"
		>
			{#if composer.loading}
				<StarDraw size={20} duration={1100} title="Posting" />
			{:else}
				Post
			{/if}
		</button>
	</header>

	{#if composer.errorMessage}
		<p class="error-msg" role="alert">{composer.errorMessage}</p>
	{/if}

	<div class="composer-body">
		<div class="author-row">
			<div class="avatar" aria-hidden="true">
				{#if user?.avatarUrl}
					<img src={user.avatarUrl} alt="" />
				{:else}
					<span class="avatar-initial">{getInitials(user?.name, user?.username)}</span>
				{/if}
			</div>
			<div class="author-meta">
				<span class="author-name">{displayName}</span>
				{#if user?.username}
					<span class="author-handle">@{user.username}</span>
				{/if}
			</div>
		</div>

		<ComposerTextarea
			value={composer.text}
			disabled={composer.loading}
			onValueChange={(v) => { composer.text = v; }}
			onInput={autoGrow}
			bindEl={(el) => { textareaEl = el; }}
		/>
	</div>

	<div class="composer-bottom" bind:this={bottomEl}>
		<ComposerPhotoPreview
			previewUrls={composer.previewUrls}
			onRemove={removePhoto}
			onView={(url) => { viewingPhotoUrl = url; }}
		/>

		<div class="toolbar">
			<div class="toolbar-left">
				{#if composer.selectedFiles.length < composer.maxPhotos}
					<ComposerPhotoPicker
						disabled={composer.loading}
						onFiles={(files) => composer.addFiles(files)}
					/>
				{/if}
				<span class="photo-hint">
					{composer.selectedFiles.length}/{composer.maxPhotos}
				</span>
			</div>

			<div
				class="counter"
				class:warn={composer.charsLeft <= 20 && !composer.overLimit}
				class:over={composer.overLimit}
				aria-label="{composer.charsLeft} characters left"
			>
				{#if composer.charsLeft <= 60}
					<span class="counter-num">{composer.charsLeft}</span>
				{/if}
				<svg class="ring" viewBox="0 0 32 32" width="30" height="30" aria-hidden="true">
					<circle class="ring-track" cx="16" cy="16" r={RING_R} />
					<circle
						class="ring-progress"
						cx="16"
						cy="16"
						r={RING_R}
						stroke-dasharray={RING_C}
						stroke-dashoffset={ringOffset}
					/>
				</svg>
			</div>
		</div>
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
			class="viewer-close glass"
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
		background: var(--tg-bg, #0f0f0f);
		display: flex;
		flex-direction: column;
		transform: translateY(100%);
		transition: transform 0.34s cubic-bezier(0.32, 0.72, 0, 1);
		pointer-events: none;
	}

	.composer.open {
		transform: translateY(0);
		pointer-events: auto;
	}

	/* ── Header ─────────────────────────────────────────────────────────── */
	.composer-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 12px;
		padding: max(
				var(--tg-content-safe-area-inset-top, 0px),
				calc(env(safe-area-inset-top, 20px) + 44px)
			)
			16px 12px;
		flex-shrink: 0;
	}

	.hdr-title {
		font-size: 16px;
		font-weight: 700;
		color: var(--tg-text, #f0f0f0);
		letter-spacing: -0.01em;
	}

	/* Black glass buttons — no accent colour anywhere. */
	.hbtn {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		gap: 6px;
		height: 36px;
		min-width: 76px;
		padding: 0 16px;
		border-radius: 18px;
		font-size: 14px;
		font-weight: 600;
		font-family: inherit;
		color: rgba(255, 255, 255, 0.7);
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
		transition:
			transform 0.16s cubic-bezier(0.34, 1.56, 0.64, 1),
			color 0.18s ease,
			opacity 0.18s ease,
			box-shadow 0.18s ease;
	}

	.hbtn:active {
		transform: scale(0.93);
	}

	.post-btn {
		color: rgba(255, 255, 255, 0.32);
	}

	.post-btn.ready {
		color: #fff;
		box-shadow:
			var(--glass-shadow, 0 8px 32px rgba(0, 0, 0, 0.4)),
			inset 0 1px 0 var(--glass-highlight, rgba(255, 255, 255, 0.14)),
			0 0 0 1px rgba(255, 255, 255, 0.14);
	}

	.post-btn:disabled {
		cursor: default;
	}

	.error-msg {
		font-size: 13px;
		color: #e05252;
		padding: 4px 20px 0;
		flex-shrink: 0;
		margin: 0;
	}

	/* ── Body ───────────────────────────────────────────────────────────── */
	.composer-body {
		flex: 1;
		overflow-y: auto;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior: contain;
		padding: 8px 20px 20px;
		scrollbar-width: none;
		-ms-overflow-style: none;
	}

	.composer-body::-webkit-scrollbar {
		display: none;
	}

	.author-row {
		display: flex;
		align-items: center;
		gap: 10px;
		margin-bottom: 14px;
	}

	.avatar {
		width: 38px;
		height: 38px;
		border-radius: 50%;
		overflow: hidden;
		flex-shrink: 0;
		background: var(--color-surface-raised, #242424);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.avatar img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.avatar-initial {
		font-size: 14px;
		font-weight: 600;
		color: rgba(255, 255, 255, 0.55);
		user-select: none;
	}

	.author-meta {
		display: flex;
		flex-direction: column;
		min-width: 0;
		line-height: 1.3;
	}

	.author-name {
		font-size: 15px;
		font-weight: 600;
		color: var(--tg-text, #f0f0f0);
	}

	.author-handle {
		font-size: 13px;
		color: var(--tg-hint, rgba(255, 255, 255, 0.4));
	}

	/* ── Bottom bar (preview strip + glass toolbar) ─────────────────────── */
	.composer-bottom {
		flex-shrink: 0;
		background: var(--glass-bg, rgba(20, 20, 20, 0.78));
		backdrop-filter: blur(var(--glass-blur, 24px)) saturate(var(--glass-saturate, 180%));
		-webkit-backdrop-filter: blur(var(--glass-blur, 24px)) saturate(var(--glass-saturate, 180%));
		border-top: 1px solid var(--glass-border, rgba(255, 255, 255, 0.08));
		padding-bottom: calc(var(--safe-bottom, 0px) + 8px);
		will-change: transform;
	}

	.toolbar {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 8px 16px;
	}

	.toolbar-left {
		display: flex;
		align-items: center;
		gap: 10px;
	}

	.photo-hint {
		font-size: 12px;
		color: var(--tg-hint, rgba(255, 255, 255, 0.35));
		font-variant-numeric: tabular-nums;
	}

	.counter {
		position: relative;
		width: 30px;
		height: 30px;
		display: flex;
		align-items: center;
		justify-content: center;
		color: rgba(255, 255, 255, 0.5);
	}

	.counter.warn {
		color: #f0a92b;
	}

	.counter.over {
		color: #e05252;
	}

	.counter-num {
		position: absolute;
		font-size: 10px;
		font-weight: 600;
		font-variant-numeric: tabular-nums;
		color: currentColor;
	}

	.ring {
		transform: rotate(-90deg);
	}

	.ring-track {
		fill: none;
		stroke: rgba(255, 255, 255, 0.1);
		stroke-width: 2.5;
	}

	.ring-progress {
		fill: none;
		stroke: currentColor;
		stroke-width: 2.5;
		stroke-linecap: round;
		transition:
			stroke-dashoffset 0.2s ease,
			stroke 0.2s ease;
	}

	/* ── Full-size photo viewer ─────────────────────────────────────────── */
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
		border-radius: 10px;
		pointer-events: none;
	}

	.viewer-close {
		position: absolute;
		top: calc(var(--tg-content-top, env(safe-area-inset-top, 0px)) + 12px);
		right: 16px;
		z-index: 2;
		cursor: pointer;
		width: 38px;
		height: 38px;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		color: #fff;
		-webkit-tap-highlight-color: transparent;
	}

	@keyframes fadeIn {
		from {
			opacity: 0;
		}
		to {
			opacity: 1;
		}
	}
</style>
