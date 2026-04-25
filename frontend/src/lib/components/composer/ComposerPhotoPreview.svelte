<script lang="ts">
	interface Props {
		previewUrls: string[];
		onRemove: (index: number) => void;
		onView: (url: string) => void;
	}

	let { previewUrls, onRemove, onView }: Props = $props();
</script>

{#if previewUrls.length > 0}
	<div class="photo-grid" class:single={previewUrls.length === 1}>
		{#each previewUrls as url, i (url)}
			<div class="photo-card">
				<button
					class="photo-tap"
					onclick={() => onView(url)}
					aria-label="View photo {i + 1}"
					type="button"
				>
					<img src={url} alt="Photo {i + 1}" />
				</button>
				<button
					class="remove-btn"
					onclick={() => onRemove(i)}
					type="button"
					aria-label="Remove photo {i + 1}"
				>
					<svg viewBox="0 0 12 12" width="10" height="10" fill="none" aria-hidden="true">
						<path
							d="M1 1l10 10M11 1L1 11"
							stroke="currentColor"
							stroke-width="2.2"
							stroke-linecap="round"
						/>
					</svg>
				</button>
			</div>
		{/each}
	</div>
{/if}

<style>
	.photo-grid {
		display: grid;
		grid-template-columns: repeat(3, 1fr);
		gap: 6px;
	}

	.photo-grid.single {
		grid-template-columns: 1fr;
	}

	.photo-grid.single .photo-card {
		aspect-ratio: 4 / 3;
	}

	.photo-card {
		position: relative;
		aspect-ratio: 1;
		border-radius: 12px;
		overflow: visible;
	}

	.photo-tap {
		display: block;
		width: 100%;
		height: 100%;
		padding: 0;
		border: none;
		background: none;
		cursor: pointer;
		border-radius: 12px;
		overflow: hidden;
		-webkit-tap-highlight-color: transparent;
	}

	.photo-tap img {
		width: 100%;
		height: 100%;
		object-fit: cover;
		display: block;
		border-radius: 12px;
		transition: opacity 0.15s ease;
	}

	.photo-tap:active img {
		opacity: 0.8;
	}

	.remove-btn {
		position: absolute;
		top: -8px;
		right: -8px;
		width: 24px;
		height: 24px;
		border-radius: 50%;
		background: rgba(20, 20, 20, 0.9);
		border: 1.5px solid rgba(255, 255, 255, 0.15);
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		color: rgba(255, 255, 255, 0.85);
		padding: 0;
		z-index: 1;
		-webkit-tap-highlight-color: transparent;
		transition: transform 0.15s cubic-bezier(0.34, 1.56, 0.64, 1);
	}

	.remove-btn:active {
		transform: scale(0.85);
	}
</style>
