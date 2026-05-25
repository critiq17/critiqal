<script lang="ts">
	interface Props {
		platform: string | null | undefined;
		size?: number;
		stroke?: number;
	}

	let { platform, size = 18, stroke = 1.6 }: Props = $props();

	let kind = $derived(detectKind(platform));

	function detectKind(p: string | null | undefined): 'mobile' | 'desktop' | 'telegram' | 'globe' {
		const v = (p ?? '').toLowerCase();
		if (v === 'telegram') return 'telegram';
		if (v === 'ios' || v === 'android') return 'mobile';
		if (v === 'windows' || v === 'macos' || v === 'linux') return 'desktop';
		return 'globe';
	}
</script>

<svg
	width={size}
	height={size}
	viewBox="0 0 24 24"
	fill="none"
	stroke="currentColor"
	stroke-width={stroke}
	stroke-linecap="round"
	stroke-linejoin="round"
	aria-hidden="true"
>
	{#if kind === 'mobile'}
		<rect x="7" y="3" width="10" height="18" rx="2.5" />
		<line x1="11" y1="18" x2="13" y2="18" />
	{:else if kind === 'desktop'}
		<rect x="3" y="4" width="18" height="12" rx="2" />
		<line x1="8" y1="20" x2="16" y2="20" />
		<line x1="12" y1="16" x2="12" y2="20" />
	{:else if kind === 'telegram'}
		<path d="M21 4 3 11l6 2 2 6 3-4 5 4z" />
	{:else}
		<circle cx="12" cy="12" r="9" />
		<path d="M3 12h18" />
		<path d="M12 3a14 14 0 0 1 0 18a14 14 0 0 1 0-18z" />
	{/if}
</svg>
