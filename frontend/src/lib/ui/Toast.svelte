<script lang="ts">
	export type ToastType = 'info' | 'success' | 'error' | 'warning';

	interface Props {
		message: string;
		type?: ToastType;
		duration?: number;
		onclose: () => void;
	}

	let { message, type = 'info', duration = 3000, onclose }: Props = $props();

	$effect(() => {
		const t = setTimeout(onclose, duration);
		return () => clearTimeout(t);
	});
</script>

<div class="toast toast-{type}" role="alert" aria-live="polite">
	<span>{message}</span>
	<button onclick={onclose} aria-label="Dismiss">✕</button>
</div>

<style>
	.toast {
		display: flex;
		align-items: center;
		gap: var(--spacing-sm);
		padding: var(--spacing-sm) var(--spacing-md);
		border-radius: var(--radius-md);
		font-size: 0.875rem;
		font-weight: 500;
		min-width: 200px;
		max-width: 320px;
		box-shadow: var(--shadow-md);
		animation: slideUp var(--transition-base) ease;
	}

	.toast span { flex: 1; }
	.toast button { background: none; border: none; cursor: pointer; opacity: 0.7; color: inherit; font-size: 1rem; }
	.toast button:hover { opacity: 1; }

	.toast-info    { background: var(--color-surface-raised); color: var(--color-text-primary); }
	.toast-success { background: #1a3a1a; color: #4caf50; }
	.toast-error   { background: #3a1a1a; color: #e05252; }
	.toast-warning { background: #3a2a1a; color: #ff9800; }

	@keyframes slideUp { from { transform: translateY(8px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
	@media (prefers-reduced-motion: reduce) { .toast { animation: none; } }
</style>
