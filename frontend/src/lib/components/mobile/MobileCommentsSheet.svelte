<script lang="ts">
	import { fade, fly, slide } from 'svelte/transition';
	import { cubicOut } from 'svelte/easing';
	import { apiClient } from '$lib/api/client';
	import { API } from '$lib/api/endpoints';
	import { authStore } from '$lib/stores/auth.store.svelte';
	import { closeMobileComments, mobileComments } from '$lib/stores/mobile-comments.store';
	import type { Comment, User } from '$lib/types';
	import { getInitials } from '$lib/utils/getInitials';
	import { formatRelativeTime } from '$lib/utils/formatRelativeTime';
	import { t } from '$lib/i18n';
	import { registerSheet } from '$lib/actions/registerSheet';
	import { elasticDrag } from '$lib/actions/elasticDrag';
	import { pushBackHandler } from '$lib/tma/back-button';
	import { hapticLight } from '$lib/tma/buttons';

	interface ReplyUiState {
		items: Comment[];
		loaded: boolean;
		loading: boolean;
		expanded: boolean;
		error: string | null;
	}

	interface ReplyTarget {
		commentId: string;
		username: string;
		displayName: string;
	}

	const UNKNOWN_AUTHOR: User = {
		id: '',
		username: 'unknown',
		name: null,
		bio: null,
		avatarUrl: null,
		email: null,
		emailVerified: false,
		pendingEmail: null,
		twoFactorEnabled: false,
		createdAt: ''
	};

	let composerInset = $state(baseInset());
	let items = $state<Comment[]>([]);
	let loading = $state(false);
	let loadError = $state<string | null>(null);
	let draft = $state('');
	let submitting = $state(false);
	let submitError = $state<string | null>(null);
	let deletingId = $state<string | null>(null);
	let replyStates = $state<Record<string, ReplyUiState>>({});
	let replyTarget = $state<ReplyTarget | null>(null);
	let composerInputEl = $state<HTMLInputElement | null>(null);
	let panelEl = $state<HTMLElement | null>(null);
	let backdropEl = $state<HTMLElement | null>(null);
	let loadToken = 0;
	const replyLoadTokens = new Map<string, number>();

	let closing = false;

	// Single animated exit for every close path (backdrop tap, ✕, back button,
	// swipe-dismiss): slide the panel down and fade the backdrop, then unmount.
	// Done manually (not a Svelte out-transition) so it continues smoothly from
	// wherever a drag left the panel instead of snapping back first.
	function animateClose(): void {
		if (closing) return;
		closing = true;
		if (panelEl) {
			panelEl.style.transition = 'transform 0.26s cubic-bezier(0.4, 0, 0.2, 1)';
			panelEl.style.transform = 'translateY(110%)';
		}
		if (backdropEl) {
			backdropEl.style.transition = 'opacity 0.24s ease';
			backdropEl.style.opacity = '0';
		}
		window.setTimeout(() => {
			closing = false;
			closeMobileComments();
		}, 250);
	}

	function requestClose(event?: Event): void {
		event?.preventDefault();
		animateClose();
	}

	// Swipe-down on the header drags the whole panel; the backdrop dims in step
	// (1 → 0) so the dismiss reads as one continuous, iOS-style gesture.
	function fadeBackdrop(progress: number): void {
		if (backdropEl) backdropEl.style.opacity = String(1 - progress);
	}

	function baseInset(): string {
		return 'calc(var(--tg-content-bottom, var(--tg-content-safe-area-inset-bottom, env(safe-area-inset-bottom, 0px))) + 8px)';
	}

	function emptyReplyState(): ReplyUiState {
		return {
			items: [],
			loaded: false,
			loading: false,
			expanded: false,
			error: null
		};
	}

	function getReplyState(commentId: string): ReplyUiState {
		return replyStates[commentId] ?? emptyReplyState();
	}

	function setReplyState(commentId: string, patch: Partial<ReplyUiState>): void {
		const current = getReplyState(commentId);
		replyStates = {
			...replyStates,
			[commentId]: {
				...current,
				...patch
			}
		};
	}

	function removeReplyState(commentId: string): void {
		if (!(commentId in replyStates)) return;
		const next = { ...replyStates };
		delete next[commentId];
		replyStates = next;
		replyLoadTokens.delete(commentId);
	}

	function pruneReplyStates(nextComments: Comment[]): void {
		const ids = new Set(nextComments.map((comment) => comment.id));
		const next: Record<string, ReplyUiState> = {};

		for (const [commentId, state] of Object.entries(replyStates)) {
			if (ids.has(commentId)) {
				next[commentId] = state;
				continue;
			}

			replyLoadTokens.delete(commentId);
		}

		replyStates = next;

		if (replyTarget && !ids.has(replyTarget.commentId)) {
			replyTarget = null;
		}
	}

	function resetPanelState(): void {
		items = [];
		loading = false;
		loadError = null;
		draft = '';
		submitting = false;
		submitError = null;
		deletingId = null;
		replyStates = {};
		replyTarget = null;
		replyLoadTokens.clear();
	}

	function normalizeComments(data: unknown): Comment[] {
		return Array.isArray(data) ? (data as Comment[]) : [];
	}

	function getErrorMessage(error: unknown, fallback: string): string {
		return error instanceof Error && error.message ? error.message : fallback;
	}

	function applyComposerInset(): void {
		if (typeof window === 'undefined' || !window.visualViewport) {
			composerInset = baseInset();
			return;
		}

		const keyboardHeight = Math.max(
			0,
			window.innerHeight - window.visualViewport.height - window.visualViewport.offsetTop
		);

		composerInset = keyboardHeight > 0 ? `${keyboardHeight + 10}px` : baseInset();
	}

	// Mirror the backend post counter onto the live post object so the feed
	// card's badge updates instantly without a refetch (replies count too).
	function bumpPostCount(delta: number): void {
		const post = $mobileComments.post;
		if (!post) return;
		post.commentCount = Math.max(0, post.commentCount + delta);
	}

	function authorFor(comment: Comment): User {
		return comment.author ?? UNKNOWN_AUTHOR;
	}

	function commentTime(comment: Comment): string {
		return comment.createdAt ? formatRelativeTime(comment.createdAt) : t('post.justNow');
	}

	function composerPlaceholder(): string {
		return replyTarget ? `${t('comment.replyTo')} ${replyTarget.displayName}…` : t('comment.placeholder');
	}

	function composerAriaLabel(): string {
		return replyTarget ? t('comment.writeReply') : t('comment.writeComment');
	}

	function submitAriaLabel(): string {
		return replyTarget ? t('comment.postReply') : t('comment.postComment');
	}

	function replyToggleLabel(state: ReplyUiState): string {
		if (state.loading) return t('comment.repliesLoading');
		if (state.error) return t('comment.repliesRetry');
		if (!state.loaded) return t('comment.viewReplies');
		if (state.expanded) return state.items.length > 0 ? t('comment.hideReplies') : t('comment.hideThread');
		if (state.items.length >= 1) return `${t('comment.viewReplies')} (${state.items.length})`;
		return t('comment.viewReplies');
	}

	function focusComposer(): void {
		if (typeof window === 'undefined') return;

		window.requestAnimationFrame(() => {
			composerInputEl?.focus();
			const valueLength = composerInputEl?.value.length ?? 0;
			composerInputEl?.setSelectionRange(valueLength, valueLength);
		});
	}

	async function loadComments(postId: string, options: { keepItems?: boolean } = {}): Promise<void> {
		const { keepItems = false } = options;
		const token = ++loadToken;
		const keepVisibleItems = keepItems && items.length > 0;

		loading = !keepVisibleItems;
		if (!keepVisibleItems) {
			loadError = null;
		}
		if (!keepItems) {
			items = [];
		}

		try {
			const data = await apiClient.get<Comment[]>(API.posts.comments(postId));

			if (token !== loadToken || !$mobileComments.open || $mobileComments.postId !== postId) {
				return;
			}

			const nextItems = normalizeComments(data);
			items = nextItems;
			pruneReplyStates(nextItems);
		} catch (error) {
			if (token !== loadToken || !$mobileComments.open || $mobileComments.postId !== postId) {
				return;
			}

			if (items.length === 0) {
				loadError = getErrorMessage(error, t('comment.failed'));
			}
		} finally {
			if (token === loadToken && $mobileComments.open && $mobileComments.postId === postId) {
				loading = false;
			}
		}
	}

	async function loadReplies(
		commentId: string,
		options: { expand?: boolean; force?: boolean; keepItems?: boolean } = {}
	): Promise<void> {
		const postId = $mobileComments.postId;
		if (!$mobileComments.open || postId === null) return;

		const { expand = true, force = false, keepItems = false } = options;
		const current = getReplyState(commentId);
		if (current.loading) return;
		if (current.loaded && !force) {
			if (expand !== current.expanded) {
				setReplyState(commentId, { expanded: expand });
			}
			return;
		}

		const token = (replyLoadTokens.get(commentId) ?? 0) + 1;
		replyLoadTokens.set(commentId, token);

		setReplyState(commentId, {
			loading: true,
			expanded: expand,
			error: null,
			...(keepItems ? {} : { items: current.items })
		});

		try {
			const data = await apiClient.get<Comment[]>(API.posts.replies(postId, commentId));

			if (
				replyLoadTokens.get(commentId) !== token ||
				!$mobileComments.open ||
				$mobileComments.postId !== postId
			) {
				return;
			}

			setReplyState(commentId, {
				items: normalizeComments(data),
				loaded: true,
				loading: false,
				expanded: expand,
				error: null
			});
		} catch (error) {
			if (
				replyLoadTokens.get(commentId) !== token ||
				!$mobileComments.open ||
				$mobileComments.postId !== postId
			) {
				return;
			}

			setReplyState(commentId, {
				loading: false,
				expanded: expand,
				error: getErrorMessage(error, t('comment.repliesFailed'))
			});
		}
	}

	async function reloadComments(event?: Event): Promise<void> {
		event?.preventDefault();
		const postId = $mobileComments.postId;
		if (!$mobileComments.open || postId === null) return;
		await loadComments(postId);
	}

	function startReply(comment: Comment, event?: Event): void {
		event?.preventDefault();
		const author = authorFor(comment);

		replyTarget = {
			commentId: comment.id,
			username: author.username,
			displayName: author.name ?? author.username
		};
		submitError = null;
		focusComposer();
	}

	function clearReplyTarget(event?: Event): void {
		event?.preventDefault();
		replyTarget = null;
		submitError = null;
		focusComposer();
	}

	function toggleReplies(commentId: string, event?: Event): void {
		event?.preventDefault();
		const state = getReplyState(commentId);
		if (state.loading) return;

		if (!state.loaded) {
			void loadReplies(commentId, { expand: true });
			return;
		}

		setReplyState(commentId, { expanded: !state.expanded, error: null });
	}

	function handleComposerKeydown(event: KeyboardEvent): void {
		if (event.key !== 'Enter') return;
		event.preventDefault();
		void handleComposerSubmit();
	}

	async function handleComposerSubmit(event?: Event): Promise<void> {
		event?.preventDefault();

		const postId = $mobileComments.postId;
		const content = draft.trim();
		const activeReplyTarget = replyTarget;

		if (!$mobileComments.open || postId === null || !content || submitting) {
			return;
		}

		submitting = true;
		submitError = null;

		try {
			if (activeReplyTarget) {
				const created = await apiClient.post<Comment>(
					API.posts.replies(postId, activeReplyTarget.commentId),
					{ content }
				);

				if (!$mobileComments.open || $mobileComments.postId !== postId) {
					return;
				}

				const current = getReplyState(activeReplyTarget.commentId);
				const nextReplies = created
					? [...current.items.filter((reply) => reply.id !== created.id), created]
					: current.items;

				setReplyState(activeReplyTarget.commentId, {
					items: nextReplies,
					loaded: true,
					expanded: true,
					loading: false,
					error: null
				});

				if (created) bumpPostCount(1);
				draft = '';
				replyTarget = null;
				void loadReplies(activeReplyTarget.commentId, {
					expand: true,
					force: true,
					keepItems: true
				});
				return;
			}

			const created = await apiClient.post<Comment>(API.posts.comments(postId), { content });

			if (!$mobileComments.open || $mobileComments.postId !== postId) {
				return;
			}

			if (created) {
				items = [created, ...items.filter((comment) => comment.id !== created.id)];
				bumpPostCount(1);
			}

			draft = '';
			void loadComments(postId, { keepItems: true });
		} catch (error) {
			if ($mobileComments.open && $mobileComments.postId === postId) {
				submitError = getErrorMessage(
					error,
					activeReplyTarget ? t('comment.replyPostFailed') : t('comment.postFailed')
				);
			}
		} finally {
			if ($mobileComments.open && $mobileComments.postId === postId) {
				submitting = false;
			}
		}
	}

	async function deleteComment(commentId: string, event?: Event): Promise<void> {
		event?.preventDefault();

		const postId = $mobileComments.postId;
		if (!$mobileComments.open || postId === null || deletingId !== null) {
			return;
		}

		deletingId = commentId;
		// Deleting a root comment removes its replies too (backend cascades).
		const removed = 1 + getReplyState(commentId).items.length;

		try {
			await apiClient.delete(API.posts.comment(postId, commentId));

			if ($mobileComments.open && $mobileComments.postId === postId) {
				items = items.filter((comment) => comment.id !== commentId);
				removeReplyState(commentId);
				if (replyTarget?.commentId === commentId) {
					replyTarget = null;
				}
				bumpPostCount(-removed);
			}
		} catch {
			// Keep the existing list intact when delete fails.
		} finally {
			if ($mobileComments.open && $mobileComments.postId === postId) {
				deletingId = null;
			}
		}
	}

	async function deleteReply(commentId: string, replyId: string, event?: Event): Promise<void> {
		event?.preventDefault();

		const postId = $mobileComments.postId;
		if (!$mobileComments.open || postId === null || deletingId !== null) {
			return;
		}

		deletingId = replyId;

		try {
			await apiClient.delete(API.posts.comment(postId, replyId));

			if ($mobileComments.open && $mobileComments.postId === postId) {
				const state = getReplyState(commentId);
				setReplyState(commentId, {
					items: state.items.filter((reply) => reply.id !== replyId)
				});
				bumpPostCount(-1);
			}
		} catch {
			// Keep the existing list intact when delete fails.
		} finally {
			if ($mobileComments.open && $mobileComments.postId === postId) {
				deletingId = null;
			}
		}
	}

	$effect(() => {
		const open = $mobileComments.open;
		const postId = $mobileComments.postId;

		if (!open || postId === null) {
			loadToken += 1;
			composerInset = baseInset();
			resetPanelState();
			return;
		}

		resetPanelState();
		void loadComments(postId);
	});

	$effect(() => {
		if (typeof window === 'undefined') return;

		if (!$mobileComments.open) {
			composerInset = baseInset();
			return;
		}

		applyComposerInset();

		// Topmost back handler while the sheet is open → back closes the
		// sheet only, leaving any underlying overlay stack intact.
		const disposeBack = pushBackHandler(requestClose);

		window.visualViewport?.addEventListener('resize', applyComposerInset);
		window.visualViewport?.addEventListener('scroll', applyComposerInset);

		return () => {
			window.visualViewport?.removeEventListener('resize', applyComposerInset);
			window.visualViewport?.removeEventListener('scroll', applyComposerInset);
			disposeBack();
		};
	});
