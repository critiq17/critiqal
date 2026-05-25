<script lang="ts">
	import { authStore } from '$lib/stores/auth.store.svelte';
	import type { UseComposer } from '$lib/features/posts/useComposer.svelte';
	import { t } from '$lib/i18n';

	interface Props {
		composer: UseComposer;
		onSubmit: () => void;
	}

	let { composer, onSubmit }: Props = $props();

	function handleKeydown(e: KeyboardEvent): void {
		if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) onSubmit();
	}

	function handlePhotoSelect(e: Event): void {
		const input = e.target as HTMLInputElement;
		const files = input.files;
		if (!files || files.length === 0) return;
		composer.addFiles(Array.from(files));
		input.value = '';
	}
</script>

<div class="compose-box">
	<div class="compose-avatar" aria-hidden="true">
		{#if authStore.user?.avatarUrl}
			<img src={authStore.user.avatarUrl} alt={authStore.user.username} class="compose-avatar-img" loading="lazy" decoding="async" />
		{:else}
			<span class="compose-avatar-initial">
				{(authStore.user?.name ?? authStore.user?.username ?? '?').charAt(0).toUpperCase()}
			</span>
		{/if}
	</div>
	<div class="compose-right">
		<textarea
			class="compose-input"
			value={composer.text}
			oninput={(e) => { composer.text = (e.target as HTMLTextAreaElement).value; }}
			onkeydown={handleKeydown}
			placeholder={t('feed.composePlaceholder')}
			rows={3}
			disabled={composer.loading}
			aria-label={t('post.composeTitle')}
		></textarea>

		{#if composer.previewUrls.length > 0}
			<div class="photo-thumbnail-row">
				{#each composer.previewUrls as url, i (url)}
					<div class="photo-thumbnail-wrap">
						<img src={url} alt="Selected photo {i + 1}" class="photo-thumbnail-img" />
						<button
							class="photo-remove-btn"
							onclick={() => composer.removePhoto(i)}
							aria-label="Remove photo {i + 1}"
							type="button"
						>×</button>
					</div>
				{/each}
			</div>
		{/if}

		<div class="compose-actions">
			<div class="compose-actions-left">
				{#if composer.selectedFiles.length < composer.maxPhotos}
					<label class="compose-photo-label" aria-label={t('common.add')} title={`${t('common.add')} (${composer.maxPhotos})`}>
						<input
							type="file"
							accept="image/jpeg,image/png,image/webp"
							class="compose-photo-input"
							onchange={handlePhotoSelect}
							disabled={composer.loading}
							multiple
						/>
						<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="18" height="18" aria-hidden="true">
							<path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z" />
							<circle cx="12" cy="13" r="4" />
						</svg>
					</label>
				{/if}
				<span class="compose-hint">
					{#if composer.selectedFiles.length > 0}
						{composer.selectedFiles.length}/{composer.maxPhotos} · Ctrl+Enter
					{:else}
						Ctrl+Enter
					{/if}
				</span>
			</div>
			<button
				class="compose-submit"
				onclick={onSubmit}
				disabled={!composer.canPost}
			>
				{#if composer.loading}
					{t('post.composePosting')}
				{:else}
					{t('post.composePost')}
				{/if}
			</button>
		</div>

		{#if composer.errorMessage}
			<p class="compose-error" role="alert">{composer.errorMessage}</p>
		{/if}
	</div>
</div>

<style>
	.compose-box {
		display: flex;
		gap: 0.75rem;
		padding: 1.125rem 1.25rem;
		margin: 0.5rem 0 1rem;
		border-radius: 1rem;
		background: var(--glass-bg-soft);
		backdrop-filter: blur(calc(var(--glass-blur) + 4px)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(calc(var(--glass-blur) + 4px)) saturate(var(--glass-saturate));
		border: 1px solid var(--glass-border);
		box-shadow: inset 0 1px 0 var(--glass-highlight);
		transition: box-shadow 0.2s ease;
	}

	.compose-box:focus-within {
		box-shadow: inset 0 1px 0 var(--glass-highlight),
			0 0 0 1px var(--glass-highlight);
	}

	@supports not ((backdrop-filter: blur(1px)) or (-webkit-backdrop-filter: blur(1px))) {
		.compose-box {
			background: var(--color-surface);
		}
	}

	.compose-avatar {
		width: 2.5rem;
		height: 2.5rem;
		border-radius: 50%;
		background: var(--color-surface-raised);
		display: flex;
		align-items: center;
		justify-content: center;
		flex-shrink: 0;
		overflow: hidden;
	}

	.compose-avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.compose-avatar-initial {
		font-size: 0.875rem;
		font-weight: 600;
		color: var(--color-text-muted);
		user-select: none;
	}

	.compose-right {
		flex: 1;
		display: flex;
		flex-direction: column;
		gap: 0.625rem;
	}

	.compose-input {
		width: 100%;
		background: none;
		border: none;
		outline: none;
		font-size: 1rem;
		color: var(--color-text-primary);
		font-family: inherit;
		resize: none;
		line-height: 1.5;
		padding: 0;
	}

	.compose-input::placeholder {
		color: var(--color-text-muted);
		opacity: 0.5;
	}

	.compose-input:disabled {
		opacity: 0.6;
	}

	.compose-actions {
		display: flex;
		align-items: center;
		justify-content: space-between;
	}

	.compose-hint {
		font-size: 0.6875rem;
		color: var(--color-text-muted);
		opacity: 0.45;
	}

	.compose-actions-left {
		display: flex;
		align-items: center;
		gap: 0.625rem;
	}

	.compose-photo-label {
		display: flex;
		align-items: center;
		justify-content: center;
		cursor: pointer;
		color: var(--color-text-muted);
		padding: 0.25rem;
		border-radius: 0.375rem;
		transition: color 0.15s ease, background-color 0.15s ease;
	}

	.compose-photo-label:hover {
		color: var(--color-text-primary);
		background-color: var(--color-surface-raised);
	}

	.compose-photo-input {
		display: none;
	}

	.photo-thumbnail-row {
		display: flex;
		gap: 0.5rem;
		flex-wrap: wrap;
		margin-bottom: 0.5rem;
	}

	.photo-thumbnail-wrap {
		position: relative;
		width: 72px;
		height: 72px;
		flex-shrink: 0;
		border-radius: 0.5rem;
		overflow: hidden;
	}

	.photo-thumbnail-img {
		display: block;
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.photo-remove-btn {
		position: absolute;
		top: 3px;
		right: 3px;
		background: rgba(0, 0, 0, 0.7);
		color: #fff;
		border: none;
		border-radius: 50%;
		width: 16px;
		height: 16px;
		font-size: 0.6875rem;
		line-height: 1;
		cursor: pointer;
		display: flex;
		align-items: center;
		justify-content: center;
		transition: background-color 0.15s ease;
		padding: 0;
	}

	.photo-remove-btn:hover {
		background: rgba(0, 0, 0, 0.9);
	}

	.compose-submit {
		background: var(--color-text-primary);
		color: var(--color-bg);
		border: none;
		border-radius: 9999px;
		padding: 0.4375rem 1.125rem;
		font-size: 0.875rem;
		font-weight: 600;
		font-family: inherit;
		cursor: pointer;
		transition: opacity 0.15s ease, transform 0.1s ease;
	}

	.compose-submit:disabled {
		opacity: 0.35;
		cursor: not-allowed;
	}

	.compose-submit:not(:disabled):hover {
		opacity: 0.85;
	}

	.compose-submit:not(:disabled):active {
		transform: scale(0.96);
	}

	.compose-error {
		margin: 0;
		font-size: 0.75rem;
		color: #e05252;
	}
</style>
