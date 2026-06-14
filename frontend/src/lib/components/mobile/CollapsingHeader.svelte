<script lang="ts">
	// Shared profile/settings header: a centered title that rides high between
	// the native Telegram header buttons, invisible at rest and frosting into
	// glass on scroll. Place it as the FIRST child inside the scroll container
	// (it is position: sticky) and drive `progress` from that container's
	// scroll position (0..1). Back is handled by the native Telegram BackButton.
	interface Props {
		title: string;
		progress: number;
	}

	let { title, progress }: Props = $props();
</script>

<header
	class="collapsing-header"
	style="--header-progress: {progress}"
>
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
		background-color: color-mix(in srgb, var(--color-bg) calc(var(--header-progress, 0) * 82%), transparent);
		backdrop-filter: blur(calc(var(--header-progress, 0) * var(--glass-blur-md))) saturate(var(--glass-saturate));
		-webkit-backdrop-filter: blur(calc(var(--header-progress, 0) * var(--glass-blur-md))) saturate(var(--glass-saturate));
	}

	.collapsing-header::after {
		content: '';
		position: absolute;
		left: 0;
		right: 0;
		bottom: 0;
		height: 1px;
		background: linear-gradient(to right, transparent, var(--glass-border), transparent);
		opacity: var(--header-progress, 0);
		pointer-events: none;
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
