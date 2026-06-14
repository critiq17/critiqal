<script lang="ts">
	interface Props {
		src?: string | null;
		name?: string | null;
		size?: number;
		onclick?: () => void;
		// Above-the-fold avatars (profile hero, current-user chip) opt into
		// eager/high-priority loading; everything in feeds and lists defers.
		priority?: boolean;
	}

	let { src, name, size = 40, onclick, priority = false }: Props = $props();

	function getInitials(n: string | null | undefined): string {
		if (!n) return '?';
		const parts = n.trim().split(/\s+/);
		if (parts.length === 1) return (parts[0]?.[0] ?? '?').toUpperCase();
		return ((parts[0]?.[0] ?? '') + (parts[parts.length - 1]?.[0] ?? '')).toUpperCase();
	}

	let imgError = $state(false);
</script>

<!-- svelte-ignore a11y_click_events_have_key_events a11y_no_static_element_interactions -->
<div
	class="avatar"
	style="width:{size}px;height:{size}px;font-size:{Math.round(size * 0.38)}px;"
	onclick={onclick}
	role={onclick ? 'button' : undefined}
	tabindex={onclick ? 0 : undefined}
>
	{#if src && !imgError}
		<img
			{src}
			alt={name ?? ''}
			loading={priority ? 'eager' : 'lazy'}
			decoding="async"
			fetchpriority={priority ? 'high' : 'auto'}
			onerror={() => { imgError = true; }}
		/>
	{:else}
		<span>{getInitials(name)}</span>
	{/if}
</div>

<style>
	.avatar {
		border-radius: var(--radius-full);
		background: var(--color-surface-raised);
		border: 1px solid var(--color-border);
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
		flex-shrink: 0;
		cursor: v-bind("onclick ? 'pointer' : 'default'");
	}

	img {
		width: 100%;
		height: 100%;
		object-fit: cover;
		border-radius: var(--radius-full);
	}

	span {
		color: var(--color-text-muted);
		font-weight: 600;
		line-height: 1;
		user-select: none;
	}
</style>
