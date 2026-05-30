<script lang="ts">
	import { onDestroy } from 'svelte';
	import { fade } from 'svelte/transition';
	import { backOut, cubicIn } from 'svelte/easing';
	import type { TransitionConfig } from 'svelte/transition';
	import { portal } from '$lib/actions/portal';
	import { elasticDrag } from '$lib/actions/elasticDrag';
	import { pushBackButton } from '$lib/tma/buttons';
	import { mobilePostFocus } from '$lib/stores/mobile-post-focus.store.svelte';
	import { openProfile } from '$lib/stores/profile-nav.store.svelte';
	import { reducedMotion } from '$lib/ui/reducedMotion.svelte';
	import Post from './Post.svelte';

	const post = $derived(mobilePostFocus.post);

	let scrollEl = $state<HTMLElement | null>(null);

	function close(): void {
		mobilePostFocus.close();
	}

	function atScrollEdge(): boolean {
		const el = scrollEl;
		if (!el) return true;
		const max = el.scrollHeight - el.clientHeight;
		if (max <= 1) return true;
		return el.scrollTop <= 0 || el.scrollTop >= max - 1;
	}

	$effect(() => {
		if (!post) return;
		const disposeBack = pushBackButton(close);
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

	// Fades and drops away cleanly — snappy, no linger.
	function cardOut(_node: HTMLElement): TransitionConfig {
		if (reducedMotion.value) return { duration: 0 };
		return {
			duration: 190,
			easing: cubicIn,
			css: (t, u) =>
				`opacity: ${t}; transform: translateY(${10 * u}px) scale(${0.97 + 0.03 * t})`,
		};
	}
</script>

<svelte:window onkeydown={(e) => e.key === 'Escape' && close()} />

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
			onclick={close}
			transition:fade={{ duration: 160 }}
		></button>

		<div class="focus-scroll" bind:this={scrollEl}>
			<div
				class="focus-card"
				use:elasticDrag={{
					axis: 'y',
					pinned: true,
					stretch: 0.06,
					stretchOrigin: 'center',
					stiffness: 200,
					damping: 20,
					canStart: atScrollEdge,
				}}
				in:cardIn
				out:cardOut
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
