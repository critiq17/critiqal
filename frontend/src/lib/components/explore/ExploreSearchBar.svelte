<script lang="ts">
	import type { ExploreTab } from '$lib/features/explore/useSearch.svelte';

	interface Props {
		query: string;
		activeTab: ExploreTab;
		inputEl?: HTMLInputElement;
		onQueryChange: (q: string) => void;
		onTabChange: (tab: ExploreTab) => void;
		onInputBind: (el: HTMLInputElement) => void;
	}

	let { query, activeTab, onQueryChange, onTabChange, onInputBind }: Props = $props();
</script>

<div class="search-bar">
	<div class="search-wrapper">
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

	<div class="tab-bar" role="tablist" aria-label="Content type">
		<button
			class="tab-btn"
			class:active={activeTab === 'posts'}
			role="tab"
			aria-selected={activeTab === 'posts'}
			type="button"
			onclick={() => onTabChange('posts')}
		>Posts</button>
		<button
			class="tab-btn"
			class:active={activeTab === 'people'}
			role="tab"
			aria-selected={activeTab === 'people'}
			type="button"
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

<style>
	.search-bar {
		position: sticky;
		top: 0;
		background: var(--color-bg, #0f0f0f);
		padding: 12px 16px 0;
		z-index: 10;
		backdrop-filter: blur(12px);
		-webkit-backdrop-filter: blur(12px);
	}

	.search-wrapper {
		position: relative;
		display: flex;
		align-items: center;
		margin-bottom: 10px;
	}

	.search-icon {
		position: absolute;
		left: 14px;
		top: 50%;
		transform: translateY(-50%);
		width: 16px;
		height: 16px;
		color: rgba(255, 255, 255, 0.4);
		pointer-events: none;
		flex-shrink: 0;
	}

	.search-input {
		width: 100%;
		height: 44px;
		border-radius: 22px;
		background: var(--color-surface-raised, #242424);
		border: 1px solid transparent;
		padding: 0 44px 0 44px;
		font-size: 15px;
		font-family: inherit;
		color: var(--color-text-primary, #f0f0f0);
		outline: none;
		box-sizing: border-box;
		transition: border-color 0.18s ease, box-shadow 0.18s ease;
		-webkit-appearance: none;
	}

	.search-input::placeholder { color: rgba(255, 255, 255, 0.35); }

	.search-input:focus {
		border-color: var(--color-accent, #e05252);
		box-shadow: 0 0 0 3px color-mix(in srgb, var(--color-accent, #e05252) 15%, transparent);
	}

	.search-input::-webkit-search-cancel-button { display: none; }

	.search-clear {
		position: absolute;
		right: 10px;
		top: 50%;
		transform: translateY(-50%);
		background: none;
		border: none;
		cursor: pointer;
		color: rgba(255, 255, 255, 0.4);
		padding: 4px;
		display: flex;
		align-items: center;
		border-radius: 50%;
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
		transition: left 0.2s ease, width 0.2s ease;
	}
</style>
