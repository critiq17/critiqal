<script lang="ts">
	import { onMount } from 'svelte';

	interface Props {
		value?: string;
		placeholder?: string;
		disabled?: boolean;
		onchange?: (v: string) => void;
		// 'popover' anchors a compact panel to the trigger (desktop default).
		// 'modal' centers a larger, touch-friendly calendar — used in the mini-app.
		present?: 'popover' | 'modal';
	}

	let {
		value = $bindable(''),
		placeholder = 'Select date & time',
		disabled = false,
		onchange,
		present = 'popover'
	}: Props = $props();

	let open = $state(false);
	let wrap: HTMLElement;
	let panelStyle = $state('');

	const today = new Date();
	let viewYear = $state(today.getFullYear());
	let viewMonth = $state(today.getMonth());

	let pickedYear = $state<number | null>(null);
	let pickedMonth = $state<number | null>(null);
	let pickedDay = $state<number | null>(null);
	let pickedHour = $state(12);
	let pickedMin = $state(0);

	const MONTHS = [
		'January', 'February', 'March', 'April', 'May', 'June',
		'July', 'August', 'September', 'October', 'November', 'December'
	];
	const DAYS = ['Su', 'Mo', 'Tu', 'We', 'Th', 'Fr', 'Sa'];

	$effect(() => {
		if (!value) {
			pickedYear = null;
			pickedMonth = null;
			pickedDay = null;
			return;
		}
		const d = new Date(value);
		if (isNaN(d.getTime())) return;
		pickedYear = d.getFullYear();
		pickedMonth = d.getMonth();
		pickedDay = d.getDate();
		pickedHour = d.getHours();
		pickedMin = d.getMinutes();
		viewYear = d.getFullYear();
		viewMonth = d.getMonth();
	});

	const cells = $derived.by(() => {
		const firstDow = new Date(viewYear, viewMonth, 1).getDay();
		const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
		const arr: (number | null)[] = Array(firstDow).fill(null);
		for (let d = 1; d <= daysInMonth; d++) arr.push(d);
		return arr;
	});

	const displayLabel = $derived.by(() => {
		if (pickedYear === null || pickedDay === null) return '';
		const d = new Date(pickedYear, pickedMonth!, pickedDay, pickedHour, pickedMin);
		return d.toLocaleString(undefined, {
			weekday: 'short',
			month: 'short',
			day: 'numeric',
			hour: '2-digit',
			minute: '2-digit'
		});
	});

	const hourDisplay = $derived(String(pickedHour).padStart(2, '0'));
	const minDisplay = $derived(String(pickedMin).padStart(2, '0'));

	function prevMonth() {
		if (viewMonth === 0) { viewYear--; viewMonth = 11; } else viewMonth--;
	}

	function nextMonth() {
		if (viewMonth === 11) { viewYear++; viewMonth = 0; } else viewMonth++;
	}

	function pickDay(d: number) {
		pickedYear = viewYear;
		pickedMonth = viewMonth;
		pickedDay = d;
	}

	function clampHour(raw: string) {
		const n = parseInt(raw, 10);
		pickedHour = isNaN(n) ? 0 : Math.max(0, Math.min(23, n));
	}

	function clampMin(raw: string) {
		const n = parseInt(raw, 10);
		pickedMin = isNaN(n) ? 0 : Math.max(0, Math.min(59, n));
	}

	function pad(n: number) {
		return String(n).padStart(2, '0');
	}

	function confirm() {
		if (pickedYear === null || pickedDay === null) return;
		const local = `${pickedYear}-${pad(pickedMonth! + 1)}-${pad(pickedDay)}T${pad(pickedHour)}:${pad(pickedMin)}`;
		value = local;
		onchange?.(local);
		open = false;
	}

	function clear() {
		pickedYear = null;
		pickedMonth = null;
		pickedDay = null;
		value = '';
		onchange?.('');
		open = false;
	}

	function isToday(d: number) {
		return d === today.getDate() && viewMonth === today.getMonth() && viewYear === today.getFullYear();
	}

	function isPicked(d: number) {
		return d === pickedDay && viewMonth === pickedMonth && viewYear === pickedYear;
	}

	function computeStyle(): string {
		if (!wrap) return '';
		const rect = wrap.getBoundingClientRect();
		const panelH = 320;
		const spaceBelow = window.innerHeight - rect.bottom;
		const topVal = spaceBelow < panelH && rect.top > panelH
			? rect.top - panelH - 8
			: rect.bottom + 8;
		const leftVal = Math.min(rect.left, window.innerWidth - 264);
		return `top:${topVal}px;left:${leftVal}px;`;
	}

	// Teleport action — moves node to document.body, removing it from any
	// backdrop-filter stacking context so z-index applies in the root context.
	function portal(node: HTMLElement) {
		document.body.appendChild(node);
		return {
			destroy() {
				node.remove();
			}
		};
	}

	$effect(() => {
		if (!open || present === 'modal') return;
		panelStyle = computeStyle();
		const update = () => { panelStyle = computeStyle(); };
		window.addEventListener('scroll', update, { capture: true, passive: true });
		window.addEventListener('resize', update, { passive: true });
		return () => {
			window.removeEventListener('scroll', update, { capture: true });
			window.removeEventListener('resize', update);
		};
	});
