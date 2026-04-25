<script lang="ts">
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
<div class="menu" role="menu">
	{#if isOwnPost}
		<button class="menu-item danger" role="menuitem" onclick={ondelete} disabled={deleting}>
			{deleting ? 'Deleting…' : 'Delete post'}
		</button>
	{/if}
	<button class="menu-item" role="menuitem" onclick={onclose}>Cancel</button>
</div>

<style>
	.overlay {
		position: fixed;
		inset: 0;
		z-index: 50;
	}

	.menu {
		position: absolute;
		right: 0;
		top: 100%;
		z-index: 51;
		background: var(--color-surface-raised);
		border: 1px solid var(--color-border);
		border-radius: var(--radius-md, 6px);
		padding: 4px;
		min-width: 160px;
		box-shadow: 0 4px 12px rgba(0, 0, 0, 0.4);
		display: flex;
		flex-direction: column;
	}

	.menu-item {
		background: none;
		border: none;
		cursor: pointer;
		padding: 10px 14px;
		border-radius: var(--radius-sm, 4px);
		text-align: left;
		font-size: 0.875rem;
		font-family: inherit;
		color: var(--color-text-primary);
		transition: background-color 0.15s ease;
	}

	.menu-item:hover {
		background: var(--color-surface);
	}

	.menu-item.danger {
		color: var(--color-accent);
	}

	.menu-item:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}
</style>
