<script lang="ts">
	// No props — pure presentational skeleton loader
</script>

<div class="skeleton-card" aria-hidden="true">
	<div class="skeleton-header">
		<div class="skeleton-avatar"></div>
		<div class="skeleton-meta">
			<div class="skeleton-line skeleton-name"></div>
			<div class="skeleton-line skeleton-username"></div>
		</div>
	</div>
	<div class="skeleton-body">
		<div class="skeleton-line skeleton-text-long"></div>
		<div class="skeleton-line skeleton-text-medium"></div>
		<div class="skeleton-line skeleton-text-short"></div>
	</div>
	<div class="skeleton-footer">
		<div class="skeleton-action"></div>
		<div class="skeleton-action"></div>
		<div class="skeleton-timestamp"></div>
	</div>
</div>

<style>
	.skeleton-card {
		padding: 1.25rem 0;
		border-bottom: 1px solid var(--color-border);
	}

	.skeleton-header {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		margin-bottom: 0.875rem;
	}

	.skeleton-avatar {
		width: 2.5rem;
		height: 2.5rem;
		border-radius: 50%;
		background: var(--color-skeleton);
		animation: shimmer 1.6s ease-in-out infinite;
		flex-shrink: 0;
	}

	.skeleton-meta {
		display: flex;
		flex-direction: column;
		gap: 0.375rem;
		flex: 1;
	}

	.skeleton-line {
		border-radius: 4px;
		background: var(--color-skeleton);
		animation: shimmer 1.6s ease-in-out infinite;
	}

	.skeleton-name {
		height: 0.875rem;
		width: 9rem;
	}

	.skeleton-username {
		height: 0.75rem;
		width: 6rem;
	}

	.skeleton-body {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
		margin-bottom: 0.875rem;
		padding-left: 3.25rem;
	}

	.skeleton-text-long {
		height: 0.875rem;
		width: 100%;
	}

	.skeleton-text-medium {
		height: 0.875rem;
		width: 78%;
	}

	.skeleton-text-short {
		height: 0.875rem;
		width: 52%;
	}

	.skeleton-footer {
		display: flex;
		align-items: center;
		gap: 1.25rem;
		padding-left: 3.25rem;
	}

	.skeleton-action {
		height: 0.75rem;
		width: 3rem;
		border-radius: 4px;
		background: var(--color-skeleton);
		animation: shimmer 1.6s ease-in-out infinite;
	}

	.skeleton-timestamp {
		height: 0.75rem;
		width: 5rem;
		border-radius: 4px;
		background: var(--color-skeleton);
		animation: shimmer 1.6s ease-in-out infinite;
		margin-left: auto;
	}

	@keyframes shimmer {
		0%,
		100% {
			opacity: 0.5;
		}
		50% {
			opacity: 1;
		}
	}
</style>
