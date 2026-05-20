<script lang="ts">
	import { onDestroy } from 'svelte';
	import { fade, scale } from 'svelte/transition';
	import { cubicOut } from 'svelte/easing';
	import { portal } from '$lib/actions/portal';
	import { elasticDrag } from '$lib/actions/elasticDrag';
	import { pushBackButton } from '$lib/tma/buttons';
	import { mobilePostFocus } from '$lib/stores/mobile-post-focus.store.svelte';
	import { openProfile } from '$lib/stores/profile-nav.store.svelte';
	import Post from './Post.svelte';

	const post = $derived(mobilePostFocus.post);

	let scrollEl = $state<HTMLElement | null>(null);

	function close(): void {
		mobilePostFocus.close();
	}

	function onBackdropClick(): void {
		close();
	}

	function onKeydown(e: KeyboardEvent): void {
		if (e.key === 'Escape') close();
	}

	// Liquid-glass pull: the card deforms in place (never translates) and
	// springs back with inertia — same physics as the menu, pinned. It only
	// engages at the scroll edges (or when comments fit without scrolling),
	// so mid-list it stays a normal comment scroll and never hijacks it.
	function atScrollEdge(): boolean {
		const el = scrollEl;
		if (!el) return true;
		const max = el.scrollHeight - el.clientHeight;
		if (max <= 1) return true;
		return el.scrollTop <= 0 || el.scrollTop >= max - 1;
	}

	// While focused: TG BackButton closes instead of leaving the screen,
	// and the feed behind cannot scroll.
	$effect(() => {
		if (!post) return;
		const disposeBack = pushBackButton(close);
		const prevOverflow = document.body.style.overflow;
		document.body.style.overflow = 'hidden';
		return () => {
			disposeBack();
			document.body.style.overflow = prevOverflow;
		};
	});

	onDestroy(() => {
		document.body.style.overflow = '';
	});
</script>

<svelte:window onkeydown={onKeydown} />

{#if post}
	<div class="focus-root" use:portal role="dialog" aria-modal="true" aria-label="Focused post">
		<button
			class="focus-backdrop"
			type="button"
			aria-label="Close"
			onclick={onBackdropClick}
			transition:fade={{ duration: 200 }}
		></button>

		<div class="focus-scroll" bind:this={scrollEl}>
			<div
				class="focus-card"
				use:elasticDrag={{
					axis: 'y',
					pinned: true,
					stretch: 0.08,
					stretchOrigin: 'center',
					stiffness: 210,
					damping: 17,
					canStart: atScrollEdge
				}}
				in:scale={{ duration: 240, start: 0.92, opacity: 0, easing: cubicOut }}
				out:scale={{ duration: 180, start: 0.95, opacity: 0, easing: cubicOut }}
			>
				<span class="grabber" aria-hidden="true"></span>
				{#key post.id}
					<Post
						post={post}
						variant="mobile"
						focused
						onAuthorClick={(username) => {
							close();
							openProfile(username);
						}}
						onDeleted={close}
					/>
				{/key}
			</div>
		</div>
	</div>
{/if}

<style>
	.focus-root {
		position: fixed;
		inset: 0;
		z-index: 1000;
	}

	/* The whole feed behind reads as glass: heavy blur + dim. */
	.focus-backdrop {
		position: absolute;
		inset: 0;
		width: 100%;
		height: 100%;
		border: 0;
		padding: 0;
		background: rgba(0, 0, 0, 0.5);
		backdrop-filter: blur(14px) saturate(120%);
		-webkit-backdrop-filter: blur(14px) saturate(120%);
		/* Own compositor layer: the scrolling comment list above must not
		   force this blur to repaint each frame. */
		transform: translateZ(0);
		cursor: pointer;
		-webkit-tap-highlight-color: transparent;
	}

	.focus-scroll {
		position: absolute;
		inset: 0;
		overflow-y: auto;
		overflow-x: hidden;
		-webkit-overflow-scrolling: touch;
		overscroll-behavior: contain;
		scrollbar-width: none;
		display: flex;
		flex-direction: column;
		justify-content: center;
		padding: max(env(safe-area-inset-top, 0px), 24px) 12px
			max(env(safe-area-inset-bottom, 0px), 24px);
		pointer-events: none;
	}

	.focus-scroll::-webkit-scrollbar {
		display: none;
	}

	.focus-card {
		position: relative;
		width: 100%;
		max-width: 520px;
		margin: auto;
		pointer-events: auto;
		will-change: transform;
	}

	.grabber {
		display: block;
		width: 36px;
		height: 4px;
		border-radius: 999px;
		background: var(--text-ghost);
		margin: 0 auto 8px;
	}

	@media (prefers-reduced-motion: reduce) {
		.focus-card {
			transition: none !important;
		}
	}
</style>
