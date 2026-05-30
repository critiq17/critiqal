<script lang="ts">
	import type { AdminUser } from '$lib/types';
	import AdminBadgePill from './AdminBadgePill.svelte';

	interface Props {
		user: AdminUser;
		onselect?: (user: AdminUser) => void;
	}

	let { user, onselect }: Props = $props();

	const initial = $derived((user.name ?? user.username).charAt(0).toUpperCase());
</script>

<button class="row" onclick={() => onselect?.(user)} aria-label={`Manage ${user.username}`}>
	<div class="avatar" aria-hidden="true">
		{#if user.avatarUrl}
			<img src={user.avatarUrl} alt="" />
		{:else}
			<span class="initial">{initial}</span>
		{/if}
	</div>
	<div class="meta">
		<span class="name">{user.name ?? user.username}</span>
		<span class="username">@{user.username}</span>
		{#if user.badges.length > 0}
			<div class="badges">
				{#each user.badges.slice(0, 4) as badge (badge.id)}
					<AdminBadgePill {badge} />
				{/each}
			</div>
		{/if}
	</div>
	<span class="chevron" aria-hidden="true">›</span>
</button>

<style>
	.row {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		width: 100%;
		padding: 0.75rem;
		background: transparent;
		border: none;
		border-radius: var(--radius-md);
		cursor: pointer;
		text-align: left;
		font-family: inherit;
	}
	.row:hover {
		background: var(--color-surface-raised);
	}
	.avatar {
		width: 44px;
		height: 44px;
		border-radius: var(--radius-full);
		background: var(--color-surface-raised);
		overflow: hidden;
		flex-shrink: 0;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.avatar img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}
	.initial {
		font-size: 1.125rem;
		font-weight: 700;
		color: var(--color-text-secondary);
	}
	.meta {
		display: flex;
		flex-direction: column;
		gap: 0.2rem;
		min-width: 0;
		flex: 1;
	}
	.name {
		color: var(--color-text-primary);
		font-weight: 600;
	}
	.username {
		color: var(--color-text-secondary);
		font-size: 0.8125rem;
	}
	.badges {
		display: flex;
		flex-wrap: wrap;
		gap: 0.25rem;
		margin-top: 0.25rem;
	}
	.chevron {
		color: var(--color-text-muted);
		font-size: 1.25rem;
		flex-shrink: 0;
	}
</style>
