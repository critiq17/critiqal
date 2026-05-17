<script lang="ts">
	// Shared profile/settings header: a centered title that rides high between
	// the native Telegram header buttons, invisible at rest and frosting into
	// glass on scroll. Place it as the FIRST child inside the scroll container
	// (it is position: sticky) and drive `scrolled` from that container's
	// scroll position. Back is handled by the native Telegram BackButton.
	interface Props {
		title: string;
		scrolled: boolean;
	}

	let { title, scrolled }: Props = $props();
</script>

<header class="collapsing-header" class:scrolled>
	<h1 class="collapsing-header__title">{title}</h1>
</header>

<style>
	.collapsing-header {
		position: sticky;
		top: 0;
		z-index: 10;
		display: flex;
		align-items: center;
		justify-content: center;
		padding: calc(
				0.4rem +
					var(--tg-content-top, var(--tg-content-safe-area-inset-top, env(safe-area-inset-top, 6px)))
			)
			3rem 0.55rem;
		background: transparent;
		border-bottom: 1px solid transparent;
		transition: background 0.25s ease, border-color 0.25s ease;
	}

	.collapsing-header.scrolled {
		background: var(--glass-bg);
		backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(var(--glass-blur)) saturate(var(--glass-saturate));
		border-bottom-color: var(--glass-border);
	}

	.collapsing-header__title {
		margin: 0;
		font-size: 1.05rem;
		font-weight: 700;
		color: var(--tg-text, #f0f0f0);
		letter-spacing: -0.015em;
		max-width: 70vw;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
		text-align: center;
	}
</style>
