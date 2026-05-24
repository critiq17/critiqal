<script lang="ts">
	import StarMark from '$lib/ui/StarMark.svelte';

	interface Props {
		isOwnProfile: boolean;
	}

	let { isOwnProfile }: Props = $props();

	const title = $derived(isOwnProfile ? 'No posts yet' : 'Nothing here yet');
	const subtitle = $derived(
		isOwnProfile
			? "Your first post is the hardest. After that, just say what's true."
			: 'When this person posts, you’ll see it here.'
	);
</script>

<div class="empty" role="status" aria-live="polite">
	<div class="brand-mark" aria-hidden="true">
		<StarMark size={48} />
	</div>
	<p class="title">{title}</p>
	<p class="subtitle">{subtitle}</p>
</div>

<style>
	.empty {
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		gap: 0.5rem;
		padding: 4rem 1.5rem 5rem;
		text-align: center;
		color: var(--color-text-muted);
		animation: fadeIn 0.25s ease-out;
	}

	.brand-mark {
		opacity: 0.5;
		margin-bottom: 0.5rem;
	}

	.title {
		margin: 0;
		font-size: 1rem;
		font-weight: 600;
		color: var(--color-text-primary);
		letter-spacing: -0.005em;
	}

	.subtitle {
		margin: 0;
		max-width: 18rem;
		font-size: 0.875rem;
		line-height: 1.5;
		color: var(--color-text-muted);
	}

	@keyframes fadeIn {
		from { opacity: 0; transform: translateY(0.25rem); }
		to { opacity: 1; transform: translateY(0); }
	}

	@media (prefers-reduced-motion: reduce) {
		.empty {
			animation: none;
		}
	}
</style>
