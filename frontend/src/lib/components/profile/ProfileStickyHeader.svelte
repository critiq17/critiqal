<script lang="ts">
	interface Props {
		username: string | null;
		visible: boolean;
		onSettings: () => void;
	}

	let { username, visible, onSettings }: Props = $props();
</script>

<div class="sticky-header" class:visible aria-hidden={!visible}>
	<span class="sticky-username">
		{#if username}@{username}{/if}
	</span>
	<button
		class="settings-btn"
		onclick={onSettings}
		aria-label="Settings"
		tabindex={visible ? 0 : -1}
	>
		<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
			<circle cx="12" cy="12" r="3"/>
			<path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/>
		</svg>
	</button>
</div>

<style>
	.sticky-header {
		position: sticky;
		top: 0;
		background: var(--color-bg, #0f0f0f);
		z-index: 10;
		padding: 12px 16px;
		display: flex;
		align-items: center;
		justify-content: space-between;
		opacity: 0;
		transition: opacity 0.2s ease;
		pointer-events: none;
	}

	.sticky-header.visible {
		opacity: 1;
		pointer-events: auto;
	}

	.sticky-username {
		font-size: 15px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
	}

	.settings-btn {
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-secondary, rgba(240, 240, 240, 0.6));
		padding: 8px;
		min-width: 44px;
		min-height: 44px;
		display: flex;
		align-items: center;
		justify-content: center;
	}
</style>
