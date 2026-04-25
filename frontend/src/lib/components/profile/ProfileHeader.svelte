<script lang="ts">
	import { getInitials } from '$lib/utils/getInitials';
	import type { User } from '$lib/types';

	interface Props {
		profile: User;
		isEditing: boolean;
		isSaving: boolean;
		editName: string;
		editBio: string;
		saveError: string | null;
		isUploadingAvatar: boolean;
		avatarError: string | null;
		onSettings: () => void;
		onEdit: () => void;
		onCancelEdit: () => void;
		onSaveEdit: () => void;
		onAvatarClick: () => void;
		onEditNameChange: (v: string) => void;
		onEditBioChange: (v: string) => void;
	}

	let {
		profile,
		isEditing,
		isSaving,
		editName,
		editBio,
		saveError,
		isUploadingAvatar,
		avatarError,
		onSettings,
		onEdit,
		onCancelEdit,
		onSaveEdit,
		onAvatarClick,
		onEditNameChange,
		onEditBioChange
	}: Props = $props();

	const initial = $derived(getInitials(profile.name, profile.username));
</script>

<section class="profile-header" aria-label="Profile info">
	<button class="settings-btn-standalone" onclick={onSettings} aria-label="Settings">
		<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
			<circle cx="12" cy="12" r="3"/>
			<path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06A1.65 1.65 0 004.68 15a1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06A1.65 1.65 0 009 4.68a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06A1.65 1.65 0 0019.4 9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z"/>
		</svg>
	</button>

	<div
		class="avatar-wrap"
		role="button"
		tabindex="0"
		onclick={onAvatarClick}
		onkeydown={(e) => { if (e.key === 'Enter' || e.key === ' ') onAvatarClick(); }}
		aria-label="Change profile photo"
	>
		{#if profile.avatarUrl}
			<img src={profile.avatarUrl} alt={profile.username} class="avatar" />
		{:else}
			<div class="avatar avatar-fallback">{initial}</div>
		{/if}
		{#if isUploadingAvatar}
			<div class="avatar-overlay" aria-hidden="true">
				<svg class="spinner" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
					<path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83"/>
				</svg>
			</div>
		{/if}
	</div>

	{#if avatarError}
		<p class="avatar-error" role="alert">{avatarError}</p>
	{/if}

	{#if isEditing}
		<form
			class="edit-form"
			aria-label="Edit profile"
			onsubmit={(e) => { e.preventDefault(); onSaveEdit(); }}
		>
			<input
				type="text"
				class="edit-input"
				value={editName}
				oninput={(e) => onEditNameChange((e.target as HTMLInputElement).value)}
				placeholder="Your name"
				maxlength={80}
				disabled={isSaving}
				aria-label="Display name"
			/>
			<textarea
				class="edit-textarea"
				value={editBio}
				oninput={(e) => onEditBioChange((e.target as HTMLTextAreaElement).value)}
				placeholder="Write something about yourself..."
				maxlength={280}
				rows={3}
				disabled={isSaving}
				aria-label="Bio"
			></textarea>
			{#if saveError}
				<p class="save-error" role="alert">{saveError}</p>
			{/if}
			<div class="edit-actions">
				<button type="submit" class="edit-save-btn" disabled={isSaving}>
					{isSaving ? 'Saving...' : 'Save'}
				</button>
				<button type="button" class="edit-cancel-btn" onclick={onCancelEdit} disabled={isSaving}>
					Cancel
				</button>
			</div>
		</form>
	{:else}
		<div class="identity">
			<div class="name-row">
				{#if profile.name}
					<span class="display-name">{profile.name}</span>
				{/if}
				<button class="edit-icon-btn" onclick={onEdit} aria-label="Edit profile">
					<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
						<path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
						<path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
					</svg>
				</button>
			</div>
			<span class="username">@{profile.username}</span>
			{#if profile.bio}
				<p class="bio">{profile.bio}</p>
			{/if}
		</div>
	{/if}
</section>

<style>
	.profile-header {
		display: flex;
		flex-direction: column;
		align-items: center;
		padding: calc(var(--tg-safe-area-inset-top, 0px) + 4px) 24px 10px;
		gap: 6px;
		position: relative;
	}

	.settings-btn-standalone {
		position: absolute;
		top: calc(var(--tg-safe-area-inset-top, 0px) + 4px);
		right: 8px;
		background: none;
		border: none;
		cursor: pointer;
		color: var(--color-text-secondary, rgba(240, 240, 240, 0.6));
		padding: 8px;
		min-width: 44px;
		min-height: 44px;
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.avatar-wrap {
		cursor: pointer;
		border-radius: 50%;
		overflow: hidden;
		width: 80px;
		height: 80px;
		flex-shrink: 0;
		position: relative;
	}

	.avatar {
		width: 80px;
		height: 80px;
		border-radius: 50%;
		object-fit: cover;
		display: block;
	}

	.avatar-fallback {
		background: var(--color-surface-raised, #242424);
		color: rgba(240, 240, 240, 0.7);
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 28px;
		font-weight: 600;
		user-select: none;
	}

	.avatar-overlay {
		position: absolute;
		inset: 0;
		border-radius: 50%;
		background: rgba(0, 0, 0, 0.45);
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

	@keyframes spin {
		from { transform: rotate(0deg); }
		to { transform: rotate(360deg); }
	}

	.avatar-error {
		font-size: 12px;
		color: #e05252;
		text-align: center;
		margin: 0;
	}

	.identity {
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: 2px;
		text-align: center;
	}

	.name-row {
		display: flex;
		align-items: center;
		gap: 6px;
	}

	.edit-icon-btn {
		background: none;
		border: none;
		cursor: pointer;
		padding: 0;
		min-width: 44px;
		min-height: 44px;
		color: rgba(240, 240, 240, 0.4);
		display: flex;
		align-items: center;
		justify-content: center;
		border-radius: 6px;
		transition: color 0.15s ease;
		-webkit-tap-highlight-color: transparent;
	}

	.edit-icon-btn:active {
		color: rgba(240, 240, 240, 0.9);
	}

	.display-name {
		font-size: 18px;
		font-weight: 700;
		color: var(--tg-theme-text-color, var(--color-text-primary, #f0f0f0));
		line-height: 1.2;
		margin: 0;
	}

	.username {
		font-size: 13px;
		color: var(--tg-theme-hint-color, rgba(240, 240, 240, 0.45));
		margin: 0;
	}

	.bio {
		font-size: 14px;
		color: var(--tg-theme-hint-color, rgba(240, 240, 240, 0.65));
		line-height: 1.4;
		margin: 0;
		max-width: 260px;
		text-align: center;
		word-break: break-word;
	}

	.edit-form {
		width: 100%;
		display: flex;
		flex-direction: column;
		gap: 8px;
	}

	.edit-input,
	.edit-textarea {
		width: 100%;
		box-sizing: border-box;
		background: var(--color-surface-raised, #242424);
		border: 1px solid var(--color-border, rgba(255, 255, 255, 0.15));
		border-radius: 10px;
		padding: 10px 12px;
		font-size: 14px;
		color: var(--color-text-primary, #f0f0f0);
		font-family: inherit;
		outline: none;
		resize: none;
	}

	.edit-input::placeholder,
	.edit-textarea::placeholder {
		color: rgba(255, 255, 255, 0.3);
	}

	.edit-input:focus,
	.edit-textarea:focus {
		border-color: rgba(255, 255, 255, 0.35);
	}

	.edit-input:disabled,
	.edit-textarea:disabled {
		opacity: 0.6;
		cursor: not-allowed;
	}

	.save-error {
		font-size: 13px;
		color: #e05252;
		margin: 0;
		text-align: center;
	}

	.edit-actions {
		display: flex;
		gap: 8px;
	}

	.edit-save-btn {
		flex: 1;
		height: 36px;
		border-radius: 10px;
		background: var(--color-text-primary, #f0f0f0);
		border: none;
		color: var(--color-bg, #0f0f0f);
		font-size: 14px;
		font-weight: 600;
		cursor: pointer;
		font-family: inherit;
	}

	.edit-save-btn:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}

	.edit-cancel-btn {
		flex: 1;
		height: 36px;
		border-radius: 10px;
		background: none;
		border: 1px solid var(--color-border, rgba(255, 255, 255, 0.15));
		color: var(--color-text-primary, #f0f0f0);
		font-size: 14px;
		font-weight: 500;
		cursor: pointer;
		font-family: inherit;
	}

	.edit-cancel-btn:disabled {
		opacity: 0.5;
		cursor: not-allowed;
	}
</style>
