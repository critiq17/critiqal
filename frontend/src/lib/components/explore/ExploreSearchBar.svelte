<script lang="ts">
	import type { ExploreTab } from '$lib/features/explore/useSearch.svelte';
	import { elasticDrag } from '$lib/actions/elasticDrag';

	interface Props {
		query: string;
		activeTab: ExploreTab;
		collapsed?: boolean;
		onQueryChange: (q: string) => void;
		onTabChange: (tab: ExploreTab) => void;
		onInputBind: (el: HTMLInputElement) => void;
	}

	let { query, activeTab, collapsed = false, onQueryChange, onTabChange, onInputBind }: Props = $props();
</script>

<div class="search-bar" class:collapsed>
	<!-- Glass pill with the same liquid stretch/inertia physics as the bottom menu -->
	<div
		class="search-pill glass glass-soft"
		use:elasticDrag={{
			axis: 'free',
			stretch: 0.14,
			pinned: true,
			stretchOrigin: 'center',
			stiffness: 240,
			damping: 14
		}}
	>
		<svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
			<circle cx="11" cy="11" r="8" />
			<line x1="21" y1="21" x2="16.65" y2="16.65" />
		</svg>
		<input
			use:onInputBind
			value={query}
			oninput={(e) => onQueryChange((e.target as HTMLInputElement).value)}
			class="search-input"
			type="search"
			placeholder="Search posts or people…"
			aria-label="Search"
			autocomplete="off"
			spellcheck="false"
		/>
		{#if query}
			<button class="search-clear" onclick={() => onQueryChange('')} aria-label="Clear search" type="button">
				<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" aria-hidden="true">
					<line x1="18" y1="6" x2="6" y2="18" />
					<line x1="6" y1="6" x2="18" y2="18" />
				</svg>
			</button>
		{/if}
	</div>

	<div class="tabs-wrap" aria-hidden={collapsed}>
		<div class="tab-bar" role="tablist" aria-label="Content type">
			<button
				class="tab-btn"
				class:active={activeTab === 'posts'}
				role="tab"
				aria-selected={activeTab === 'posts'}
				type="button"
				tabindex={collapsed ? -1 : 0}
				onclick={() => onTabChange('posts')}
			>Posts</button>
			<button
				class="tab-btn"
				class:active={activeTab === 'people'}
				role="tab"
				aria-selected={activeTab === 'people'}
				type="button"
				tabindex={collapsed ? -1 : 0}
				onclick={() => onTabChange('people')}
			>People</button>
			<span
				class="tab-indicator"
				style:left={activeTab === 'posts' ? '0%' : '50%'}
				style:width="50%"
				aria-hidden="true"
			></span>
		</div>
	</div>
</div>

<style>
	/* Always frosted glass — readable over any scrolling content, in both
	   states (the tabs come back as semi-transparent glass, not bare). The
	   bar owns the TG-header clearance so collapse can shrink it and tuck
	   the pill up between the native Close / ⋯ buttons. */
	.search-bar {
		position: sticky;
		top: 0;
		padding: var(--tg-top-clearance) 16px 0;
		z-index: 10;
		background-color: var(--glass-bg-soft, rgba(20, 20, 20, 0.5));
		backdrop-filter: blur(calc(var(--glass-blur, 24px) + 8px)) saturate(var(--glass-saturate, 180%));
		-webkit-backdrop-filter: blur(calc(var(--glass-blur, 24px) + 8px)) saturate(var(--glass-saturate, 180%));
		border-bottom: 1px solid var(--glass-border, rgba(255, 255, 255, 0.08));
		/* GPU-cheap + a tiny localized padding reflow — smooth, no jank */
		transition:
			padding-top 0.34s cubic-bezier(0.4, 0, 0.2, 1),
			padding-bottom 0.34s cubic-bezier(0.4, 0, 0.2, 1);
	}

	/* Collapsed: bar slims and rides up into the ~44px native header band;
	   only the search input stays, narrowed between the TG buttons. */
	.search-bar.collapsed {
		padding-top: calc(env(safe-area-inset-top, 20px) + 4px);
		padding-bottom: 8px;
	}

	.search-bar.collapsed .search-pill {
		max-width: min(62%, 360px);
		height: 38px;
		border-radius: 19px;
		margin-bottom: 0;
	}

	/* Tabs collapse vertically with a single eased clip — no layout thrash. */
	.tabs-wrap {
		overflow: hidden;
		max-height: 48px;
		opacity: 1;
		transform: translateY(0);
		transition:
			max-height 0.32s cubic-bezier(0.4, 0, 0.2, 1),
			opacity 0.22s ease,
			transform 0.32s cubic-bezier(0.4, 0, 0.2, 1);
	}

	.search-bar.collapsed .tabs-wrap {
		max-height: 0;
		opacity: 0;
		transform: translateY(-8px);
		pointer-events: none;
	}

	/* Transparent glass pill, no explicit border — only the soft inset
	   highlight from .glass gives it shape. elasticDrag owns `transform`. */
	.search-pill {
		position: relative;
		display: flex;
		align-items: center;
		width: 100%;
		max-width: 100%;
		height: 44px;
		border-radius: 22px;
		border-color: transparent;
		margin: 0 auto 12px;
		padding: 0 12px;
		touch-action: none;
		will-change: transform;
		transition:
			box-shadow 0.2s ease,
			max-width 0.34s cubic-bezier(0.4, 0, 0.2, 1),
			height 0.34s cubic-bezier(0.4, 0, 0.2, 1),
			border-radius 0.34s cubic-bezier(0.4, 0, 0.2, 1),
			margin-bottom 0.34s cubic-bezier(0.4, 0, 0.2, 1);
	}

	.search-pill:focus-within {
		box-shadow: var(--glass-shadow, 0 8px 32px rgba(0, 0, 0, 0.4)),
			inset 0 1px 0 var(--glass-highlight, rgba(255, 255, 255, 0.1)),
			0 0 0 1px color-mix(in srgb, var(--tg-accent, #e05252) 30%, transparent);
	}

	.search-icon {
		width: 16px;
		height: 16px;
		margin: 0 8px 0 4px;
		color: rgba(255, 255, 255, 0.4);
		pointer-events: none;
		flex-shrink: 0;
	}

	.search-input {
		flex: 1;
		min-width: 0;
		height: 100%;
		background: transparent;
		border: none;
		padding: 0;
		font-size: 15px;
		font-family: inherit;
		color: var(--color-text-primary, #f0f0f0);
		outline: none;
		-webkit-appearance: none;
	}

	.search-input::placeholder { color: rgba(255, 255, 255, 0.4); }
	.search-input::-webkit-search-cancel-button { display: none; }

	.search-clear {
		background: none;
		border: none;
		cursor: pointer;
		color: rgba(255, 255, 255, 0.4);
		padding: 4px;
		margin-left: 4px;
		display: flex;
		align-items: center;
		border-radius: 50%;
		flex-shrink: 0;
		transition: color 0.15s ease, background-color 0.15s ease;
	}

	.search-clear svg { width: 14px; height: 14px; }

	.search-clear:hover {
		color: var(--color-text-primary, #f0f0f0);
		background-color: rgba(255, 255, 255, 0.1);
	}

	.tab-bar {
		display: flex;
		position: relative;
		border-bottom: 1px solid var(--color-border, rgba(255, 255, 255, 0.08));
	}

	.tab-btn {
		flex: 1;
		text-align: center;
		padding: 10px 0;
		background: none;
		border: none;
		cursor: pointer;
		font-size: 14px;
		font-weight: 500;
		font-family: inherit;
		color: rgba(255, 255, 255, 0.5);
		transition: color 0.2s ease;
		position: relative;
		z-index: 1;
	}

	.tab-btn:hover { color: var(--color-text-primary, #f0f0f0); }
	.tab-btn.active { color: var(--color-text-primary, #f0f0f0); font-weight: 600; }

	.tab-indicator {
		position: absolute;
		bottom: -1px;
		height: 2px;
		background: var(--tg-accent, #e05252);
		border-radius: 1px 1px 0 0;
		transition: left 0.28s cubic-bezier(0.34, 1.3, 0.64, 1), width 0.28s ease;
	}
</style>
