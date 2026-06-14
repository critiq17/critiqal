<script lang="ts">
	interface Props {
		open: boolean;
		onclose: () => void;
		title?: string;
		children: import('svelte').Snippet;
	}
	let { open, onclose, title, children }: Props = $props();

	function onkeydown(e: KeyboardEvent) { if (e.key === 'Escape') onclose(); }
</script>

{#if open}
	<!-- svelte-ignore a11y_click_events_have_key_events a11y_no_static_element_interactions -->
	<div class="backdrop" onclick={onclose}></div>
	<div
		class="modal"
		role="dialog"
		aria-modal="true"
		aria-label={title}
		tabindex="-1"
		onkeydown={onkeydown}
	>
		{#if title}
			<div class="modal-header">
				<h2>{title}</h2>
				<button onclick={onclose} aria-label="Close">✕</button>
			</div>
		{/if}
		<div class="modal-body">
			{@render children()}
		</div>
	</div>
{/if}

<style>
	.backdrop {
		position: fixed; inset: 0;
		background: rgba(0,0,0,0.7);
		backdrop-filter: blur(var(--glass-blur-scrim));
		-webkit-backdrop-filter: blur(var(--glass-blur-scrim));
		z-index: 200;
		animation: fadeIn var(--transition-base);
	}
	.modal {
		position: fixed;
		top: 50%; left: 50%;
		transform: translate(-50%, -50%);
		background: var(--color-surface);
		border-radius: var(--radius-lg);
		border: 1px solid var(--color-border);
		z-index: 201;
		min-width: 280px;
		max-width: 90vw;
		max-height: 85vh;
		display: flex;
		flex-direction: column;
		overflow: hidden;
		outline: none;
		animation: scaleIn var(--transition-base);
	}
	.modal-header {
		display: flex; align-items: center; justify-content: space-between;
		padding: var(--spacing-md);
		border-bottom: 1px solid var(--color-border);
	}
	.modal-header h2 { margin: 0; font-size: 1rem; font-weight: 600; }
	.modal-header button { background: none; border: none; cursor: pointer; color: var(--color-text-muted); font-size: 1.1rem; }
	.modal-body { padding: var(--spacing-md); overflow-y: auto; flex: 1; }
	@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
	@keyframes scaleIn { from { opacity: 0; transform: translate(-50%,-50%) scale(0.95); } to { opacity: 1; transform: translate(-50%,-50%) scale(1); } }
	@media (prefers-reduced-motion: reduce) { .backdrop, .modal { animation: none; } }
</style>
