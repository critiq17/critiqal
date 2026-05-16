<script lang="ts">
	interface Props {
		previewUrls: string[];
		onRemove: (index: number) => void;
		onView: (url: string) => void;
	}

	let { previewUrls, onRemove, onView }: Props = $props();
</script>

{#if previewUrls.length > 0}
	<div class="photo-strip" role="list" aria-label="Attached photos">
		{#each previewUrls as url, i (url)}
			<div class="photo-card" role="listitem">
				<button
					class="photo-tap"
					onclick={() => onView(url)}
					aria-label="View photo {i + 1}"
					type="button"
				>
					<img src={url} alt="Photo {i + 1}" />
				</button>
				<button
					class="remove-btn glass"
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
	/* Horizontal, scrollable thumbnail strip — sits just above the toolbar
	   so attachments stay in view while typing. */
	.photo-strip {
		display: flex;
		gap: 10px;
		overflow-x: auto;
		overflow-y: visible;
		padding: 14px 16px 16px;
		scrollbar-width: none;
		-ms-overflow-style: none;
		scroll-snap-type: x proximity;
		-webkit-overflow-scrolling: touch;
	}

	.photo-strip::-webkit-scrollbar {
		display: none;
	}

	.photo-card {
		position: relative;
		flex: 0 0 auto;
		width: 76px;
		height: 76px;
		scroll-snap-align: start;
		animation: pop 0.26s cubic-bezier(0.34, 1.56, 0.64, 1) both;
	}

	.photo-tap {
		display: block;
		width: 100%;
		height: 100%;
		padding: 0;
		border: none;
		background: none;
		cursor: pointer;
		border-radius: 14px;
		overflow: hidden;
		-webkit-tap-highlight-color: transparent;
		box-shadow: 0 2px 10px rgba(0, 0, 0, 0.35);
		transition: transform 0.16s ease;
	}

	.photo-tap:active {
		transform: scale(0.94);
	}

	.photo-tap img {
		width: 100%;
		height: 100%;
		object-fit: cover;
		display: block;
	}

	.remove-btn {
		position: absolute;
		top: -7px;
		right: -7px;
		width: 22px;
		height: 22px;
		border-radius: 50%;
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		color: rgba(255, 255, 255, 0.9);
		padding: 0;
		z-index: 1;
		-webkit-tap-highlight-color: transparent;
		transition: transform 0.16s cubic-bezier(0.34, 1.56, 0.64, 1);
	}

	.remove-btn:active {
		transform: scale(0.82);
	}

	@keyframes pop {
		from {
			opacity: 0;
			transform: scale(0.7);
		}
		to {
			opacity: 1;
			transform: scale(1);
		}
	}
</style>
