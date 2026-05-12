<script lang="ts">
	import { getInitials } from '$lib/utils/getInitials';
	import type { User } from '$lib/types';

	interface Props {
		profile: User;
		isUploadingAvatar: boolean;
		avatarError: string | null;
		onSettings: () => void;
		onEdit: () => void;
		onAvatarClick: () => void;
	}

	let { profile, isUploadingAvatar, avatarError, onSettings, onEdit, onAvatarClick }: Props = $props();

	const initial = $derived(getInitials(profile.name, profile.username));
</script>

<section class="profile-header" aria-label="Profile info">
	<button class="settings-btn" onclick={onSettings} aria-label="Settings">
		<svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor"
			stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
			<circle cx="12" cy="12" r="3"/>
			<path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/>
		</svg>
	</button>

	<button class="avatar-wrap" onclick={onAvatarClick} aria-label="Change profile photo" disabled={isUploadingAvatar}>
		{#if profile.avatarUrl}
			<img src={profile.avatarUrl} alt={profile.username} class="avatar-img" />
		{:else}
			<div class="avatar-fallback">{initial}</div>
		{/if}
		{#if isUploadingAvatar}
			<div class="avatar-overlay" aria-hidden="true">
				<svg class="spinner" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
					<path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
				</svg>
			</div>
		{/if}
	</button>

	{#if avatarError}
		<p class="avatar-error" role="alert">{avatarError}</p>
	{/if}

	<div class="identity">
		<div class="name-row">
			{#if profile.name}
				<span class="display-name">{profile.name}</span>
			{:else}
				<span class="display-name muted">{profile.username}</span>
			{/if}
			<button class="edit-btn" onclick={onEdit} aria-label="Edit profile">
				<svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor"
					stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
					<path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7"/>
					<path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z"/>
				</svg>
			</button>
		</div>
		<span class="username">@{profile.username}</span>
		{#if profile.bio}
			<p class="bio">{profile.bio}</p>
		{/if}
	</div>
</section>

<style>
	.profile-header {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: 8px 20px 14px;
		gap: 8px;
		position: relative;
	}

	.settings-btn {
		position: absolute;
		top: 4px;
		right: 4px;
		background: none;
		border: none;
		cursor: pointer;
		color: rgba(240, 240, 240, 0.38);
		padding: 10px;
		min-width: 44px;
		min-height: 44px;
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 10px;
		-webkit-tap-highlight-color: transparent;
		transition: color 0.15s;
	}

	.settings-btn:active { color: rgba(240, 240, 240, 0.8); }

	/* ── Avatar ────────────────────────────────────────────────────────────── */

	.avatar-wrap {
		width: 86px;
		height: 86px;
		border-radius: 50%;
		border: none;
		padding: 0;
		background: none;
		cursor: pointer;
		position: relative;
		flex-shrink: 0;
		-webkit-tap-highlight-color: transparent;
	}

	.avatar-wrap:disabled { cursor: default; }

	.avatar-img {
		width: 86px;
		height: 86px;
		border-radius: 50%;
		object-fit: cover;
		display: block;
	}

	.avatar-fallback {
		width: 86px;
		height: 86px;
		border-radius: 50%;
		background: rgba(255, 255, 255, 0.07);
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 30px;
		font-weight: 600;
		color: rgba(240, 240, 240, 0.55);
		user-select: none;
	}

	.avatar-overlay {
		position: absolute;
		inset: 0;
		border-radius: 50%;
		background: rgba(0, 0, 0, 0.5);
		display: flex;
		align-items: center;
		justify-content: center;
		color: #fff;
	}

	.spinner {
		width: 20px;
		height: 20px;
		animation: spin 0.9s linear infinite;
	}

	@keyframes spin { to { transform: rotate(360deg); } }

	.avatar-error {
		font-size: 12px;
		color: #e05252;
		text-align: center;
		margin: 0;
	}

	/* ── Identity ──────────────────────────────────────────────────────────── */

	.identity {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 3px;
		text-align: center;
	}

	.name-row {
		display: flex;
		align-items: center;
		gap: 4px;
	}

	.display-name {
		font-size: 19px;
		font-weight: 700;
		color: var(--tg-theme-text-color, #f0f0f0);
		letter-spacing: -0.025em;
		line-height: 1.2;
	}

	.display-name.muted { color: rgba(240, 240, 240, 0.5); }

	.edit-btn {
		background: none;
		border: none;
		cursor: pointer;
		padding: 6px;
		color: rgba(240, 240, 240, 0.28);
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 6px;
		-webkit-tap-highlight-color: transparent;
		transition: color 0.15s;
		min-width: 30px;
		min-height: 30px;
	}

	.edit-btn:active { color: rgba(240, 240, 240, 0.75); }

	.username {
		font-size: 13px;
		color: rgba(240, 240, 240, 0.38);
	}

	.bio {
		font-size: 14px;
		color: rgba(240, 240, 240, 0.58);
		line-height: 1.45;
		margin: 3px 0 0;
		max-width: 260px;
		word-break: break-word;
	}
</style>
