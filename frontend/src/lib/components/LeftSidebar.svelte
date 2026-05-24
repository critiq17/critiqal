<script lang="ts">
	import { page } from '$app/stores';
	import { onMount, untrack } from 'svelte';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import StarMark from '$lib/ui/StarMark.svelte';
	import { t } from '$lib/i18n';

	function handleLogout(): void {
		authStore.logout();
	}

	function isActive(href: string): boolean {
		const path = $page.url.pathname;
		if (href === '/') return path === '/';
		return path.startsWith(href);
	}

	// Sliding glass indicator — same spring-glide pattern as the mobile BottomNav,
	// but vertical. Measured from the active nav link relative to the nav element.
	let navEl: HTMLElement | undefined = $state();
	const linkEls: HTMLAnchorElement[] = [];
	let ind = $state({ y: 0, h: 0, ready: false });

	// NEVER read `ind` here. This runs inside an $effect; reading the same
	// $state it writes creates a self-dependency → effect_update_depth_exceeded
	// (which froze the whole app on routes with no matching nav link, e.g.
	// the profile page).
	function measureIndicator(): void {
		const active = linkEls.find((el) => el?.dataset.active === 'true');
		ind = active
			? { y: active.offsetTop, h: active.offsetHeight, ready: true }
			: { y: 0, h: 0, ready: false };
	}

	// Re-measure whenever the route changes (effects run post-DOM-update).
	// untrack the write so it can never feed back into this effect.
	$effect(() => {
		void $page.url.pathname;
		void authStore.isAuthenticated;
		untrack(() => measureIndicator());
	});

	onMount(() => {
		measureIndicator();
		const ro = new ResizeObserver(() => measureIndicator());
		if (navEl) ro.observe(navEl);
		return () => ro.disconnect();
	});
</script>

