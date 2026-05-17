<script lang="ts">
	import { formatCount } from '$lib/utils/formatCount';

	interface Props {
		postsCount: number | null;
		followersCount: number | null;
		followingCount: number | null;
		onOpenFollowers: () => void;
		onOpenFollowing: () => void;
	}

	let {
		postsCount,
		followersCount,
		followingCount,
		onOpenFollowers,
		onOpenFollowing,
	}: Props = $props();
</script>

<div class="stats" role="group" aria-label="Profile counts">
	<div class="stat">
		<span class="value">{formatCount(postsCount)}</span>
		<span class="label">posts</span>
	</div>
	<button
		class="stat stat--btn"
		type="button"
		onclick={onOpenFollowers}
		aria-label="View followers"
	>
		<span class="value">{formatCount(followersCount)}</span>
		<span class="label">followers</span>
	</button>
	<button
		class="stat stat--btn"
		type="button"
		onclick={onOpenFollowing}
		aria-label="View following"
	>
		<span class="value">{formatCount(followingCount)}</span>
		<span class="label">following</span>
	</button>
</div>

<style>
	.stats {
		display: flex;
		flex-wrap: wrap;
		gap: 1.25rem;
		align-items: baseline;
	}

	.stat {
		display: inline-flex;
		align-items: baseline;
		gap: 0.3125rem;
		padding: 0;
		background: none;
		border: none;
		font-family: inherit;
		color: inherit;
	}

	.stat--btn {
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
	}

	.stat--btn:hover .label,
	.stat--btn:focus-visible .label {
		text-decoration: underline;
	}

	.stat--btn:focus-visible {
		outline: 2px solid var(--color-text-muted);
		outline-offset: 3px;
		border-radius: 2px;
	}

	.value {
		font-size: 0.9375rem;
		font-weight: 700;
		color: var(--color-text-primary);
		font-variant-numeric: tabular-nums;
	}

	.label {
		font-size: 0.875rem;
		color: var(--color-text-muted);
	}
</style>
