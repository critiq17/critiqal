<script lang="ts">
	interface Props {
		followersCount: number;
		followingCount: number;
		onOpenFollowers: () => void;
		onOpenFollowing: () => void;
	}

	let { followersCount, followingCount, onOpenFollowers, onOpenFollowing }: Props = $props();

	function formatCount(n: number): string {
		if (n >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
		if (n >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
		return String(n);
	}
</script>

<div class="profile-stats">
	<button class="stat-item" onclick={onOpenFollowers}>
		<span class="stat-value">{formatCount(followersCount)}</span>
		<span class="stat-label">Followers</span>
	</button>
	<button class="stat-item" onclick={onOpenFollowing}>
		<span class="stat-value">{formatCount(followingCount)}</span>
		<span class="stat-label">Following</span>
	</button>
</div>

<style>
	.profile-stats {
		display: flex;
		gap: 1.5rem;
		margin-top: 1.25rem;
	}

	.stat-item {
		display: flex;
		align-items: baseline;
		gap: 0.3125rem;
		background: none;
		border: none;
		padding: 0;
		font-family: inherit;
		cursor: pointer;
	}

	.stat-item:hover .stat-label {
		text-decoration: underline;
	}

	.stat-item:focus-visible {
		outline: 2px solid var(--color-text-muted);
		outline-offset: 3px;
		border-radius: 2px;
	}

	.stat-value {
		font-size: 0.9375rem;
		font-weight: 700;
		color: var(--color-text-primary);
	}

	.stat-label {
		font-size: 0.875rem;
		color: var(--color-text-muted);
	}
</style>