<aside class="sidebar" aria-label="Navigation sidebar">
	<div class="sidebar-logo">
		<StarMark size={20} title="Critiqal" />
		<span class="logo-text">critiqal</span>
	</div>

	<nav bind:this={navEl} class="sidebar-nav" aria-label="Main navigation">
		<span
			class="nav-indicator glass"
			class:ready={ind.ready}
			aria-hidden="true"
			style:height="{ind.h}px"
			style:transform="translateY({ind.y}px)"
		></span>

		<a
			bind:this={linkEls[0]}
			href="/"
			class="nav-link"
			class:nav-link-active={isActive('/')}
			data-active={isActive('/')}
			aria-current={isActive('/') ? 'page' : undefined}
		>
			<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="nav-icon" aria-hidden="true">
				<path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z" />
				<polyline points="9 22 9 12 15 12 15 22" />
			</svg>
			<span>{t('nav.feed')}</span>
		</a>

		{#if authStore.isAuthenticated}
			<a
				bind:this={linkEls[1]}
				href="/explore"
				class="nav-link"
				class:nav-link-active={isActive('/explore')}
				data-active={isActive('/explore')}
				aria-current={isActive('/explore') ? 'page' : undefined}
			>
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="nav-icon" aria-hidden="true">
					<circle cx="11" cy="11" r="8" />
					<line x1="21" y1="21" x2="16.65" y2="16.65" />
				</svg>
				<span>{t('nav.explore')}</span>
			</a>

			<a
				bind:this={linkEls[2]}
				href="/notifications"
				class="nav-link"
				class:nav-link-active={isActive('/notifications')}
				data-active={isActive('/notifications')}
				aria-current={isActive('/notifications') ? 'page' : undefined}
			>
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="nav-icon" aria-hidden="true">
					<path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
					<path d="M13.73 21a2 2 0 0 1-3.46 0" />
				</svg>
				<span>{t('nav.notifications')}</span>
			</a>

			<a
				bind:this={linkEls[3]}
				href="/settings"
				class="nav-link"
				class:nav-link-active={isActive('/settings')}
				data-active={isActive('/settings')}
				aria-current={isActive('/settings') ? 'page' : undefined}
			>
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="nav-icon" aria-hidden="true">
					<circle cx="12" cy="12" r="3"/>
					<path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 2.83-2.83l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/>
				</svg>
				<span>{t('nav.settings')}</span>
			</a>
		{/if}
	</nav>

	<div class="sidebar-bottom">
		{#if authStore.isAuthenticated && authStore.user}
			<div class="profile-card glass glass-soft">
				<a href="/{authStore.user.username}" class="profile-card-link" aria-label="View your profile">
					<div class="profile-avatar" aria-hidden="true">
						{#if authStore.user.avatarUrl}
							<img src={authStore.user.avatarUrl} alt={authStore.user.username} class="avatar-img" />
						{:else}
							<span class="avatar-initial">
								{(authStore.user.name ?? authStore.user.username).charAt(0).toUpperCase()}
							</span>
						{/if}
					</div>
					<div class="profile-info">
						<span class="profile-name">{authStore.user.name ?? authStore.user.username}</span>
						<span class="profile-username">@{authStore.user.username}</span>
					</div>
				</a>
				<button class="logout-btn" onclick={handleLogout} aria-label={t('nav.signOut')} title={t('nav.signOut')}>
					<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="logout-icon" aria-hidden="true">
						<path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
						<polyline points="16 17 21 12 16 7" />
						<line x1="21" y1="12" x2="9" y2="12" />
					</svg>
				</button>
			</div>
		{:else}
			<div class="auth-cta glass glass-soft">
				<p class="auth-cta-text">{t('auth.login.subtitle')}</p>
				<a href="/login" class="btn btn-primary">{t('nav.signIn')}</a>
				<a href="/register" class="btn btn-secondary">{t('auth.register.submit')}</a>
			</div>
		{/if}
	</div>
</aside>

<style>
	.sidebar {
		display: flex;
		flex-direction: column;
		height: 100%;
		padding: 1.5rem 0;
	}

	.sidebar-logo {
		display: flex;
		align-items: center;
		gap: 0.5rem;
		padding: 0 0.75rem;
		margin-bottom: 2.25rem;
	}

	.logo-text {
		font-size: 1.125rem;
		font-weight: 800;
		color: var(--color-text-primary);
		letter-spacing: -0.045em;
	}

	.sidebar-nav {
		display: flex;
		flex-direction: column;
		gap: 0.25rem;
		flex: 1;
		position: relative;
	}

	/* Single shared glass pill that glides between nav links — same spring
	   feel as the mobile dock indicator, just vertical. */
	.nav-indicator {
		position: absolute;
		top: 0;
		left: 0;
		right: 0;
		border-radius: 0.75rem;
		opacity: 0;
		pointer-events: none;
		z-index: 0;
		will-change: transform, height;
		transition:
			transform 0.42s cubic-bezier(0.34, 1.4, 0.5, 1),
			height 0.42s cubic-bezier(0.34, 1.4, 0.5, 1),
			opacity 0.2s ease;
	}

	.nav-indicator.ready {
		opacity: 1;
	}

	.nav-link {
		display: flex;
		align-items: center;
		gap: 0.75rem;
		padding: 0.6875rem 0.875rem;
		border-radius: 0.75rem;
		color: var(--color-text-muted);
		text-decoration: none;
		font-size: 0.9375rem;
		font-weight: 500;
		transition: color 0.18s ease, transform 0.12s ease;
		position: relative;
		z-index: 1;
	}

	.nav-link:hover {
		color: var(--color-text-secondary);
	}

	.nav-link:active {
		transform: scale(0.97);
	}

	.nav-link-active {
		color: var(--color-text-primary);
		font-weight: 600;
	}

	.nav-icon {
		width: 1.1875rem;
		height: 1.1875rem;
		flex-shrink: 0;
		stroke-width: 1.75;
		transition: transform 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
	}

	.nav-link-active .nav-icon {
		transform: scale(1.06);
	}

	.sidebar-bottom {
		padding-top: 1rem;
	}

	.profile-card {
		display: flex;
		align-items: center;
		gap: 0.625rem;
		padding: 0.625rem;
		border-radius: 1rem;
		transition: transform 0.16s ease, box-shadow 0.2s ease;
	}

	.profile-card:hover {
		transform: translateY(-1px);
		box-shadow: var(--glass-shadow), inset 0 1px 0 var(--glass-highlight),
			0 0 0 1px var(--glass-highlight);
	}

	.profile-card-link {
		display: flex;
		align-items: center;
		gap: 0.625rem;
		flex: 1;
		min-width: 0;
		text-decoration: none;
	}

	.profile-avatar {
		width: 2.25rem;
		height: 2.25rem;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.06);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.avatar-initial {
		font-size: 0.8125rem;
		font-weight: 600;
		color: var(--color-text-secondary);
		user-select: none;
	}

	.profile-info {
		display: flex;
		flex-direction: column;
		gap: 0.125rem;
		flex: 1;
		min-width: 0;
	}

	.profile-name {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--color-text-primary);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.profile-username {
		font-size: 0.75rem;
		color: var(--color-text-muted);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.logout-btn {
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-muted);
		padding: 0.375rem;
		border-radius: 0.5rem;
		display: flex;
		align-items: center;
		transition: color 0.15s ease, background-color 0.15s ease, transform 0.12s ease;
		flex-shrink: 0;
	}

	.logout-btn:hover {
		color: var(--color-text-primary);
		background-color: rgba(255, 255, 255, 0.06);
	}

	.logout-btn:active {
		transform: scale(0.9);
	}

	.logout-icon {
		width: 1.125rem;
		height: 1.125rem;
	}

	.auth-cta {
		display: flex;
		flex-direction: column;
		gap: 0.625rem;
		padding: 1.25rem;
		border-radius: 1rem;
	}

	.auth-cta-text {
		font-size: 0.9375rem;
		font-weight: 600;
		color: var(--color-text-primary);
		margin: 0 0 0.25rem;
	}

	.btn {
		display: block;
		text-align: center;
		padding: 0.5625rem 1rem;
		border-radius: 0.625rem;
		font-size: 0.875rem;
		font-weight: 600;
		text-decoration: none;
		transition:
			background-color 0.15s ease,
			color 0.15s ease,
			transform 0.1s ease;
	}

	.btn:active {
		transform: scale(0.97);
	}

	.btn-primary {
		background-color: var(--color-text-primary);
		color: var(--color-bg);
	}

	.btn-primary:hover {
		background-color: var(--color-text-muted);
	}

	.btn-secondary {
		background-color: rgba(255, 255, 255, 0.04);
		color: var(--color-text-primary);
		border: 1px solid var(--glass-border);
	}

	.btn-secondary:hover {
		background-color: rgba(255, 255, 255, 0.08);
	}

	@media (prefers-reduced-motion: reduce) {
		.nav-indicator {
			transition: opacity 0.2s ease;
		}
	}

	/* Collapsed rail (≤900px): hide labels, center icons, indicator still glides */
	@media (max-width: 900px) {
		.sidebar-logo {
			padding: 0;
			justify-content: center;
		}

		.logo-text {
			display: none;
		}

		.nav-link span:not(.nav-icon),
		.profile-info {
			display: none;
		}

		.nav-link {
			justify-content: center;
			padding: 0.6875rem 0;
		}

		.profile-card {
			justify-content: center;
			padding: 0.5rem;
		}
	}
</style>
