<script lang="ts">
	interface Props {
		variant?: 'primary' | 'secondary' | 'ghost' | 'danger' | 'glass';
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
		transform-origin: center;
		transition:
			opacity var(--duration-micro) var(--ease-out-quart),
			background-color var(--duration-micro) var(--ease-out-quart),
			transform var(--duration-press) var(--ease-out-quart);
		white-space: nowrap;
	}

	.btn:disabled { opacity: 0.5; cursor: not-allowed; }
	.btn:active:not(:disabled) {
		opacity: 0.85;
		transform: scale(0.94);
	}

	@media (prefers-reduced-motion: reduce) {
		.btn {
			transition:
				opacity var(--duration-micro) var(--ease-out-quart),
				background-color var(--duration-micro) var(--ease-out-quart);
		}
		.btn:active:not(:disabled) { transform: none; }
	}

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
	/* Translucent glass button — pairs with the .glass surface system */
	.btn-glass {
		background: var(--glass-bg-soft);
		backdrop-filter: blur(calc(var(--glass-blur) + 8px)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(calc(var(--glass-blur) + 8px)) saturate(var(--glass-saturate));
		border: 1px solid var(--glass-border);
		box-shadow: inset 0 1px 0 var(--glass-highlight);
		color: var(--color-text-primary);
	}
	.btn-glass:hover:not(:disabled) { background: var(--glass-bg); }

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
