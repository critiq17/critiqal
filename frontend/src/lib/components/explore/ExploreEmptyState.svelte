<script lang="ts">
	interface Props {
		variant: 'empty' | 'error';
		message: string;
		onRetry?: () => void;
	}

	let { variant, message, onRetry }: Props = $props();
</script>

<div class="empty-state" role={variant === 'error' ? 'alert' : 'status'}>
	<div class="icon-wrap" class:dashed={variant === 'empty'}>
		{#if variant === 'error'}
			<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
				<circle cx="12" cy="12" r="10" />
				<line x1="12" y1="8" x2="12" y2="12" />
				<line x1="12" y1="16" x2="12.01" y2="16" />
			</svg>
		{:else}
			<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
				<circle cx="11" cy="11" r="8" />
				<line x1="21" y1="21" x2="16.65" y2="16.65" />
			</svg>
		{/if}
	</div>
	<p class="title">{variant === 'error' ? 'Something went wrong' : 'Nothing found'}</p>
	<p class="subtitle">{message}</p>
	{#if variant === 'error' && onRetry}
		<button class="retry-btn" type="button" onclick={onRetry}>Try again</button>
	{/if}
</div>

<style>
	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 60px 24px;
		gap: 16px;
		color: rgba(255, 255, 255, 0.4);
		text-align: center;
	}

	.icon-wrap {
		width: 48px;
		height: 48px;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		background: rgba(255, 255, 255, 0.04);
	}

	.icon-wrap svg { width: 22px; height: 22px; color: rgba(255, 255, 255, 0.3); }

	.icon-wrap.dashed {
		border: 1.5px dashed rgba(255, 255, 255, 0.2);
		background: none;
	}

	.title {
		font-size: 15px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
		margin: 0;
	}

	.subtitle { font-size: 13px; color: rgba(255, 255, 255, 0.4); margin: 0; margin-top: -8px; }

	.retry-btn {
		margin-top: 4px;
		padding: 8px 20px;
		border-radius: 8px;
		border: 1px solid var(--color-border, rgba(255, 255, 255, 0.12));
		background: none;
		color: var(--color-text-primary, #f0f0f0);
		font-size: 14px;
		font-weight: 500;
		font-family: inherit;
		cursor: pointer;
		transition: background-color 0.15s ease, transform 0.1s ease;
	}

	.retry-btn:hover { background-color: var(--color-surface-raised, #242424); }
	.retry-btn:active { transform: scale(0.97); }
</style>
