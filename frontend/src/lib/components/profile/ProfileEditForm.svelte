<script lang="ts">
	interface Props {
		editName: string;
		editBio: string;
		isSaving: boolean;
		saveError: string | null;
		onSave: () => void;
		onCancel: () => void;
		onNameChange: (v: string) => void;
		onBioChange: (v: string) => void;
	}

	let {
		editName,
		editBio,
		isSaving,
		saveError,
		onSave,
		onCancel,
		onNameChange,
		onBioChange
	}: Props = $props();
</script>

<form
	class="edit-form"
	aria-label="Edit profile"
	onsubmit={(e) => { e.preventDefault(); onSave(); }}
>
	<div class="edit-field">
		<label for="edit-name" class="edit-label">Name</label>
		<input
			id="edit-name"
			type="text"
			class="edit-input"
			value={editName}
			oninput={(e) => onNameChange((e.target as HTMLInputElement).value)}
			placeholder="Your name"
			maxlength={80}
			disabled={isSaving}
		/>
	</div>
	<div class="edit-field">
		<label for="edit-bio" class="edit-label">Bio</label>
		<textarea
			id="edit-bio"
			class="edit-textarea"
			value={editBio}
			oninput={(e) => onBioChange((e.target as HTMLTextAreaElement).value)}
			placeholder="Write something about yourself..."
			maxlength={280}
			rows={3}
			disabled={isSaving}
		></textarea>
	</div>
	{#if saveError}
		<p class="save-error" role="alert">{saveError}</p>
	{/if}
	<div class="edit-actions">
		<button type="submit" class="action-btn btn-primary" disabled={isSaving}>
			{isSaving ? 'Saving...' : 'Save'}
		</button>
		<button type="button" class="action-btn btn-outline" onclick={onCancel} disabled={isSaving}>
			Cancel
		</button>
	</div>
</form>

<style>
	.edit-form {
		display: flex;
		flex-direction: column;
		gap: 0.875rem;
		animation: fadeIn 0.15s ease-out;
	}

	.edit-field {
		display: flex;
		flex-direction: column;
		gap: 0.375rem;
	}

	.edit-label {
		font-size: 0.8125rem;
		font-weight: 500;
		color: var(--color-text-muted);
	}

	.edit-input,
	.edit-textarea {
		width: 100%;
		background: var(--color-surface-raised);
		border: 1px solid var(--color-border);
		border-radius: 0.5rem;
		padding: 0.625rem 0.75rem;
		font-size: 0.9375rem;
		color: var(--color-text-primary);
		font-family: inherit;
		outline: none;
		resize: none;
		transition:
			border-color 0.15s ease,
			box-shadow 0.15s ease;
		box-sizing: border-box;
	}

	.edit-input::placeholder,
	.edit-textarea::placeholder {
		color: var(--color-text-muted);
		opacity: 0.5;
	}

	.edit-input:focus,
	.edit-textarea:focus {
		border-color: var(--color-text-muted);
		box-shadow: 0 0 0 3px rgba(240, 240, 240, 0.06);
	}

	.edit-input:disabled,
	.edit-textarea:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}

	.save-error {
		font-size: 0.875rem;
		color: var(--color-accent);
	}

	.edit-actions {
		display: flex;
		gap: 0.625rem;
	}

	.action-btn {
		padding: 0.5rem 1.125rem;
		border-radius: 9999px;
		font-size: 0.875rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition:
			background-color 0.15s ease,
			transform 0.1s ease;
	}

	.action-btn:active:not(:disabled) {
		transform: scale(0.96);
	}

	.action-btn:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}

	.btn-primary {
		background-color: var(--color-text-primary);
		color: var(--color-bg);
		border: none;
	}

	.btn-primary:hover:not(:disabled) {
		background-color: var(--color-text-muted);
	}

	.btn-outline {
		background: none;
		color: var(--color-text-primary);
		border: 1px solid var(--color-border);
	}

	.btn-outline:hover:not(:disabled) {
		background-color: var(--color-surface-raised);
	}

	@keyframes fadeIn {
		from { opacity: 0; transform: translateY(0.25rem); }
		to { opacity: 1; transform: translateY(0); }
	}
</style>
