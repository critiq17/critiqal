<script lang="ts">
	interface Props {
		value: string;
		submitting: boolean;
		placeholder?: string;
		onValueChange: (v: string) => void;
		onSubmit: () => void;
	}

	let { value, submitting, placeholder = 'Add a comment…', onValueChange, onSubmit }: Props =
		$props();

	function handleKeydown(e: KeyboardEvent): void {
		if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
			onSubmit();
		}
	}
</script>

<div class="comment-compose">
	<div class="comment-compose-inner">
		<textarea
			class="comment-input"
			value={value}
			oninput={(e) => onValueChange((e.target as HTMLTextAreaElement).value)}
			onkeydown={handleKeydown}
			{placeholder}
			rows={1}
			disabled={submitting}
			aria-label="Write a comment"
		></textarea>
		<button
			class="comment-submit-btn"
			onclick={onSubmit}
			disabled={!value.trim() || submitting}
			aria-label="Post comment"
		>
			{submitting ? '…' : 'Post'}
		</button>
	</div>
</div>

<style>
	.comment-compose {
		padding-top: 0.5rem;
		padding-right: 1rem;
		padding-bottom: 0.875rem;
	}

	.comment-compose-inner {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		border: 1px solid var(--color-border);
		border-radius: 1.5rem;
		padding: 0.4375rem 0.5rem 0.4375rem 1rem;
		background: var(--color-surface-raised);
		transition: border-color 0.15s ease;
	}

	.comment-compose-inner:focus-within {
		border-color: var(--color-text-muted);
	}

	.comment-input {
		flex: 1;
		background: none;
		border: none;
		outline: none;
		font-size: 0.875rem;
		color: var(--color-text-primary);
		font-family: inherit;
		resize: none;
		line-height: 1.5;
		min-height: 1.5em;
		padding: 0;
	}

	.comment-input::placeholder {
		color: var(--color-text-muted);
		opacity: 0.5;
	}

	.comment-input:disabled {
		opacity: 0.6;
	}

	.comment-submit-btn {
		background: var(--color-text-primary);
		color: var(--color-bg);
		border: none;
		border-radius: 9999px;
		padding: 0.3125rem 0.75rem;
		font-size: 0.8125rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		white-space: nowrap;
		flex-shrink: 0;
		transition: opacity 0.15s ease, transform 0.1s ease;
	}

	.comment-submit-btn:disabled {
		opacity: 0.35;
		cursor: not-allowed;
	}

	.comment-submit-btn:not(:disabled):hover {
		opacity: 0.85;
	}
</style>
