<script lang="ts">
	import { scale } from 'svelte/transition';
	import { cubicOut } from 'svelte/easing';
	import { t } from '$lib/i18n';

	interface Props {
		isOwnPost: boolean;
		deleting: boolean;
		ondelete: () => void;
		onclose: () => void;
	}

	let { isOwnPost, deleting, ondelete, onclose }: Props = $props();
</script>

<!-- svelte-ignore a11y_click_events_have_key_events a11y_no_static_element_interactions -->
<div class="overlay" onclick={onclose}></div>
<div
	class="menu glass"
	role="menu"
	transition:scale={{ duration: 160, start: 0.92, opacity: 0, easing: cubicOut }}
>
	{#if isOwnPost}
		<button class="menu-item danger" role="menuitem" onclick={ondelete} disabled={deleting}>
			{deleting ? '…' : t('post.delete')}
		</button>
	{/if}
	<button class="menu-item" role="menuitem" onclick={onclose}>{t('common.cancel')}</button>
</div>

<style>
	.overlay {
		position: fixed;
		inset: 0;
		z-index: 50;
	}

	/* Semi-transparent liquid glass — the feed glows softly through, and the
	   menu springs out from the ⋯ corner rather than just appearing. */
	.menu {
		position: absolute;
		right: 0;
		top: calc(100% + 4px);
		z-index: 51;
		transform-origin: top right;
		background: var(--glass-bg-soft, rgba(28, 28, 30, 0.55));
		backdrop-filter: blur(calc(var(--glass-blur, 24px) + 4px)) saturate(var(--glass-saturate, 180%));
		-webkit-backdrop-filter: blur(calc(var(--glass-blur, 24px) + 4px)) saturate(var(--glass-saturate, 180%));
		border: 1px solid var(--glass-border, var(--surface-tint-medium));
		border-radius: 14px;
		padding: 5px;
		min-width: 168px;
		box-shadow:
			inset 0 1px 0 var(--glass-highlight, rgba(255, 255, 255, 0.08)),
			0 12px 32px rgba(0, 0, 0, 0.42);
		display: flex;
		flex-direction: column;
		gap: 1px;
	}

	.menu-item {
		background: none;
		border: none;
		cursor: pointer;
		padding: 11px 14px;
		border-radius: 10px;
		text-align: left;
		font-size: 0.9rem;
		font-family: inherit;
		color: var(--color-text-primary);
		transition: background-color 0.15s ease;
		-webkit-tap-highlight-color: transparent;
	}

	.menu-item:active {
		background: var(--surface-tint-medium);
	}

	@media (hover: hover) {
		.menu-item:hover {
			background: var(--surface-tint-subtle);
		}
	}

	.menu-item.danger {
		color: var(--color-accent, #e05252);
	}

	.menu-item:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	@media (prefers-reduced-motion: reduce) {
		.menu {
			transition: none;
		}
	}
</style>
