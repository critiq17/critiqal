<script lang="ts">
	import type { AdminUser, AdminBadge, BadgeCode } from '$lib/types';
	import { adminService } from '$lib/services/admin.service';
	import { badgeMeta, tierStyle } from '$lib/badgeMeta';

	interface Props {
		user: AdminUser;
		badges: AdminBadge[];
		onclose: () => void;
		onchanged: (user: AdminUser) => void;
		onfeedback: (message: string, tone: 'success' | 'error') => void;
	}

	let { user, badges, onclose, onchanged, onfeedback }: Props = $props();

	// Spread to create an independent local copy; $effect below keeps it in sync when the prop changes.
	let current = $state<AdminUser>({ ...user });
	let pending = $state<BadgeCode | null>(null);
	let banReason = $state('');
	let banDays = $state<number | undefined>(undefined);
	let banPending = $state(false);

	$effect(() => {
		current = user;
	});

	const ownedCodes = $derived(new Set(current.badges.map((b) => b.code)));

	const initial = $derived((current.name ?? current.username).charAt(0).toUpperCase());

	async function refresh(): Promise<void> {
		const fresh = await adminService.getUser(current.id);
		current = fresh;
		onchanged(fresh);
	}

	async function ban(): Promise<void> {
		if (banPending || !banReason.trim()) return;
		banPending = true;
		try {
			await adminService.banUser(current.id, banReason.trim(), banDays);
			onfeedback(`Banned @${current.username}`, 'success');
			banReason = '';
			banDays = undefined;
			await refresh();
		} catch {
			onfeedback(`Could not ban @${current.username}`, 'error');
		} finally {
			banPending = false;
		}
	}

	async function unban(): Promise<void> {
		if (banPending) return;
		banPending = true;
		try {
			await adminService.unbanUser(current.id);
			onfeedback(`Unbanned @${current.username}`, 'success');
			await refresh();
		} catch {
			onfeedback(`Could not unban @${current.username}`, 'error');
		} finally {
			banPending = false;
		}
	}

	async function toggle(badge: AdminBadge): Promise<void> {
		if (pending) return;
		const owned = ownedCodes.has(badge.code);
		pending = badge.code;
		try {
			if (owned) {
				await adminService.revokeBadge(current.id, badge.code);
				onfeedback(`Revoked ${badge.name}`, 'success');
			} else {
				await adminService.grantBadge(current.id, badge.code);
				onfeedback(`Granted ${badge.name}`, 'success');
			}
			await refresh();
		} catch {
			onfeedback(
				owned ? `Could not revoke ${badge.name}` : `Could not grant ${badge.name}`,
				'error'
			);
		} finally {
			pending = null;
		}
	}
</script>

