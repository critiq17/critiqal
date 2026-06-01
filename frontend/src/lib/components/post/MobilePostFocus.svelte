<script lang="ts">
	import { onDestroy } from 'svelte';
	import { fade } from 'svelte/transition';
	import { backOut } from 'svelte/easing';
	import type { TransitionConfig } from 'svelte/transition';
	import { portal } from '$lib/actions/portal';
	import { elasticDrag } from '$lib/actions/elasticDrag';
	import { pushBackButton, hapticLight } from '$lib/tma/buttons';
	import { mobilePostFocus } from '$lib/stores/mobile-post-focus.store.svelte';
	import { openProfile } from '$lib/stores/profile-nav.store.svelte';
	import { reducedMotion } from '$lib/ui/reducedMotion.svelte';
	import Post from './Post.svelte';

	const post = $derived(mobilePostFocus.post);

	let scrollEl = $state<HTMLElement | null>(null);
	let cardEl = $state<HTMLElement | null>(null);
	let dimEl = $state<HTMLElement | null>(null);
	let dismissing = false;

	function close(): void {
		mobilePostFocus.close();
	}

	// Animated dismiss (backdrop tap, back, Escape, swipe): the card rides down
	// and the dim fades, then unmount. On a swipe it continues from wherever the
	// drag left the card, so the gesture flows straight into the exit.
	function dismiss(): void {
		if (dismissing) return;
		if (reducedMotion.value) {
			close();
			return;
		}
		dismissing = true;
		if (cardEl) {
			cardEl.style.transition = 'transform 0.28s cubic-bezier(0.32, 0.72, 0, 1), opacity 0.24s ease';
			cardEl.style.transform = 'translateY(110%)';
			cardEl.style.opacity = '0';
		}
		if (dimEl) {
			dimEl.style.transition = 'opacity 0.24s ease';
			dimEl.style.opacity = '0';
		}
		window.setTimeout(close, 260);
	}

	// Only the top edge arms the pull-to-dismiss; mid-scroll and the bottom edge
	// stay native so a downward flick there doesn't yank the post away.
	function atTop(): boolean {
		return scrollEl ? scrollEl.scrollTop <= 0 : true;
	}

	function fadeDim(progress: number): void {
		if (dimEl) dimEl.style.opacity = String(1 - progress);
	}

	$effect(() => {
		if (!post) return;
		const disposeBack = pushBackButton(dismiss);
		const prev = document.body.style.overflow;
		document.body.style.overflow = 'hidden';
		return () => {
			disposeBack();
			document.body.style.overflow = prev;
		};
	});

	onDestroy(() => {
		document.body.style.overflow = '';
	});

	// Springs up from slightly below — tactile, not jerky.
	function cardIn(_node: HTMLElement): TransitionConfig {
		if (reducedMotion.value) return { duration: 0 };
		return {
			duration: 340,
			easing: backOut,
			css: (t, u) =>
				`opacity: ${t}; transform: translateY(${20 * u}px) scale(${0.95 + 0.05 * t})`,
		};
	}

</script>

<svelte:window onkeydown={(e) => e.key === 'Escape' && dismiss()} />

{#if post}
	<div class="focus-root" use:portal role="dialog" aria-modal="true" aria-label="Focused post">
		<!--
			Two-layer backdrop: the blur layer appears instantly (no animation)
			so the GPU never has to repaint the filter on every frame.
			The dim layer carries only a cheap opacity fade.
		-->
		<div class="blur-layer"></div>
		<button
			class="dim-layer"
			type="button"
			aria-label="Close"
			bind:this={dimEl}
			onclick={dismiss}
			in:fade={{ duration: 160 }}
		></button>

		<div class="focus-scroll" bind:this={scrollEl}>
			<div
				class="focus-card"
				bind:this={cardEl}
				use:elasticDrag={{
					axis: 'y',
					positiveOnly: true,
					dismissDistance: 130,
					dismissVelocity: 0.5,
					onDismiss: () => {
						hapticLight();
						dismiss();
					},
					onProgress: fadeDim,
					stretch: 0.04,
					stretchOrigin: 'top center',
					stiffness: 200,
					damping: 20,
					canStart: atTop,
				}}
				in:cardIn
			>
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

	/*
		Blur layer: never animates its opacity, so the GPU computes the filter
		exactly once (on mount) rather than on every animation frame.
		Placed on its own compositor layer via translateZ(0).
	*/
	.blur-layer {
		position: absolute;
		inset: 0;
		backdrop-filter: blur(10px) saturate(110%);
		-webkit-backdrop-filter: blur(10px) saturate(110%);
		transform: translateZ(0);
		pointer-events: none;
	}

	/* Dim layer: alpha-only, no filter — GPU just blends one color per pixel. */
	.dim-layer {
		position: absolute;
		inset: 0;
		width: 100%;
		height: 100%;
		border: 0;
		padding: 0;
		background: rgba(0, 0, 0, 0.44);
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
		align-items: center;
		justify-content: center;
		padding: max(env(safe-area-inset-top, 0px), 20px) 12px
			max(env(safe-area-inset-bottom, 0px), 20px);
		pointer-events: none;
	}

	.focus-scroll::-webkit-scrollbar {
		display: none;
	}

	.focus-card {
		position: relative;
		width: 100%;
		max-width: 520px;
		pointer-events: auto;
		will-change: transform, opacity;
		isolation: isolate;
	}
</style>
