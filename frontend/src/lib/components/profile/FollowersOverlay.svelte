<script lang="ts">
	import { onMount } from 'svelte';
	import { getTelegramWebApp } from '$lib/telegram';
	import { notifyOverlaySwipe } from '$lib/overlay-swipe';
	import { getInitials } from '$lib/utils/getInitials';
	import type { User } from '$lib/types';

	interface Props {
		open: boolean;
		type: 'followers' | 'following' | null;
		list: User[];
		listsLoading: boolean;
		onClose: () => void;
	}

	let { open, type, list, listsLoading, onClose }: Props = $props();

	let overlayEl: HTMLElement | null = null;

	// ── Swipe-to-dismiss (same right-swipe pattern) ──────────────────────────

	let _touchStartX = 0;
	let _touchStartY = 0;
	let _dirLocked: 'h' | 'v' | null = null;
	let _lastX = 0;
	let _lastT = 0;
	let _velocity = 0;
	let _currentX = 0;

	const DISMISS_RATIO = 0.35;
	const VELOCITY_PX_MS = 0.45;

	function _applyTransform(x: number, transition: string): void {
		if (!overlayEl) return;
		overlayEl.style.transition = transition;
		overlayEl.style.transform = `translateX(${x}px)`;
	}

	function onTouchStart(): void {
		_applyTransform(_currentX, 'none');
		_touchStartX = 0;
		_dirLocked = null;
		_velocity = 0;
	}

	function onTouchMove(e: TouchEvent): void {
		const t = e.touches[0];
		if (!t) return;
		if (!_dirLocked && _touchStartX === 0) {
			_touchStartX = t.clientX;
			_touchStartY = t.clientY;
			_lastX = t.clientX;
			_lastT = Date.now();
		}
		const dx = t.clientX - _touchStartX;
		const dy = t.clientY - _touchStartY;
		if (!_dirLocked && (Math.abs(dx) > 5 || Math.abs(dy) > 5)) {
			_dirLocked = Math.abs(dx) >= Math.abs(dy) ? 'h' : 'v';
		}
		if (_dirLocked !== 'h' || dx < 0) return;
		const now = Date.now();
		const dt = now - _lastT;
		if (dt > 0) _velocity = (t.clientX - _lastX) / dt;
		_lastX = t.clientX;
		_lastT = now;
		_currentX = dx;
		if (overlayEl) overlayEl.style.transform = `translateX(${dx}px)`;
		notifyOverlaySwipe(dx, window.innerWidth, 'drag');
	}

	function onTouchEnd(): void {
		if (_currentX <= 0) return;
		const sw = window.innerWidth;
		if (_currentX > sw * DISMISS_RATIO || _velocity > VELOCITY_PX_MS) {
			getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
			_applyTransform(sw, 'transform 0.24s cubic-bezier(0.4, 0, 1, 1)');
			notifyOverlaySwipe(sw, sw, 'dismiss');
			setTimeout(onClose, 260);
		} else {
			_applyTransform(0, 'transform 0.38s cubic-bezier(0.34, 1.56, 0.64, 1)');
			notifyOverlaySwipe(0, sw, 'cancel');
		}
		_currentX = 0;
	}

	function handleBack(): void {
		getTelegramWebApp()?.HapticFeedback.impactOccurred('light');
		onClose();
	}

	// ── Slide-in on mount ────────────────────────────────────────────────────

	onMount(() => {
		if (overlayEl) {
			overlayEl.style.transform = `translateX(${window.innerWidth}px)`;
			overlayEl.style.transition = 'none';
			requestAnimationFrame(() => {
				if (!overlayEl) return;
				overlayEl.style.transition = 'transform 0.28s cubic-bezier(0.4, 0, 0.2, 1)';
				overlayEl.style.transform = 'translateX(0)';
			});
		}
	});

	// ── Telegram back button ─────────────────────────────────────────────────

	$effect(() => {
		if (!open) return;
		const tg = getTelegramWebApp();
		if (!tg) return;
		tg.BackButton.show();
		tg.BackButton.onClick(handleBack);
		return () => {
			tg.BackButton.offClick(handleBack);
			tg.BackButton.hide();
		};
	});
</script>

