<script lang="ts">
	import { onMount } from 'svelte';
	import { getTelegramWebApp } from '$lib/telegram';

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

	// ── Swipe-down to dismiss ────────────────────────────────────────────────

	let _startY = 0;
	let _currentY = 0;
	let _dirLocked: 'v' | 'h' | null = null;
	let _lastY = 0;
	let _lastT = 0;
	let _velocity = 0;

	const DISMISS_RATIO = 0.3;
	const VELOCITY_PX_MS = 0.5;

	function _applyTransform(y: number, transition: string): void {
		if (!panelEl) return;
		panelEl.style.transition = transition;
		panelEl.style.transform = `translateY(${y}px)`;
		if (backdropEl) {
			const h = panelEl.offsetHeight || window.innerHeight * 0.7;
			const opacity = Math.max(0, 1 - y / h);
			backdropEl.style.opacity = String(opacity);
		}
	}

	function onTouchStart(e: TouchEvent): void {
		const t = e.touches[0];
		if (!t) return;
		_applyTransform(_currentY, 'none');
		_startY = t.clientY;
		_dirLocked = null;
		_velocity = 0;
		_lastY = t.clientY;
		_lastT = Date.now();
	}

	function onTouchMove(e: TouchEvent): void {
		const t = e.touches[0];
		if (!t) return;
		const dy = t.clientY - _startY;
		const dx = Math.abs(t.clientX - (e.touches[0]?.clientX ?? t.clientX));
		if (!_dirLocked && (Math.abs(dy) > 5 || Math.abs(dx) > 5)) {
			_dirLocked = Math.abs(dy) >= Math.abs(dx) ? 'v' : 'h';
		}
		if (_dirLocked !== 'v' || dy < 0) return;
		const now = Date.now();
		const dt = now - _lastT;
		if (dt > 0) _velocity = (t.clientY - _lastY) / dt;
		_lastY = t.clientY;
		_lastT = now;
		_currentY = dy;
		_applyTransform(dy, 'none');
	}

	function onTouchEnd(): void {
		if (_currentY <= 0) return;
		const h = panelEl?.offsetHeight ?? window.innerHeight * 0.7;
		if (_currentY > h * DISMISS_RATIO || _velocity > VELOCITY_PX_MS) {
			getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
			_applyTransform(h, 'transform 0.22s cubic-bezier(0.4, 0, 1, 1)');
			setTimeout(onCancel, 230);
		} else {
			_currentY = 0;
			_applyTransform(0, 'transform 0.32s cubic-bezier(0.34, 1.56, 0.64, 1)');
		}
		_currentY = 0;
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
		const tg = getTelegramWebApp();
		if (!tg) return;
		tg.BackButton.show();
		tg.BackButton.onClick(onCancel);
		return () => {
			tg.BackButton.offClick(onCancel);
			tg.BackButton.hide();
		};
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
		class="panel"
		bind:this={panelEl}
		ontouchstart={onTouchStart}
		ontouchmove={onTouchMove}
		ontouchend={onTouchEnd}
		role="dialog"
		aria-label="Edit profile"
		aria-modal="true"
	>
		<!-- Drag handle -->
		<div class="drag-handle" aria-hidden="true"></div>

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
		background: var(--tg-bg, #111);
		border-radius: 20px 20px 0 0;
		will-change: transform;
		max-height: 90dvh;
		display: flex;
		flex-direction: column;
	}

	/* ── Drag handle ────────────────────────────────────────────────────────── */

	.drag-handle {
		width: 36px;
		height: 4px;
		border-radius: 2px;
		background: rgba(255, 255, 255, 0.18);
		margin: 10px auto 0;
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

	.cancel-btn { color: rgba(240, 240, 240, 0.5); text-align: left; }
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
		color: rgba(240, 240, 240, 0.4);
		text-transform: uppercase;
		letter-spacing: 0.06em;
	}

	.field-input,
	.field-textarea {
		width: 100%;
		box-sizing: border-box;
		background: rgba(255, 255, 255, 0.05);
		border: 1px solid rgba(255, 255, 255, 0.08);
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
	.field-textarea::placeholder { color: rgba(255, 255, 255, 0.2); }

	.field-input:focus,
	.field-textarea:focus {
		border-color: rgba(255, 255, 255, 0.2);
	}

	.field-input:disabled,
	.field-textarea:disabled { opacity: 0.45; }

	.char-count {
		font-size: 11px;
		color: rgba(255, 255, 255, 0.25);
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
