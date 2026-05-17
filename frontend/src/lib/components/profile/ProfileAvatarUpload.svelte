<script lang="ts">
	import type { User } from '$lib/types';

	interface Props {
		profile: User;
		isUploading: boolean;
		error: string | null;
		onUpload: (file: File) => void;
	}

	let { profile, isUploading, error, onUpload }: Props = $props();

	function getAvatarInitial(user: User): string {
		return (user.name ?? user.username).charAt(0).toUpperCase();
	}

	function handleChange(e: Event): void {
		const input = e.target as HTMLInputElement;
		const file = input.files?.[0];
		if (file) {
			onUpload(file);
			input.value = '';
		}
	}
</script>

<label class="avatar-upload-label" aria-label="Upload avatar" title="Change profile photo">
	<input
		type="file"
		accept="image/jpeg,image/png,image/webp"
		class="avatar-file-input"
		onchange={handleChange}
		disabled={isUploading}
	/>
	<div class="avatar-lg" aria-hidden="true">
		{#if profile.avatarUrl}
			<img src={profile.avatarUrl} alt={profile.username} class="avatar-img" />
		{:else}
			<span class="avatar-initial">{getAvatarInitial(profile)}</span>
		{/if}
		{#if isUploading}
			<div class="avatar-spinner-overlay" aria-hidden="true">
				<svg class="avatar-spinner" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
					<path d="M12 2v4M12 18v4M4.93 4.93l2.83 2.83M16.24 16.24l2.83 2.83M2 12h4M18 12h4M4.93 19.07l2.83-2.83M16.24 7.76l2.83-2.83" />
				</svg>
			</div>
		{:else}
			<div class="avatar-camera-overlay" aria-hidden="true">
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
					<path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
					<circle cx="12" cy="13" r="4" />
				</svg>
			</div>
		{/if}
	</div>
</label>
{#if error}
	<p class="avatar-error" role="alert">{error}</p>
{/if}

<style>
	.avatar-upload-label {
		position: relative;
		cursor: pointer;
		display: block;
		flex-shrink: 0;
	}

	.avatar-file-input {
		display: none;
	}

	.avatar-lg {
		width: 5rem;
		height: 5rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		overflow: hidden;
		position: relative;
	}

	.avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.avatar-initial {
		font-size: 1.75rem;
		font-weight: 600;
		color: var(--color-text-muted);
		user-select: none;
	}

	.avatar-camera-overlay {
		position: absolute;
		inset: 0;
		border-radius: 50%;
		background: rgba(0, 0, 0, 0.45);
		display: flex;
		align-items: center;
		justify-content: center;
		color: #fff;
		opacity: 0;
		transition: opacity 0.18s ease;
	}

	.avatar-upload-label:hover .avatar-camera-overlay {
		opacity: 1;
	}

	.avatar-spinner-overlay {
		position: absolute;
		inset: 0;
		border-radius: 50%;
		background: rgba(0, 0, 0, 0.5);
		display: flex;
		align-items: center;
		justify-content: center;
		color: #fff;
	}

	.avatar-spinner {
		width: 1.25rem;
		height: 1.25rem;
		animation: spin 0.9s linear infinite;
	}

	.avatar-error {
		font-size: 0.75rem;
		color: var(--color-accent);
		margin-top: 0.375rem;
		max-width: 5rem;
		text-align: center;
		line-height: 1.3;
	}

	@keyframes spin {
		from { transform: rotate(0deg); }
		to { transform: rotate(360deg); }
	}
</style>
