<script lang="ts">
	import { Sheet } from '$lib/ui';
	import { getInitials } from '$lib/utils/getInitials';
	import type { User } from '$lib/types';

	interface Props {
		open: boolean;
		type: 'followers' | 'following' | null;
		list: User[];
		listsLoading: boolean;
		onClose: () => void;
	}

	let { open, type, list, listsLoading, onClose }: Props = $props();
</script>

<Sheet
	{open}
	onclose={onClose}
	title={type === 'followers' ? 'Followers' : 'Following'}
	maxHeight="65vh"
>
	<div class="sheet-body">
		{#if listsLoading}
			{#each { length: 4 } as _, i (i)}
				<div class="user-skeleton" aria-hidden="true">
					<div class="skeleton-avatar-sm"></div>
					<div class="skeleton-text-block">
						<div class="skeleton-line wide"></div>
						<div class="skeleton-line narrow"></div>
					</div>
				</div>
			{/each}
		{:else if list.length === 0}
			<p class="sheet-empty">
				{type === 'followers' ? 'No followers yet.' : 'Not following anyone yet.'}
			</p>
		{:else}
			{#each list as user (user.id)}
				<a href="/{user.username}" class="user-row" onclick={onClose}>
					<div class="user-avatar-sm" aria-hidden="true">
						{#if user.avatarUrl}
							<img src={user.avatarUrl} alt={user.username} class="user-avatar-img" />
						{:else}
							<span class="user-avatar-initial">{getInitials(user.name, user.username)}</span>
						{/if}
					</div>
					<div class="user-info">
						<span class="user-display-name">{user.name ?? user.username}</span>
						<span class="user-handle">@{user.username}</span>
					</div>
				</a>
			{/each}
		{/if}
	</div>
</Sheet>

<style>
	.sheet-body {
		flex: 1;
		overflow-y: auto;
		-webkit-overflow-scrolling: touch;
	}

	.sheet-empty {
		padding: 32px 16px;
		text-align: center;
		font-size: 14px;
		color: var(--text-quaternary);
	}

	.user-row {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 10px 16px;
		text-decoration: none;
		transition: background-color 0.15s ease;
	}

	.user-row:active {
		background: var(--color-surface-raised, #242424);
	}

	.user-avatar-sm {
		width: 40px;
		height: 40px;
		border-radius: 50%;
		background: var(--color-surface-raised, #242424);
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
		flex-shrink: 0;
	}

	.user-avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
		display: block;
	}

	.user-avatar-initial {
		font-size: 15px;
		font-weight: 600;
		color: var(--text-tertiary);
		user-select: none;
	}

	.user-info {
		display: flex;
		flex-direction: column;
		gap: 1px;
		min-width: 0;
	}

	.user-display-name {
		font-size: 14px;
		font-weight: 600;
		color: var(--color-text-primary, #f0f0f0);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.user-handle {
		font-size: 13px;
		color: var(--text-quaternary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.user-skeleton {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 10px 16px;
	}

	.skeleton-avatar-sm {
		width: 40px;
		height: 40px;
		border-radius: 50%;
		background: var(--color-skeleton, var(--surface-tint-medium));
		animation: shimmer 1.6s ease-in-out infinite;
		flex-shrink: 0;
	}

	.skeleton-text-block {
		flex: 1;
		display: flex;
		flex-direction: column;
		gap: 6px;
	}

	.skeleton-line {
		height: 12px;
		border-radius: 4px;
		background: var(--color-skeleton, var(--surface-tint-medium));
		animation: shimmer 1.6s ease-in-out infinite;
	}

	.skeleton-line.wide {
		width: 110px;
	}

	.skeleton-line.narrow {
		width: 72px;
	}

	@keyframes shimmer {
		0%, 100% { opacity: 0.5; }
		50% { opacity: 1; }
	}
</style>
