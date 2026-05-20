<script lang="ts">
	interface Props {
		title: string;
		onBack: () => void;
	}

	let { title, onBack }: Props = $props();
</script>

<header class="overlay-header glass-strong">
	<button class="back-btn" type="button" onclick={onBack} aria-label="Back">
		<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor"
			stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
			<polyline points="15 18 9 12 15 6" />
		</svg>
	</button>
	<span class="header-title">{title}</span>
	<div class="header-spacer" aria-hidden="true"></div>
</header>

<style>
	/* Sits clear of the native Telegram header (Close / collapse / menu) by
	   offsetting the full content-safe-area / safe-area inset on top. */
	.overlay-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 8px;
		flex-shrink: 0;
		padding: 6px 4px 10px;
		padding-top: calc(10px + var(--tg-top-clearance));
		/* glass-strong supplies background/blur/border/shadow */
		border-bottom: 1px solid var(--surface-tint-soft);
	}

	.back-btn {
		background: none;
		border: none;
		cursor: pointer;
		color: var(--tg-text, var(--text-strong));
		min-width: 44px;
		min-height: 44px;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 12px;
		-webkit-tap-highlight-color: transparent;
		transition: opacity 0.15s ease, background-color 0.15s ease;
	}

	.back-btn:active {
		opacity: 0.55;
		background: var(--surface-tint-soft);
	}

	.header-title {
		flex: 1;
		min-width: 0;
		text-align: center;
		font-size: 16px;
		font-weight: 600;
		color: var(--tg-text, #f0f0f0);
		letter-spacing: -0.01em;
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.header-spacer {
		width: 44px;
		flex-shrink: 0;
	}
</style>
