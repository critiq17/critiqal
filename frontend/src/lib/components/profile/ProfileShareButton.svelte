<script lang="ts">
	import { fade } from 'svelte/transition';
	import { getTelegramWebApp } from '$lib/telegram';
	import { buildUserShareLink } from '$lib/deeplink';
	import { hapticLight } from '$lib/tma/buttons';
	import { t } from '$lib/i18n';

	interface Props {
		username: string;
		displayName?: string | null;
	}

	let { username, displayName }: Props = $props();

	let copiedToast = $state(false);
	let copyToastTimer: ReturnType<typeof setTimeout> | null = null;

	function profileUrl(): string {
		// Telegram Mini App deep link — opens the app straight to this profile.
		return buildUserShareLink(username);
	}

	function shareText(): string {
		const who = displayName?.trim() || `@${username}`;
		return `${who} on Critiqal`;
	}

	function flashCopied(): void {
		copiedToast = true;
		if (copyToastTimer) clearTimeout(copyToastTimer);
		copyToastTimer = setTimeout(() => {
			copiedToast = false;
		}, 1600);
	}

	async function copyToClipboard(url: string): Promise<boolean> {
		try {
			await navigator.clipboard.writeText(url);
			return true;
		} catch {
			return false;
		}
	}

	async function handleShare(): Promise<void> {
		hapticLight();
		const url = profileUrl();
		const text = shareText();

		const tg = getTelegramWebApp();
		if (tg) {
			// Telegram's native share sheet — picks chats to forward into.
			const link = `https://t.me/share/url?url=${encodeURIComponent(url)}&text=${encodeURIComponent(text)}`;
			tg.openTelegramLink(link);
			return;
		}

		if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
			try {
				await navigator.share({ title: text, text, url });
				return;
			} catch {
				// User dismissed share sheet — silently fall through to clipboard fallback.
			}
		}

		if (await copyToClipboard(url)) {
			flashCopied();
		}
	}
</script>

<button class="share-btn" type="button" onclick={handleShare} aria-label={t('post.share')}>
	<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
		<path d="M4 12v7a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-7" />
		<polyline points="16 6 12 2 8 6" />
		<line x1="12" y1="2" x2="12" y2="15" />
	</svg>
	<span>{t('post.share')}</span>
</button>

{#if copiedToast}
	<div class="copied-toast" role="status" aria-live="polite" transition:fade={{ duration: 160 }}>
		{t('post.shareCopied')}
	</div>
{/if}

<style>
	.share-btn {
		display: inline-flex;
		align-items: center;
		gap: 0.4375rem;
		padding: 0.5rem 0.875rem;
		border-radius: 9999px;
		font-size: 0.875rem;
		font-weight: 600;
		font-family: inherit;
		background: none;
		color: var(--color-text-primary);
		border: 1px solid var(--color-border);
		cursor: pointer;
		transition: background-color 0.15s ease, transform 0.1s ease;
		-webkit-tap-highlight-color: transparent;
	}

	.share-btn:hover {
		background-color: var(--color-surface-raised);
	}

	.share-btn:active:not(:disabled) {
		transform: scale(0.96);
	}

	.share-btn svg {
		width: 0.875rem;
		height: 0.875rem;
	}

	.copied-toast {
		position: fixed;
		bottom: calc(env(safe-area-inset-bottom, 0px) + 1.5rem);
		left: 50%;
		transform: translateX(-50%);
		padding: 0.5rem 0.875rem;
		border-radius: 9999px;
		background: rgba(20, 20, 20, 0.92);
		color: #fff;
		font-size: 0.8125rem;
		font-weight: 500;
		box-shadow: 0 4px 18px rgba(0, 0, 0, 0.32);
		z-index: 60;
		pointer-events: none;
	}

	@media (prefers-reduced-motion: reduce) {
		.share-btn {
			transition: none;
		}
	}
</style>
