<script lang="ts">
	interface Props { size?: number; color?: string; }
	let { size = 24, color = 'var(--color-text-muted)' }: Props = $props();
</script>

<div class="spinner" style="width:{size}px;height:{size}px;border-top-color:{color};"></div>

<style>
	.spinner {
		border: 2px solid var(--color-border);
		border-radius: 50%;
		animation: spin 0.7s linear infinite;
	}
	@keyframes spin { to { transform: rotate(360deg); } }
	@media (prefers-reduced-motion: reduce) { .spinner { animation: none; border-top-color: var(--color-text-muted); } }
</style>