<aside class="panel" aria-label={`Manage badges for ${current.username}`}>
	<header class="panel-head">
		<div class="who">
			<div class="avatar" aria-hidden="true">
				{#if current.avatarUrl}
					<img src={current.avatarUrl} alt="" />
				{:else}
					<span class="initial">{initial}</span>
				{/if}
			</div>
			<div class="who-text">
				<span class="name">{current.name ?? current.username}</span>
				<span class="username">@{current.username}</span>
			</div>
		</div>
		<button class="close" onclick={onclose} aria-label="Close panel">✕</button>
	</header>

	<p class="section-label">Moderation</p>
	{#if current.banned}
		<div class="ban-status">
			<span class="ban-status-text">
				Banned{current.bannedUntil
					? ` until ${new Date(current.bannedUntil).toLocaleDateString()}`
					: ' permanently'}
			</span>
			<button class="ban-action unban" disabled={banPending} onclick={unban}>
				{banPending ? '…' : 'Unban'}
			</button>
		</div>
	{:else}
		<div class="ban-form">
			<textarea
				class="ban-reason"
				placeholder="Reason for ban…"
				rows={3}
				bind:value={banReason}
				disabled={banPending}
			></textarea>
			<div class="ban-row">
				<input
					class="ban-days"
					type="number"
					min={1}
					placeholder="Days (empty = permanent)"
					bind:value={banDays}
					disabled={banPending}
				/>
				<button class="ban-action ban" disabled={banPending || !banReason.trim()} onclick={ban}>
					{banPending ? '…' : 'Ban'}
				</button>
			</div>
		</div>
	{/if}

	<p class="section-label">Badges</p>
	<ul class="badge-grid">
		{#each badges as badge (badge.code)}
			{@const owned = ownedCodes.has(badge.code)}
			{@const meta = badgeMeta(badge.code)}
			{@const style = tierStyle(meta.tier)}
			<li>
				<button
					class="badge-option"
					class:owned
					style:--badge-accent={style.accent}
					disabled={pending !== null}
					aria-pressed={owned}
					onclick={() => toggle(badge)}
				>
					<span class="badge-dot" aria-hidden="true"></span>
					<span class="badge-text">
						<span class="badge-name">{badge.name}</span>
						<span class="badge-desc">{badge.description}</span>
					</span>
					<span class="badge-action">
						{#if pending === badge.code}
							…
						{:else if owned}
							Revoke
						{:else}
							Grant
						{/if}
					</span>
				</button>
			</li>
		{/each}
	</ul>
</aside>

<style>
	.panel {
		display: flex;
		flex-direction: column;
		gap: 1rem;
		height: 100%;
		padding: 1.25rem;
		background: var(--color-surface);
		border-left: 1px solid var(--color-border);
		overflow-y: auto;
	}
	.panel-head {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
	}
	.who {
		display: flex;
		align-items: center;
		gap: 0.625rem;
		min-width: 0;
	}
	.avatar {
		width: 40px;
		height: 40px;
		border-radius: var(--radius-full);
		background: var(--color-surface-raised);
		overflow: hidden;
		flex-shrink: 0;
		display: flex;
		align-items: center;
		justify-content: center;
	}
	.avatar img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}
	.initial {
		font-size: 1rem;
		font-weight: 700;
		color: var(--color-text-secondary);
	}
	.who-text {
		display: flex;
		flex-direction: column;
		min-width: 0;
	}
	.name {
		font-weight: 600;
		color: var(--color-text-primary);
	}
	.username {
		font-size: 0.8125rem;
		color: var(--color-text-secondary);
	}
	.close {
		background: transparent;
		border: none;
		color: var(--color-text-muted);
		cursor: pointer;
		font-size: 0.9375rem;
		padding: 0.25rem;
	}
	.close:hover {
		color: var(--color-text-primary);
	}
	.section-label {
		margin: 0;
		font-size: 0.75rem;
		text-transform: uppercase;
		letter-spacing: 0.05em;
		color: var(--color-text-muted);
	}
	.ban-status {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.75rem;
		padding: 0.625rem 0.75rem;
		background: color-mix(in srgb, #e53e3e 8%, var(--color-bg));
		border: 1px solid color-mix(in srgb, #e53e3e 35%, var(--color-border));
		border-radius: var(--radius-md);
	}
	.ban-status-text {
		font-size: 0.875rem;
		color: #e53e3e;
		font-weight: 500;
	}
	.ban-form {
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}
	.ban-reason {
		width: 100%;
		padding: 0.5rem 0.75rem;
		background: var(--color-bg);
		border: 1px solid var(--color-border);
		border-radius: var(--radius-md);
		color: var(--color-text-primary);
		font-family: inherit;
		font-size: 0.875rem;
		resize: vertical;
		box-sizing: border-box;
	}
	.ban-reason:focus {
		outline: none;
		border-color: #e53e3e;
	}
	.ban-reason:disabled {
		opacity: 0.6;
	}
	.ban-row {
		display: flex;
		gap: 0.5rem;
	}
	.ban-days {
		flex: 1;
		padding: 0.5rem 0.75rem;
		background: var(--color-bg);
		border: 1px solid var(--color-border);
		border-radius: var(--radius-md);
		color: var(--color-text-primary);
		font-family: inherit;
		font-size: 0.875rem;
		min-width: 0;
	}
	.ban-days:focus {
		outline: none;
		border-color: #e53e3e;
	}
	.ban-days:disabled {
		opacity: 0.6;
	}
	.ban-action {
		padding: 0.5rem 1rem;
		border: none;
		border-radius: var(--radius-md);
		font-family: inherit;
		font-size: 0.875rem;
		font-weight: 600;
		cursor: pointer;
		white-space: nowrap;
		transition: opacity 0.15s ease;
	}
	.ban-action:disabled {
		opacity: 0.45;
		cursor: not-allowed;
	}
	.ban-action.ban {
		background: #e53e3e;
		color: #fff;
	}
	.ban-action.ban:hover:not(:disabled) {
		opacity: 0.85;
	}
	.ban-action.unban {
		background: var(--color-bg);
		border: 1px solid #e53e3e;
		color: #e53e3e;
	}
	.ban-action.unban:hover:not(:disabled) {
		background: color-mix(in srgb, #e53e3e 10%, var(--color-bg));
	}
	.badge-grid {
		list-style: none;
		margin: 0;
		padding: 0;
		display: flex;
		flex-direction: column;
		gap: 0.5rem;
	}
	.badge-option {
		--badge-accent: var(--color-accent);
		display: flex;
		align-items: center;
		gap: 0.75rem;
		width: 100%;
		padding: 0.625rem 0.75rem;
		background: var(--color-bg);
		border: 1px solid var(--color-border);
		border-radius: var(--radius-md);
		cursor: pointer;
		text-align: left;
		font-family: inherit;
		transition: border-color 0.15s ease, background 0.15s ease;
	}
	.badge-option:hover:not(:disabled) {
		border-color: color-mix(in srgb, var(--badge-accent) 50%, var(--color-border));
	}
	.badge-option:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}
	.badge-option.owned {
		border-color: color-mix(in srgb, var(--badge-accent) 45%, var(--color-border));
		background: color-mix(in srgb, var(--badge-accent) 8%, var(--color-bg));
	}
	.badge-dot {
		width: 10px;
		height: 10px;
		border-radius: 50%;
		background: var(--badge-accent);
		flex-shrink: 0;
	}
	.badge-text {
		display: flex;
		flex-direction: column;
		min-width: 0;
		flex: 1;
	}
	.badge-name {
		font-weight: 600;
		color: var(--color-text-primary);
		font-size: 0.875rem;
	}
	.badge-desc {
		font-size: 0.75rem;
		color: var(--color-text-secondary);
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
	.badge-action {
		flex-shrink: 0;
		font-size: 0.8125rem;
		font-weight: 600;
		color: var(--badge-accent);
	}
</style>
