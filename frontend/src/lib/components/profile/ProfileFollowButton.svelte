<script lang="ts">
	interface Props {
		isFollowing: boolean;
		isLoading: boolean;
		onFollow: () => void;
	}

	let { isFollowing, isLoading, onFollow }: Props = $props();
</script>

<button
	class="action-btn"
	class:btn-follow={!isFollowing}
	class:btn-following={isFollowing}
	onclick={onFollow}
	disabled={isLoading}
	aria-pressed={isFollowing}
>
	{isFollowing ? 'Following' : 'Follow'}
</button>

<style>
	.action-btn {
		padding: 0.5rem 1.125rem;
		border-radius: 9999px;
		font-size: 0.875rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition:
			background-color 0.15s ease,
			color 0.15s ease,
			border-color 0.15s ease,
			transform 0.1s ease;
	}

	.action-btn:active:not(:disabled) {
		transform: scale(0.96);
	}

	.action-btn:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}

	.btn-follow {
		background-color: var(--color-text-primary);
		color: var(--color-bg);
		border: none;
	}

	.btn-follow:hover:not(:disabled) {
		background-color: var(--color-text-muted);
	}

	.btn-following {
		background: none;
		color: var(--color-text-primary);
		border: 1px solid var(--color-border);
	}

	.btn-following:hover:not(:disabled) {
		background-color: rgba(224, 82, 82, 0.08);
		border-color: var(--color-accent);
		color: var(--color-accent);
	}
</style>
