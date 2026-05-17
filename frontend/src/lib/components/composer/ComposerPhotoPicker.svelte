<script lang="ts">
	interface Props {
		disabled: boolean;
		onFiles: (files: File[]) => void;
		bindEl?: (el: HTMLInputElement) => void;
	}

	let { disabled, onFiles, bindEl }: Props = $props();

	function handleChange(e: Event): void {
		const input = e.target as HTMLInputElement;
		if (!input.files?.length) return;
		onFiles(Array.from(input.files));
		input.value = '';
	}

	function attachRef(el: HTMLInputElement): void {
		bindEl?.(el);
	}
</script>

<button
	class="footer-icon-btn glass"
	onclick={() => {
		const el = document.querySelector<HTMLInputElement>('.composer-photo-input-hidden');
		el?.click();
	}}
	{disabled}
	type="button"
	aria-label="Add photo"
>
	<svg
		viewBox="0 0 24 24"
		fill="none"
		stroke="currentColor"
		stroke-width="1.75"
		width="22"
		height="22"
		aria-hidden="true"
	>
		<path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
		<circle cx="12" cy="13" r="4" />
	</svg>
</button>

<input
	class="composer-photo-input-hidden sr-only"
	type="file"
	accept="image/*"
	multiple
	onchange={handleChange}
	{disabled}
	aria-hidden="true"
	tabindex="-1"
	use:attachRef
/>

<style>
	.sr-only {
		position: absolute;
		width: 1px;
		height: 1px;
		padding: 0;
		margin: -1px;
		overflow: hidden;
		clip: rect(0, 0, 0, 0);
		white-space: nowrap;
		border: 0;
	}

	/* Black glass action button — no accent colour. */
	.footer-icon-btn {
		cursor: pointer;
		width: 42px;
		height: 42px;
		padding: 0;
		color: rgba(255, 255, 255, 0.75);
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 14px;
		-webkit-tap-highlight-color: transparent;
		transition:
			color 0.15s ease,
			transform 0.16s cubic-bezier(0.34, 1.56, 0.64, 1),
			background-color 0.15s ease;
	}

	.footer-icon-btn:active {
		color: #fff;
		transform: scale(0.9);
		background-color: rgba(255, 255, 255, 0.06);
	}

	.footer-icon-btn:disabled {
		opacity: 0.35;
		cursor: not-allowed;
	}
</style>