</script>

<div class="dtp" bind:this={wrap}>
	<button
		type="button"
		class="dtp-trigger"
		class:filled={!!displayLabel}
		{disabled}
		onclick={() => { if (!disabled) open = !open; }}
	>
		<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="12" height="12" aria-hidden="true">
			<rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
			<line x1="16" y1="2" x2="16" y2="6"/>
			<line x1="8" y1="2" x2="8" y2="6"/>
			<line x1="3" y1="10" x2="21" y2="10"/>
		</svg>
		<span>{displayLabel || placeholder}</span>
	</button>

	{#if open}
		<!-- Scrim and panel are teleported to document.body to escape any
		     backdrop-filter stacking context. Scrim handles all outside clicks. -->
		<!-- svelte-ignore a11y_no_static_element_interactions -->
		<!-- svelte-ignore a11y_click_events_have_key_events -->
		<div
			class="dtp-scrim"
			class:dtp-scrim-modal={present === 'modal'}
			use:portal
			onclick={() => { open = false; }}
			aria-hidden="true"
		></div>
		<div
			class="dtp-panel"
			class:dtp-panel-modal={present === 'modal'}
			use:portal
			style={present === 'modal' ? '' : panelStyle}
			role="dialog"
			aria-label="Pick date and time"
		>
			<div class="dtp-nav">
				<button type="button" class="dtp-arrow" onclick={prevMonth} aria-label="Previous month">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="12" height="12">
						<polyline points="15 18 9 12 15 6"/>
					</svg>
				</button>
				<span class="dtp-month">{MONTHS[viewMonth]} {viewYear}</span>
				<button type="button" class="dtp-arrow" onclick={nextMonth} aria-label="Next month">
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" width="12" height="12">
						<polyline points="9 18 15 12 9 6"/>
					</svg>
				</button>
			</div>

			<div class="dtp-grid">
				{#each DAYS as day}
					<span class="dtp-dow">{day}</span>
				{/each}
				{#each cells as cell}
					{#if cell === null}
						<span></span>
					{:else}
						<button
							type="button"
							class="dtp-day"
							class:dtp-today={isToday(cell)}
							class:dtp-picked={isPicked(cell)}
							onclick={() => pickDay(cell)}
						>{cell}</button>
					{/if}
				{/each}
			</div>

			<div class="dtp-time">
				<span class="dtp-time-lbl">Time</span>
				<div class="dtp-time-ctrl">
					<input
						type="number"
						class="dtp-num"
						min="0"
						max="23"
						value={hourDisplay}
						oninput={(e) => clampHour((e.target as HTMLInputElement).value)}
						aria-label="Hour"
					/>
					<span class="dtp-colon">:</span>
					<input
						type="number"
						class="dtp-num"
						min="0"
						max="59"
						step="5"
						value={minDisplay}
						oninput={(e) => clampMin((e.target as HTMLInputElement).value)}
						aria-label="Minute"
					/>
				</div>
			</div>

			<div class="dtp-actions">
				{#if pickedDay !== null}
					<button type="button" class="dtp-clear" onclick={clear}>Clear</button>
				{:else}
					<span></span>
				{/if}
				<button
					type="button"
					class="dtp-confirm"
					onclick={confirm}
					disabled={pickedDay === null}
				>Confirm</button>
			</div>
		</div>
	{/if}
</div>

<style>
	.dtp {
		position: relative;
		display: block;
	}

	.dtp-trigger {
		display: inline-flex;
		align-items: center;
		gap: 6px;
		background: none;
		border: none;
		padding: 0;
		font-family: inherit;
		font-size: 0.83rem;
		color: var(--color-text-muted);
		cursor: pointer;
		opacity: 0.5;
		transition: opacity 0.12s;
		text-align: left;
	}

	.dtp-trigger.filled {
		opacity: 1;
		color: var(--color-text-primary);
	}

	.dtp-trigger:not(:disabled):hover { opacity: 0.8; }
	.dtp-trigger.filled:not(:disabled):hover { opacity: 0.85; }
	.dtp-trigger:disabled { cursor: not-allowed; }

	/* Scrim — blocks clicks through to backdrop-filter elements */
	:global(.dtp-scrim) {
		position: fixed;
		inset: 0;
		z-index: 9998;
		background: transparent;
		cursor: default;
	}

	/* Panel — above everything in root stacking context */
	:global(.dtp-panel) {
		position: fixed;
		z-index: 9999;
		width: 248px;
		padding: 14px;
		border-radius: 18px;
		background: var(--glass-bg-soft, rgba(20, 20, 22, 0.92));
		backdrop-filter: blur(24px) saturate(160%);
		-webkit-backdrop-filter: blur(24px) saturate(160%);
		border: 1px solid var(--glass-border, rgba(255,255,255,0.1));
		box-shadow:
			inset 0 1px 0 var(--glass-highlight, rgba(255,255,255,0.08)),
			0 20px 60px rgba(0,0,0,0.5);
	}

	/* ── Modal variant — centered, larger touch targets (mini-app) ───────────── */
	:global(.dtp-scrim-modal) {
		background: rgba(0, 0, 0, 0.55);
		backdrop-filter: blur(var(--glass-blur-scrim, 8px));
		-webkit-backdrop-filter: blur(var(--glass-blur-scrim, 8px));
		z-index: 10000;
		animation: dtp-fade 0.18s ease;
	}

	:global(.dtp-panel-modal) {
		top: 50%;
		left: 50%;
		transform: translate(-50%, -50%);
		width: min(360px, 92vw);
		z-index: 10001;
		padding: 20px;
		border-radius: 24px;
		animation: dtp-pop 0.22s cubic-bezier(0.34, 1.56, 0.64, 1);
	}

	@keyframes dtp-fade {
		from { opacity: 0; }
		to { opacity: 1; }
	}

	@keyframes dtp-pop {
		from { opacity: 0; transform: translate(-50%, -50%) scale(0.94); }
		to { opacity: 1; transform: translate(-50%, -50%) scale(1); }
	}

	@media (prefers-reduced-motion: reduce) {
		:global(.dtp-scrim-modal),
		:global(.dtp-panel-modal) { animation: none; }
	}

	:global(.dtp-panel-modal .dtp-nav) { margin-bottom: 16px; }
	:global(.dtp-panel-modal .dtp-month) { font-size: 1rem; }
	:global(.dtp-panel-modal .dtp-arrow) { padding: 8px; }
	:global(.dtp-panel-modal .dtp-arrow svg) { width: 16px; height: 16px; }
	:global(.dtp-panel-modal .dtp-grid) { gap: 3px; }
	:global(.dtp-panel-modal .dtp-dow) { font-size: 0.78rem; padding: 6px 0 10px; }
	:global(.dtp-panel-modal .dtp-day) { font-size: 0.95rem; }
	:global(.dtp-panel-modal .dtp-time) { margin-top: 16px; padding-top: 16px; }
	:global(.dtp-panel-modal .dtp-time-lbl) { font-size: 0.85rem; }
	:global(.dtp-panel-modal .dtp-num) {
		width: 52px;
		font-size: 1rem;
		padding: 8px;
	}
	:global(.dtp-panel-modal .dtp-colon) { font-size: 1rem; }
	:global(.dtp-panel-modal .dtp-actions) { margin-top: 16px; }
	:global(.dtp-panel-modal .dtp-clear) { font-size: 0.85rem; padding: 8px 12px; }
	:global(.dtp-panel-modal .dtp-confirm) {
		font-size: 0.9rem;
		padding: 9px 22px;
	}

	:global(.dtp-nav) {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-bottom: 10px;
	}

	:global(.dtp-month) {
		font-size: 0.8rem;
		font-weight: 600;
		color: var(--color-text-primary);
	}

	:global(.dtp-arrow) {
		background: none;
		border: none;
		cursor: pointer;
		padding: 4px;
		border-radius: 7px;
		color: var(--color-text-muted);
		display: flex;
		align-items: center;
		justify-content: center;
		transition: background-color 0.1s, color 0.1s;
	}

	:global(.dtp-arrow:hover) {
		background: rgba(255,255,255,0.07);
		color: var(--color-text-primary);
	}

	:global(.dtp-grid) {
		display: grid;
		grid-template-columns: repeat(7, 1fr);
		gap: 1px;
	}

	:global(.dtp-dow) {
		font-size: 0.65rem;
		text-align: center;
		color: var(--color-text-muted);
		opacity: 0.4;
		padding: 4px 0 6px;
		font-weight: 500;
		letter-spacing: 0.02em;
	}

	:global(.dtp-day) {
		aspect-ratio: 1;
		border: none;
		background: none;
		cursor: pointer;
		font-size: 0.76rem;
		font-family: inherit;
		color: var(--color-text-primary);
		border-radius: 50%;
		display: flex;
		align-items: center;
		justify-content: center;
		transition: background-color 0.1s;
		padding: 0;
		width: 100%;
	}

	:global(.dtp-day:hover:not(.dtp-picked)) {
		background: rgba(255,255,255,0.08);
	}

	:global(.dtp-today) {
		color: var(--color-accent, #e05252);
		font-weight: 700;
	}

	:global(.dtp-picked) {
		background: var(--color-text-primary) !important;
		color: var(--color-bg);
		font-weight: 600;
	}

	:global(.dtp-time) {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 10px;
		padding-top: 10px;
		border-top: 1px solid rgba(255,255,255,0.06);
	}

	:global(.dtp-time-lbl) {
		font-size: 0.75rem;
		color: var(--color-text-muted);
		opacity: 0.6;
	}

	:global(.dtp-time-ctrl) {
		display: flex;
		align-items: center;
		gap: 4px;
	}

	:global(.dtp-num) {
		width: 40px;
		background: rgba(255,255,255,0.06);
		border: 1px solid rgba(255,255,255,0.1);
		border-radius: 8px;
		color: var(--color-text-primary);
		font-size: 0.82rem;
		font-family: inherit;
		font-weight: 500;
		text-align: center;
		padding: 4px;
		outline: none;
		appearance: textfield;
		-moz-appearance: textfield;
		transition: border-color 0.12s;
	}

	:global(.dtp-num:focus) { border-color: rgba(255,255,255,0.22); }

	:global(.dtp-num::-webkit-outer-spin-button),
	:global(.dtp-num::-webkit-inner-spin-button) {
		-webkit-appearance: none;
	}

	:global(.dtp-colon) {
		font-size: 0.85rem;
		color: var(--color-text-muted);
		opacity: 0.5;
	}

	:global(.dtp-actions) {
		display: flex;
		align-items: center;
		justify-content: space-between;
		margin-top: 10px;
	}

	:global(.dtp-clear) {
		background: none;
		border: none;
		cursor: pointer;
		font-size: 0.75rem;
		font-family: inherit;
		color: var(--color-text-muted);
		padding: 5px 8px;
		border-radius: 8px;
		transition: color 0.12s;
	}

	:global(.dtp-clear:hover) { color: var(--color-text-primary); }

	:global(.dtp-confirm) {
		background: var(--color-text-primary);
		color: var(--color-bg);
		border: none;
		border-radius: 999px;
		padding: 5px 14px;
		font-size: 0.76rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition: opacity 0.12s;
	}

	:global(.dtp-confirm:disabled) { opacity: 0.25; cursor: not-allowed; }
	:global(.dtp-confirm:not(:disabled):hover) { opacity: 0.82; }
</style>
