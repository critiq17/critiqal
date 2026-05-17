<style>
	.profile-skeleton {
		display: flex;
		align-items: flex-start;
		gap: 1rem;
		padding: 1.5rem 0;
	}

	.skeleton-avatar-lg {
		width: 5rem;
		height: 5rem;
		border-radius: 50%;
		background: var(--color-skeleton);
		animation: shimmer 1.6s ease-in-out infinite;
		flex-shrink: 0;
	}

	.skeleton-info {
		flex: 1;
		padding-top: 0.5rem;
	}

	.skeleton-line {
		border-radius: 4px;
		background: var(--color-skeleton);
		animation: shimmer 1.6s ease-in-out infinite;
		display: block;
	}

	@keyframes shimmer {
		0%, 100% { opacity: 0.5; }
		50% { opacity: 1; }
	}
</style>

<div class="profile-skeleton" aria-hidden="true">
	<div class="skeleton-avatar-lg"></div>
	<div class="skeleton-info">
		<div class="skeleton-line" style="width: 8rem; height: 1rem;"></div>
		<div class="skeleton-line" style="width: 5rem; height: 0.75rem; margin-top: 0.25rem;"></div>
		<div class="skeleton-line" style="width: 100%; height: 0.75rem; margin-top: 0.875rem;"></div>
		<div class="skeleton-line" style="width: 72%; height: 0.75rem; margin-top: 0.375rem;"></div>
	</div>
</div>
