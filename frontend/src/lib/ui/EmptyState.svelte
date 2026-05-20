<script lang="ts">
	import type { Snippet } from 'svelte';

	interface Props {
		title: string;
		subtitle?: string;
		icon?: Snippet;
		cta?: Snippet;
	}

	let { title, subtitle, icon, cta }: Props = $props();
</script>

<div class="empty-state" role="status">
	{#if icon}
		<div class="empty-icon">
			{@render icon()}
		</div>
	{/if}
	<p class="empty-title">{title}</p>
	{#if subtitle}
		<p class="empty-subtitle">{subtitle}</p>
	{/if}
	{#if cta}
		<div class="empty-cta">
			{@render cta()}
		</div>
	{/if}
</div>

<style>
	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		padding: 60px 24px;
		gap: 12px;
		text-align: center;
	}

	.empty-icon {
		width: 48px;
		height: 48px;
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		background: var(--surface-tint-subtle);
		color: var(--text-faint);
		margin-bottom: 4px;
	}

	.empty-title {
		font-size: 15px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
		margin: 0;
	}

	.empty-subtitle {
		font-size: 13px;
		color: var(--text-quaternary);
		margin: 0;
		margin-top: -4px;
		max-width: 240px;
	}

	.empty-cta {
		margin-top: 4px;
	}
</style>
