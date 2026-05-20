<script lang="ts">
	import { onMount } from 'svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import { registerSheet } from '$lib/actions/registerSheet';
	import { elasticDrag } from '$lib/actions/elasticDrag';
	import { pushBackHandler } from '$lib/tma/back-button';

	interface Props {
		open: boolean;
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
		open,
		editName,
		editBio,
		isSaving,
		saveError,
		onSave,
		onCancel,
		onNameChange,
		onBioChange
	}: Props = $props();

	let panelEl: HTMLElement | null = null;
	let backdropEl: HTMLElement | null = null;

	// Swipe-down to dismiss — physics handled by the shared elasticDrag action.
	function dismiss(): void {
		getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
		onCancel();
	}

	function fadeBackdrop(progress: number): void {
		if (backdropEl) backdropEl.style.opacity = String(1 - progress);
	}

	onMount(() => {
		if (panelEl) {
			panelEl.style.transform = `translateY(100%)`;
			panelEl.style.transition = 'none';
			requestAnimationFrame(() => {
				if (!panelEl) return;
				panelEl.style.transition = 'transform 0.3s cubic-bezier(0.4, 0, 0.2, 1)';
				panelEl.style.transform = 'translateY(0)';
			});
		}
	});

	$effect(() => {
		if (!open) return;
		return pushBackHandler(onCancel);
	});
</script>

{#if open}
	<!-- Backdrop -->
	<button
		class="backdrop"
		bind:this={backdropEl}
		onclick={onCancel}
		aria-label="Close"
		tabindex="-1"
	></button>

	<!-- Panel -->
	<div
		class="panel glass"
		use:registerSheet
		bind:this={panelEl}
		role="dialog"
		aria-label="Edit profile"
		aria-modal="true"
	>
		<!-- Drag handle — the only drag affordance so inputs/scroll are safe -->
		<div
			class="drag-handle-area"
			aria-hidden="true"
			use:elasticDrag={{
				target: () => panelEl,
				dismissDistance: 140,
				dismissVelocity: 0.5,
				onDismiss: dismiss,
				onProgress: fadeBackdrop,
				stretch: 0.04,
				stretchOrigin: 'bottom center'
			}}
		>
			<div class="drag-handle"></div>
		</div>

		<div class="panel-header">
			<button class="cancel-btn" onclick={onCancel} disabled={isSaving}>Cancel</button>
			<span class="panel-title">Edit Profile</span>
			<button class="save-btn" onclick={onSave} disabled={isSaving || !editName.trim()}>
				{isSaving ? 'Saving…' : 'Save'}
			</button>
		</div>

		<div class="panel-body">
			{#if saveError}
				<p class="error-msg" role="alert">{saveError}</p>
			{/if}

			<div class="field">
				<label class="field-label" for="edit-name">Name</label>
				<input
					id="edit-name"
					type="text"
					class="field-input"
					value={editName}
					oninput={(e) => onNameChange((e.target as HTMLInputElement).value)}
					placeholder="Your name"
					maxlength={80}
					disabled={isSaving}
					autocomplete="name"
				/>
			</div>

			<div class="field">
				<label class="field-label" for="edit-bio">Bio</label>
				<textarea
					id="edit-bio"
					class="field-textarea"
					value={editBio}
					oninput={(e) => onBioChange((e.target as HTMLTextAreaElement).value)}
					placeholder="Tell something about yourself…"
					maxlength={280}
					rows={4}
					disabled={isSaving}
				></textarea>
				<span class="char-count">{editBio.length}/280</span>
			</div>
		</div>

		<div class="safe-bottom" aria-hidden="true"></div>
	</div>
{/if}

<style>
	.backdrop {
		position: fixed;
		inset: 0;
		background: rgba(0, 0, 0, 0.55);
		z-index: 290;
		border: none;
		cursor: default;
		-webkit-tap-highlight-color: transparent;
	}

	.panel {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		z-index: 300;
		/* glass class supplies background/blur/border/shadow */
		border-radius: 20px 20px 0 0;
		will-change: transform;
		max-height: 90dvh;
		display: flex;
		flex-direction: column;
	}

	/* ── Drag handle ────────────────────────────────────────────────────────── */

	.drag-handle-area {
		display: flex;
		justify-content: center;
		padding: 8px 0 6px;
		cursor: grab;
		touch-action: none;
		flex-shrink: 0;
	}

	.drag-handle {
		width: 36px;
		height: 4px;
		border-radius: 2px;
		background: var(--text-ghost);
		flex-shrink: 0;
	}

	/* ── Panel header ───────────────────────────────────────────────────────── */

	.panel-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 14px 16px 10px;
		flex-shrink: 0;
	}

	.panel-title {
		font-size: 15px;
		font-weight: 600;
		color: var(--tg-text, #f0f0f0);
		letter-spacing: -0.01em;
	}

	.cancel-btn,
	.save-btn {
		background: none;
		border: none;
		font-family: inherit;
		font-size: 15px;
		cursor: pointer;
		padding: 4px 2px;
		min-width: 60px;
		-webkit-tap-highlight-color: transparent;
		transition: opacity 0.15s;
	}

	.cancel-btn { color: var(--text-tertiary); text-align: left; }
	.cancel-btn:active { opacity: 0.6; }

	.save-btn {
		color: var(--tg-text, #f0f0f0);
		font-weight: 600;
		text-align: right;
	}

	.save-btn:disabled { opacity: 0.35; cursor: not-allowed; }
	.save-btn:not(:disabled):active { opacity: 0.6; }

	/* ── Panel body ─────────────────────────────────────────────────────────── */

	.panel-body {
		overflow-y: auto;
		-webkit-overflow-scrolling: touch;
		padding: 8px 16px 0;
		display: flex;
		flex-direction: column;
		gap: 16px;
		flex: 1;
	}

	.field {
		display: flex;
		flex-direction: column;
		gap: 6px;
		position: relative;
	}

	.field-label {
		font-size: 12px;
		font-weight: 500;
		color: var(--text-quaternary);
		text-transform: uppercase;
		letter-spacing: 0.06em;
	}

	.field-input,
	.field-textarea {
		width: 100%;
		box-sizing: border-box;
		background: var(--surface-tint-subtle);
		border: 1px solid var(--surface-tint-medium);
		border-radius: 12px;
		padding: 12px 14px;
		font-size: 16px;
		color: var(--tg-text, #f0f0f0);
		font-family: inherit;
		outline: none;
		resize: none;
		transition: border-color 0.15s;
		-webkit-tap-highlight-color: transparent;
	}

	.field-input::placeholder,
	.field-textarea::placeholder { color: var(--text-ghost); }

	.field-input:focus,
	.field-textarea:focus {
		border-color: var(--text-ghost);
	}

	.field-input:disabled,
	.field-textarea:disabled { opacity: 0.45; }

	.char-count {
		font-size: 11px;
		color: var(--text-faint);
		text-align: right;
	}

	.error-msg {
		font-size: 13px;
		color: #e05252;
		margin: 0;
		text-align: center;
		padding: 8px 12px;
		background: rgba(224, 82, 82, 0.08);
		border-radius: 8px;
	}

	.safe-bottom {
		height: calc(20px + env(safe-area-inset-bottom, 0px));
		flex-shrink: 0;
	}
</style>