</script>

{#if $mobileComments.open}
	<div class="comments-root" use:registerSheet>
		<button
			class="comments-backdrop"
			type="button"
			aria-label={t('comment.close')}
			bind:this={backdropEl}
			onclick={requestClose}
			onpointerup={requestClose}
			ontouchend={requestClose}
			in:fade={{ duration: 200 }}
		></button>

		<div
			class="comments-panel glass-strong"
			role="dialog"
			aria-modal="true"
			aria-label="Comments"
			bind:this={panelEl}
			in:fly={{ y: 360, duration: 320, opacity: 1, easing: cubicOut }}
		>
			<!-- Header is the drag affordance: swipe it down to dismiss. The scroll
			     body and composer stay untouched (drag only engages past a small
			     threshold, so taps on the close button still pass through). -->
			<header
				class="comments-header"
				use:elasticDrag={{
					target: () => panelEl,
					dismissDistance: 120,
					dismissVelocity: 0.5,
					onDismiss: () => {
						hapticLight();
						animateClose();
					},
					onProgress: fadeBackdrop,
					positiveOnly: true,
					stretch: 0.04,
					stretchOrigin: 'top center'
				}}
			>
				<div class="comments-handle" aria-hidden="true"></div>
				<div class="comments-header-row">
					<h2 class="comments-title">{t('comment.title')}</h2>
					<button
						class="comments-close-btn"
						type="button"
						aria-label={t('comment.close')}
						onclick={requestClose}
						onpointerup={requestClose}
						ontouchend={requestClose}
					>
						<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
							<line x1="18" y1="6" x2="6" y2="18" />
							<line x1="6" y1="6" x2="18" y2="18" />
						</svg>
					</button>
				</div>
			</header>

			<div class="comments-area">
				{#if loading && items.length === 0}
					<div class="comments-loading" aria-busy="true" aria-label={t('comment.loading')}>
						<span class="loading-dot"></span>
						<span class="loading-dot"></span>
						<span class="loading-dot"></span>
					</div>
				{:else if loadError}
					<div class="comments-state comments-state--error" role="alert">
						<p>{loadError}</p>
						<button
							type="button"
							class="state-btn"
							onclick={reloadComments}
							onpointerup={reloadComments}
							ontouchend={reloadComments}
						>
								{t('common.retry')}
							</button>
					</div>
				{:else if items.length === 0}
					<div class="comments-state">
						<p>{t('comment.empty')}</p>
					</div>
				{:else}
					<ul class="comments-list">
						{#each items as comment (comment.id)}
							{@const author = authorFor(comment)}
							{@const replyState = getReplyState(comment.id)}
							<li
								class="comment-row"
								class:is-deleting={deletingId === comment.id}
								class:is-reply-target={replyTarget?.commentId === comment.id}
							>
								<div class="comment-avatar" aria-hidden="true">
									{#if author.avatarUrl}
										<img src={author.avatarUrl} alt="" class="comment-avatar-img" loading="lazy" decoding="async" />
									{:else}
										<span class="comment-avatar-fallback">
											{getInitials(author.name, author.username)}
										</span>
									{/if}
								</div>

								<div class="comment-content">
									<div class="comment-meta">
										<span class="comment-author">{author.name ?? author.username}</span>
										<time class="comment-time" datetime={comment.createdAt}>
											{commentTime(comment)}
										</time>
										{#if authStore.user?.id === author.id}
											<button
												type="button"
												class="comment-delete"
												aria-label={t('comment.delete')}
												onclick={(event) => deleteComment(comment.id, event)}
												onpointerup={(event) => deleteComment(comment.id, event)}
												ontouchend={(event) => deleteComment(comment.id, event)}
											>
												×
											</button>
										{/if}
									</div>
									<p class="comment-text">{comment.content}</p>

									<div class="comment-actions">
										{#if authStore.isAuthenticated}
											<button
												type="button"
												class="comment-action-btn"
												class:is-active={replyTarget?.commentId === comment.id}
												onclick={(event) => startReply(comment, event)}
												onpointerup={(event) => startReply(comment, event)}
												ontouchend={(event) => startReply(comment, event)}
											>
												{t('post.reply')}
											</button>
										{/if}

										<button
											type="button"
											class="comment-action-btn comment-action-btn--muted"
											aria-expanded={replyState.expanded}
											onclick={(event) => toggleReplies(comment.id, event)}
											onpointerup={(event) => toggleReplies(comment.id, event)}
											ontouchend={(event) => toggleReplies(comment.id, event)}
										>
											{replyToggleLabel(replyState)}
										</button>
									</div>

									{#if replyState.expanded}
										<div class="reply-thread" transition:slide={{ duration: 170 }}>
											{#if replyState.loading}
												<div class="replies-loading" aria-label={t('comment.repliesLoading')}>
													<span class="loading-dot"></span>
													<span class="loading-dot"></span>
													<span class="loading-dot"></span>
												</div>
											{:else if replyState.error}
												<p class="replies-error" role="alert">{replyState.error}</p>
											{:else if replyState.loaded && replyState.items.length === 0}
												<p class="replies-empty">{t('comment.noReplies')}</p>
											{:else if replyState.loaded && replyState.items.length > 0}
												<ul class="replies-list">
													{#each replyState.items as reply (reply.id)}
														{@const replyAuthor = authorFor(reply)}
														<li
															class="reply-row"
															class:is-deleting={deletingId === reply.id}
															in:fade={{ duration: 140 }}
														>
															<div class="reply-avatar" aria-hidden="true">
																{#if replyAuthor.avatarUrl}
																	<img src={replyAuthor.avatarUrl} alt="" class="comment-avatar-img" loading="lazy" decoding="async" />
																{:else}
																	<span class="reply-avatar-fallback">
																		{getInitials(replyAuthor.name, replyAuthor.username)}
																	</span>
																{/if}
															</div>

															<div class="reply-content">
																<div class="reply-meta">
																	<span class="reply-author">
																		{replyAuthor.name ?? replyAuthor.username}
																	</span>
																	<time class="reply-time" datetime={reply.createdAt}>
																		{commentTime(reply)}
																	</time>
																	{#if authStore.user?.id === replyAuthor.id}
																		<button
																			type="button"
																			class="comment-delete reply-delete"
																			aria-label={t('comment.deleteReply')}
																			onclick={(event) => deleteReply(comment.id, reply.id, event)}
																			onpointerup={(event) => deleteReply(comment.id, reply.id, event)}
																			ontouchend={(event) => deleteReply(comment.id, reply.id, event)}
																		>
																			×
																		</button>
																	{/if}
																</div>
																<p class="reply-text">{reply.content}</p>
															</div>
														</li>
													{/each}
												</ul>
											{/if}
										</div>
									{/if}
								</div>
							</li>
						{/each}
					</ul>
				{/if}
			</div>

			{#if authStore.isAuthenticated}
				<form
					class="comments-composer-shell"
					style:padding-bottom={composerInset}
					onsubmit={handleComposerSubmit}
				>
					{#if replyTarget}
						<div class="composer-context" transition:fade={{ duration: 120 }}>
							<div class="composer-context-copy">
								<span class="composer-context-label">{t('comment.replyingTo')}</span>
								<span class="composer-context-user">@{replyTarget.username}</span>
							</div>
							<button
								type="button"
								class="composer-context-close"
								aria-label={t('comment.cancelReply')}
								onclick={clearReplyTarget}
								onpointerup={clearReplyTarget}
								ontouchend={clearReplyTarget}
							>
								×
							</button>
						</div>
					{/if}

					<div class="comments-composer">
						<div class="composer-avatar" aria-hidden="true">
							{#if authStore.user?.avatarUrl}
								<img src={authStore.user.avatarUrl} alt="" class="comment-avatar-img" loading="lazy" decoding="async" />
							{:else}
								<span class="comment-avatar-fallback">
									{getInitials(authStore.user?.name, authStore.user?.username)}
								</span>
							{/if}
						</div>

						<div class="composer-field">
							<input
								bind:this={composerInputEl}
								class="composer-input"
								type="text"
								bind:value={draft}
								oninput={() => {
									submitError = null;
								}}
								onkeydown={handleComposerKeydown}
								placeholder={composerPlaceholder()}
								autocomplete="off"
								autocapitalize="sentences"
								enterkeyhint="send"
								disabled={submitting}
								aria-label={composerAriaLabel()}
							/>
						</div>

						<button
							type="button"
							class="composer-send"
							disabled={!draft.trim() || submitting}
							aria-label={submitAriaLabel()}
							onclick={handleComposerSubmit}
							onpointerup={handleComposerSubmit}
							ontouchend={handleComposerSubmit}
						>
							<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
								<path d="M22 2L11 13" />
								<path d="M22 2L15 22L11 13L2 9L22 2Z" />
							</svg>
						</button>
					</div>

					{#if submitError}
						<p class="composer-error" role="alert">{submitError}</p>
					{/if}
				</form>
			{/if}
		</div>
	</div>
{/if}

<style>
	.comments-root {
		position: fixed;
		inset: 0;
		z-index: 320;
		display: flex;
		align-items: flex-end;
		justify-content: stretch;
	}

	.comments-backdrop {
		position: absolute;
		inset: 0;
		border: none;
		padding: 0;
		margin: 0;
		background: rgba(0, 0, 0, 0.54);
		-webkit-tap-highlight-color: transparent;
		touch-action: manipulation;
	}

	.comments-panel {
		position: relative;
		z-index: 1;
		display: flex;
		flex-direction: column;
		width: 100%;
		height: min(74dvh, 42rem);
		min-height: 19rem;
		border-radius: 1.4rem 1.4rem 0 0;
		/* glass-strong supplies translucent background + blur + border + shadow:
		   readable, but the feed glows softly through. */
		overflow: hidden;
	}

	.comments-header {
		flex-shrink: 0;
		padding: 0.5rem 1rem 0.55rem;
		border-bottom: 1px solid var(--surface-tint-soft);
		/* transparent — lets the panel glass show through (minimal) */
		/* Drag affordance: own the vertical gesture so the swipe-to-dismiss is
		   crisp and never fights native scroll. */
		touch-action: none;
		cursor: grab;
	}

	.comments-header:active {
		cursor: grabbing;
	}

	.comments-handle {
		width: 2.1rem;
		height: 0.24rem;
		border-radius: 9999px;
		background: var(--text-ghost);
		margin: 0 auto 0.45rem;
	}

	.comments-header-row {
		position: relative;
		display: flex;
		align-items: center;
		justify-content: center;
		min-height: 1.8rem;
	}

	.comments-title {
		margin: 0;
		font-size: 0.94rem;
		font-weight: 600;
		letter-spacing: -0.01em;
		color: var(--color-text-primary);
	}

	.comments-close-btn,
	.state-btn,
	.comment-delete,
	.composer-send,
	.comment-action-btn,
	.composer-context-close {
		-webkit-tap-highlight-color: transparent;
		touch-action: manipulation;
	}

	.comments-close-btn {
		position: absolute;
		right: 0;
		top: 50%;
		transform: translateY(-50%);
		width: 1.85rem;
		height: 1.85rem;
		border-radius: 9999px;
		/* Minimal liquid-glass chip */
		background: var(--glass-bg-soft);
		backdrop-filter: blur(calc(var(--glass-blur) + 8px)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(calc(var(--glass-blur) + 8px)) saturate(var(--glass-saturate));
		border: 1px solid var(--glass-border);
		box-shadow: inset 0 1px 0 var(--glass-highlight);
		color: var(--text-secondary-2);
		display: inline-flex;
		align-items: center;
		justify-content: center;
		transition: transform 0.34s cubic-bezier(0.34, 1.56, 0.64, 1), color 0.15s ease;
	}

	.comments-close-btn:active {
		transform: translateY(-50%) scale(0.88);
		transition-duration: 0.07s;
		color: var(--color-text-primary);
	}

	.comments-close-btn svg {
		width: 0.8rem;
		height: 0.8rem;
		stroke-width: 1.75;
	}

	@media (prefers-reduced-motion: reduce) {
		.comments-close-btn { transition: color 0.15s ease; }
		.comments-close-btn:active { transform: translateY(-50%); }
	}

	.comments-area {
		flex: 1;
		min-height: 0;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior-y: contain;
		padding: 0.9rem 1rem 0.4rem;
		background: var(--color-bg);
	}

	.comments-loading {
		display: flex;
		gap: 0.25rem;
		align-items: center;
		padding: 0.45rem 0;
	}

	.loading-dot {
		width: 0.3125rem;
		height: 0.3125rem;
		border-radius: 50%;
		background: var(--text-quaternary);
		animation: blink 1.2s ease-in-out infinite;
	}

	.loading-dot:nth-child(2) {
		animation-delay: 0.2s;
	}

	.loading-dot:nth-child(3) {
		animation-delay: 0.4s;
	}

	.comments-state {
		padding: 0.45rem 0 1rem;
		color: var(--text-tertiary);
		font-size: 0.9rem;
	}

	.comments-state p {
		margin: 0;
	}

	.comments-state--error {
		display: flex;
		flex-direction: column;
		align-items: flex-start;
		gap: 0.65rem;
	}

	.state-btn {
		border: none;
		border-radius: 9999px;
		padding: 0.42rem 0.88rem;
		background: var(--surface-tint-medium);
		color: var(--color-text-primary);
		font: inherit;
	}

	.comments-list,
	.replies-list {
		list-style: none;
		padding: 0;
		margin: 0;
	}

	.comments-list {
		display: flex;
		flex-direction: column;
		gap: 1rem;
	}

	.comment-row {
		display: flex;
		gap: 0.72rem;
		align-items: flex-start;
		transition: opacity 0.16s ease, transform 0.16s ease;
	}

	.comment-row.is-deleting {
		opacity: 0.45;
	}

	.comment-row.is-reply-target .comment-content {
		background: var(--surface-tint-faint);
	}

	.comment-avatar,
	.composer-avatar {
		width: 1.95rem;
		height: 1.95rem;
		flex-shrink: 0;
		border-radius: 9999px;
		overflow: hidden;
		background: var(--surface-tint-medium);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.comment-avatar-img {
		width: 100%;
		height: 100%;
		object-fit: cover;
	}

	.comment-avatar-fallback,
	.reply-avatar-fallback {
		font-size: 0.72rem;
		font-weight: 700;
		color: var(--text-tertiary);
	}

	.comment-content {
		flex: 1;
		min-width: 0;
		border-radius: 1rem;
		padding: 0.02rem 0;
	}

	.comment-meta,
	.reply-meta {
		display: flex;
		align-items: center;
		gap: 0.42rem;
		margin-bottom: 0.16rem;
	}

	.comment-author,
	.reply-author {
		font-size: 0.82rem;
		font-weight: 600;
		color: var(--color-text-primary);
	}

	.comment-time,
	.reply-time {
		font-size: 0.74rem;
		color: var(--text-quaternary);
	}

	.comment-delete {
		margin-left: auto;
		border: none;
		background: none;
		color: var(--text-quaternary);
		font-size: 1rem;
		line-height: 1;
	}

	.comment-text,
	.reply-text {
		margin: 0;
		color: var(--color-text-primary);
		font-size: 0.91rem;
		line-height: 1.42;
		word-break: break-word;
	}

	.comment-actions {
		display: flex;
		align-items: center;
		gap: 0.85rem;
		margin-top: 0.35rem;
	}

	.comment-action-btn {
		padding: 0;
		border: none;
		background: none;
		color: var(--text-tertiary);
		font: inherit;
		font-size: 0.78rem;
		line-height: 1.2;
	}

	.comment-action-btn.is-active,
	.comment-action-btn:not(.comment-action-btn--muted) {
		color: var(--text-secondary-2);
	}

	.comment-action-btn--muted {
		color: var(--text-quaternary);
	}

	.reply-thread {
		margin-top: 0.58rem;
		padding-left: 0.2rem;
	}

	.replies-loading,
	.replies-empty,
	.replies-error {
		margin: 0;
		padding-left: 0.75rem;
		color: var(--text-quaternary);
		font-size: 0.78rem;
	}

	.replies-loading {
		display: flex;
		align-items: center;
		gap: 0.25rem;
		padding-top: 0.2rem;
	}

	.replies-list {
		display: flex;
		flex-direction: column;
		gap: 0.7rem;
		padding-left: 0.78rem;
		border-left: 1px solid var(--surface-tint-medium);
	}

	.reply-row {
		display: flex;
		gap: 0.6rem;
		align-items: flex-start;
	}

	.reply-row.is-deleting {
		opacity: 0.45;
	}

	.reply-avatar {
		width: 1.45rem;
		height: 1.45rem;
		flex-shrink: 0;
		border-radius: 9999px;
		overflow: hidden;
		background: var(--surface-tint-medium);
		display: flex;
		align-items: center;
		justify-content: center;
	}

	.reply-avatar-fallback {
		font-size: 0.58rem;
	}

	.reply-content {
		flex: 1;
		min-width: 0;
	}

	.reply-author {
		font-size: 0.76rem;
	}

	.reply-time {
		font-size: 0.69rem;
	}

	.reply-text {
		font-size: 0.84rem;
		line-height: 1.4;
	}

	.reply-delete {
		font-size: 0.92rem;
	}

	.comments-composer-shell {
		flex-shrink: 0;
		background: var(--color-bg);
		border-top: 1px solid var(--surface-tint-soft);
	}

	.composer-context {
		display: flex;
		align-items: center;
		justify-content: space-between;
		gap: 0.6rem;
		padding: 0.55rem 0.9rem 0;
	}

	.composer-context-copy {
		display: flex;
		align-items: center;
		gap: 0.35rem;
		min-width: 0;
	}

	.composer-context-label {
		font-size: 0.76rem;
		color: var(--text-quaternary);
	}

	.composer-context-user {
		font-size: 0.76rem;
		font-weight: 600;
		color: var(--text-strong);
		white-space: nowrap;
		overflow: hidden;
		text-overflow: ellipsis;
	}

	.composer-context-close {
		width: 1.5rem;
		height: 1.5rem;
		flex-shrink: 0;
		border: none;
		border-radius: 9999px;
		background: var(--surface-tint-soft);
		color: var(--text-secondary-2);
		font-size: 1rem;
		line-height: 1;
	}

	.comments-composer {
		display: flex;
		align-items: center;
		gap: 0.68rem;
		padding: 0.68rem 0.9rem 0;
	}

	.composer-field {
		flex: 1;
		min-width: 0;
	}

	.composer-input {
		width: 100%;
		height: 2.65rem;
		border: 1px solid var(--surface-tint-soft);
		border-radius: 9999px;
		background: var(--color-surface-raised);
		color: var(--color-text-primary);
		padding: 0 0.98rem;
		font: inherit;
		outline: none;
	}

	.composer-input::placeholder {
		color: var(--text-quaternary);
	}

	.composer-input:disabled {
		opacity: 0.7;
	}

	.composer-send {
		width: 2rem;
		height: 2rem;
		flex-shrink: 0;
		border: none;
		border-radius: 9999px;
		background: transparent;
		color: var(--tg-accent, #4da3ff);
		display: inline-flex;
		align-items: center;
		justify-content: center;
	}

	.composer-send:disabled {
		opacity: 0.3;
	}

	.composer-send svg {
		width: 1rem;
		height: 1rem;
	}

	.composer-error {
		margin: 0;
		padding: 0.45rem 0.95rem 0;
		font-size: 0.8rem;
		color: #ff7b7b;
	}

	@keyframes blink {
		0%,
		100% {
			opacity: 0.3;
		}
		50% {
			opacity: 1;
		}
	}
</style>
