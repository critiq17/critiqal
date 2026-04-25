<script lang="ts">
	interface Props {
		value: string;
		disabled: boolean;
		placeholder?: string;
		onValueChange: (v: string) => void;
		onInput?: (el: HTMLTextAreaElement) => void;
		bindEl?: (el: HTMLTextAreaElement) => void;
	}

	let {
		value,
		disabled,
		placeholder = "What's on your mind?",
		onValueChange,
		onInput,
		bindEl
	}: Props = $props();

	function handleInput(e: Event): void {
		const el = e.target as HTMLTextAreaElement;
		onValueChange(el.value);
		onInput?.(el);
	}

	function attachRef(el: HTMLTextAreaElement): void {
		bindEl?.(el);
	}
</script>

<textarea
	class="composer-textarea"
	{value}
	oninput={handleInput}
	use:attachRef
	{placeholder}
	{disabled}
	aria-label="Post content"
	autocomplete="off"
	spellcheck="true"
></textarea>

<style>
	.composer-textarea {
		background: none;
		border: none;
		outline: none;
		font-size: 17px;
		line-height: 1.55;
		color: var(--tg-text, #f0f0f0);
		width: 100%;
		resize: none;
		overflow-y: hidden;
		min-height: 120px;
		font-family: inherit;
		box-sizing: border-box;
		caret-color: var(--tg-accent, #e05252);
	}

	.composer-textarea::placeholder {
		color: var(--tg-hint, rgba(255, 255, 255, 0.3));
	}

	.composer-textarea:disabled {
		opacity: 0.6;
	}
</style>
