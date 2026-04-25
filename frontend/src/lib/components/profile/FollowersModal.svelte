<script lang="ts">
	import type { User } from '$lib/types';

	interface Props {
		open: boolean;
		tab: 'followers' | 'following';
		followersList: User[];
		followingList: User[];
		loading: boolean;
		onClose: () => void;
	}

	let { open, tab, followersList, followingList, loading, onClose }: Props = $props();

	const list = $derived(tab === 'followers' ? followersList : followingList);

	function getInitial(user: User): string {
		return (user.name ?? user.username).charAt(0).toUpperCase();
	}

	function handleBackdropClick(e: MouseEvent): void {
		if (e.target === e.currentTarget) onClose();
	}

	function handleBackdropKeydown(e: KeyboardEvent): void {
		if (e.key === 'Escape') onClose();
	}
</script>

{#if open}
	<div
		class="modal-backdrop"
		role="presentation"
		onclick={handleBackdropClick}
		onkeydown={handleBackdropKeydown}
	>
		<div
			class="modal-panel"
			role="dialog"
			aria-modal="true"
			aria-label={tab === 'followers' ? 'Followers' : 'Following'}
		>
			<header class="modal-header">
				<h2 class="modal-title">{tab === 'followers' ? 'Followers' : 'Following'}</h2>
				<button class="modal-close" onclick={onClose} aria-label="Close">&times;</button>
			</header>

			<div class="modal-body">
				{#if loading}
					{#each { length: 3 } as _, i (i)}
						<div class="user-skeleton" aria-hidden="true">
							<div class="skeleton-avatar"></div>
							<div class="skeleton-text-group">
								<div class="skeleton-line wide"></div>
								<div class="skeleton-line narrow"></div>
							</div>
						</div>
					{/each}
				{:else if list.length === 0}
					<p class="modal-empty">
						{tab === 'followers' ? 'No followers yet.' : 'No one followed yet.'}
					</p>
				{:else}
					{#each list as user (user.id)}
						<a href="/{user.username}" class="user-row" onclick={onClose}>
							<div class="user-avatar" aria-hidden="true">
								{#if user.avatarUrl}
									<img src={user.avatarUrl} alt={user.username} class="avatar-img" />
								{:else}
									<span class="user-avatar-initial">{getInitial(user)}</span>
								{/if}
							</div>
							<div class="user-info">
								<span class="user-name">{user.name ?? user.username}</span>
								<span class="user-handle">@{user.username}</span>
							</div>
						</a>
					{/each}
				{/if}
			</div>
		</div>
	</div>
{/if}

<style>
	.modal-backdrop {
		position: fixed;
		inset: 0;
		z-index: 100;
		background: rgba(0, 0, 0, 0.55);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.modal-panel {
		background: var(--color-bg);
		border: 1px solid var(--color-border);
		border-radius: 1rem;
		width: 90vw;
		max-width: 420px;
		max-height: 70vh;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.modal-header {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 1rem 1.25rem;
		border-bottom: 1px solid var(--color-border);
		flex-shrink: 0;
	}

	.modal-title {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text-primary);
		letter-spacing: -0.01em;
	}

	.modal-close {
		background: none;
		border: none;
		cursor: pointer;
		font-size: 1.5rem;
		line-height: 1;
		color: var(--color-text-muted);
		padding: 0.125rem 0.375rem;
		border-radius: 0.375rem;
		font-family: inherit;
		transition: color 0.15s ease, background-color 0.15s ease;
	}

	.modal-close:hover {
		color: var(--color-text-primary);
		background-color: var(--color-surface-raised);
	}

	.modal-body {
		overflow-y: auto;
		flex: 1;
	}

	.modal-empty {
		padding: 2.5rem 1.25rem;
		text-align: center;
		font-size: 0.9375rem;
		color: var(--color-text-muted);
	}

	.user-row {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		padding: 0.75rem 1.25rem;
		text-decoration: none;
		transition: background-color 0.15s ease;
	}

	.user-row:hover {
		background-color: var(--color-surface-raised);
	}

	.user-avatar {
		width: 2.5rem;
		height: 2.5rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
		flex-shrink: 0;
	}

	.avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.user-avatar-initial {
		font-size: 1rem;
		font-weight: 600;
		color: var(--color-text-muted);
		user-select: none;
	}

	.user-info {
		display: flex;
		flex-direction: column;
		gap: 0.0625rem;
		min-width: 0;
	}

	.user-name {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text-primary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.user-handle {
		font-size: 0.875rem;
		color: var(--color-text-muted);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	/* Skeletons */
	.user-skeleton {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		padding: 0.75rem 1.25rem;
	}

	.skeleton-avatar {
		width: 2.5rem;
		height: 2.5rem;
		border-radius: 50%;
		background: var(--color-skeleton);
		animation: shimmer 1.6s ease-in-out infinite;
		flex-shrink: 0;
	}

	.skeleton-text-group {
		flex: 1;
		display: flex;
		flex-direction: column;
		gap: 0.25rem;
	}

	.skeleton-line {
		border-radius: 4px;
		background: var(--color-skeleton);
		animation: shimmer 1.6s ease-in-out infinite;
		height: 0.8125rem;
	}

	.skeleton-line.wide { width: 7rem; }
	.skeleton-line.narrow { width: 4.5rem; height: 0.75rem; }

	@keyframes shimmer {
		0%, 100% { opacity: 0.5; }
		50% { opacity: 1; }
	}
</style>