{#if open}
	<div
		class="overlay"
		bind:this={overlayEl}
		ontouchstart={onTouchStart}
		ontouchmove={onTouchMove}
		ontouchend={onTouchEnd}
	>
		<header class="overlay-header">
			<button class="back-btn" onclick={handleBack} aria-label="Back">
				<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
					stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
					<polyline points="15 18 9 12 15 6" />
				</svg>
			</button>
			<span class="header-title">
				{type === 'followers' ? 'Followers' : 'Following'}
			</span>
			<div class="header-spacer" aria-hidden="true"></div>
		</header>

		<div class="scroll-area mobile-scroll-container">
			{#if listsLoading}
				{#each { length: 6 } as _, i (i)}
					<div class="user-skeleton">
						<div class="skeleton-avatar"></div>
						<div class="skeleton-lines">
							<div class="skeleton-line" style="width:110px"></div>
							<div class="skeleton-line" style="width:72px;opacity:0.6"></div>
						</div>
					</div>
				{/each}
			{:else if list.length === 0}
				<div class="empty-state">
					<svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor"
						stroke-width="1.25" stroke-linecap="round" stroke-linejoin="round"
						style="color:rgba(255,255,255,0.18)" aria-hidden="true">
						<path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/>
						<circle cx="9" cy="7" r="4"/>
						<path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75"/>
					</svg>
					<p class="empty-text">
						{type === 'followers' ? 'No followers yet' : 'Not following anyone'}
					</p>
				</div>
			{:else}
				<ul class="user-list" role="list">
					{#each list as user (user.id)}
						<li role="listitem">
							<a href="/{user.username}" class="user-row" onclick={onClose}>
								<div class="user-avatar">
									{#if user.avatarUrl}
										<img src={user.avatarUrl} alt={user.username} class="avatar-img" />
									{:else}
										<span class="avatar-initial">{getInitials(user.name, user.username)}</span>
									{/if}
								</div>
								<div class="user-info">
									<span class="user-name">{user.name ?? user.username}</span>
									<span class="user-handle">@{user.username}</span>
								</div>
								<svg class="row-chevron" width="14" height="14" viewBox="0 0 24 24" fill="none"
									stroke="currentColor" stroke-width="2" aria-hidden="true">
									<polyline points="9 18 15 12 9 6" />
								</svg>
							</a>
						</li>
					{/each}
				</ul>
			{/if}

			<div class="safe-bottom" aria-hidden="true"></div>
		</div>
	</div>
{/if}

<style>
	.overlay {
		position: fixed;
		inset: 0;
		background: var(--tg-bg, var(--color-bg, #0a0a0a));
		z-index: 200;
		display: flex;
		flex-direction: column;
		will-change: transform;
		overflow: hidden;
	}

	/* ── Header ────────────────────────────────────────────────────────────── */

	.overlay-header {
		display: flex;
		align-items: center;
		justify-content: space-between;
		padding: 0 4px;
		padding-top: var(--tg-content-top, var(--tg-content-safe-area-inset-top, env(safe-area-inset-top, 0px)));
		height: calc(52px + var(--tg-content-top, var(--tg-content-safe-area-inset-top, env(safe-area-inset-top, 0px))));
		border-bottom: 1px solid rgba(255, 255, 255, 0.06);
		flex-shrink: 0;
	}

	.back-btn {
		background: none;
		border: none;
		cursor: pointer;
		color: rgba(240, 240, 240, 0.6);
		padding: 10px 12px;
		min-width: 44px;
		min-height: 44px;
		display: flex;
		align-items: center;
		justify-content: center;
		-webkit-tap-highlight-color: transparent;
		transition: opacity 0.15s;
	}

	.back-btn:active { opacity: 0.5; }

	.header-title {
		font-size: 1rem;
		font-weight: 600;
		color: var(--tg-text, #f0f0f0);
		letter-spacing: -0.01em;
	}

	.header-spacer { width: 44px; }

	/* ── Scroll ────────────────────────────────────────────────────────────── */

	.scroll-area {
		flex: 1;
		overflow-y: auto;
		-webkit-overflow-scrolling: touch;
		padding-top: 0 !important;
		padding-bottom: 0 !important;
	}

	/* ── Empty ─────────────────────────────────────────────────────────────── */

	.empty-state {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 12px;
		padding: 60px 16px;
	}

	.empty-text {
		font-size: 14px;
		color: rgba(255, 255, 255, 0.3);
		margin: 0;
	}

	/* ── User list ─────────────────────────────────────────────────────────── */

	.user-list {
		list-style: none;
		padding: 0;
		margin: 0;
	}

	.user-row {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 12px 16px;
		text-decoration: none;
		border-bottom: 1px solid rgba(255, 255, 255, 0.05);
		-webkit-tap-highlight-color: transparent;
		transition: background-color 0.12s;
	}

	.user-row:active { background: rgba(255, 255, 255, 0.04); }

	.user-avatar {
		width: 42px;
		height: 42px;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.07);
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
		flex-shrink: 0;
	}

	.avatar-img { width: 100%; height: 100%; object-fit: cover; display: block; }

	.avatar-initial {
		font-size: 15px;
		font-weight: 600;
		color: rgba(255, 255, 255, 0.5);
		user-select: none;
	}

	.user-info {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 2px;
	}

	.user-name {
		font-size: 15px;
		font-weight: 500;
		color: var(--tg-text, #f0f0f0);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.user-handle {
		font-size: 12px;
		color: rgba(255, 255, 255, 0.38);
	}

	.row-chevron {
		color: rgba(255, 255, 255, 0.18);
		flex-shrink: 0;
	}

	/* ── Skeleton ──────────────────────────────────────────────────────────── */

	.user-skeleton {
		display: flex;
		align-items: center;
		gap: 12px;
		padding: 12px 16px;
		border-bottom: 1px solid rgba(255, 255, 255, 0.04);
	}

	.skeleton-avatar {
		width: 42px;
		height: 42px;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.07);
		flex-shrink: 0;
		animation: pulse 1.4s ease-in-out infinite;
	}

	.skeleton-lines {
		display: flex;
		flex-direction: column;
		gap: 7px;
		flex: 1;
	}

	.skeleton-line {
		height: 12px;
		border-radius: 4px;
		background: rgba(255, 255, 255, 0.07);
		animation: pulse 1.4s ease-in-out infinite;
	}

	@keyframes pulse {
		0%, 100% { opacity: 1; }
		50% { opacity: 0.4; }
	}

	.safe-bottom {
		height: calc(16px + env(safe-area-inset-bottom, 0px));
	}
</style>
