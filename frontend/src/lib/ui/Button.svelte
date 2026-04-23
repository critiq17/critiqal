<script lang="ts">
	interface Props {
		variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
		size?: 'sm' | 'md' | 'lg';
		disabled?: boolean;
		loading?: boolean;
		type?: 'button' | 'submit' | 'reset';
		onclick?: (e: MouseEvent) => void;
		children: import('svelte').Snippet;
	}

	let {
		variant = 'primary',
		size = 'md',
		disabled = false,
		loading = false,
		type = 'button',
		onclick,
		children,
	}: Props = $props();
</script>

<button
	{type}
	class="btn btn-{variant} btn-{size}"
	disabled={disabled || loading}
	{onclick}
>
	{#if loading}
		<span class="spinner"></span>
	{:else}
		{@render children()}
	{/if}
</button>

<style>
	.btn {
		display: inline-flex;
		align-items: center;
		justify-content: center;
		gap: var(--spacing-xs);
		border: none;
		border-radius: var(--radius-md);
		font-weight: 600;
		cursor: pointer;
		transition: opacity var(--transition-fast), background var(--transition-fast);
		white-space: nowrap;
	}

	.btn:disabled { opacity: 0.5; cursor: not-allowed; }
	.btn:active:not(:disabled) { opacity: 0.8; }

	.btn-sm { padding: 6px 12px; font-size: 0.8rem; }
	.btn-md { padding: 10px 20px; font-size: 0.9rem; }
	.btn-lg { padding: 14px 28px; font-size: 1rem; }

	.btn-primary { background: var(--color-accent); color: #fff; }
	.btn-primary:hover:not(:disabled) { background: #c84848; }
	.btn-secondary { background: var(--color-surface-raised); color: var(--color-text-primary); }
	.btn-secondary:hover:not(:disabled) { background: var(--color-border); }
	.btn-ghost { background: transparent; color: var(--color-text-muted); }
	.btn-ghost:hover:not(:disabled) { color: var(--color-text-primary); }
	.btn-danger { background: #c0392b; color: #fff; }

	.spinner {
		width: 16px;
		height: 16px;
		border: 2px solid rgba(255,255,255,0.3);
		border-top-color: #fff;
		border-radius: var(--radius-full);
		animation: spin 0.7s linear infinite;
	}

	@keyframes spin { to { transform: rotate(360deg); } }
	@media (prefers-reduced-motion: reduce) { .spinner { animation: none; } }
</style>
