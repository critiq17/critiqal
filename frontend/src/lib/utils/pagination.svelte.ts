import type { PageResponse } from '$lib/types';

export class PaginatedLoader<T> {
	items = $state<T[]>([]);
	page = $state(0);
	hasNext = $state(false);
	isLoading = $state(false);
	isLoadingMore = $state(false);
	error = $state<string | null>(null);

	readonly #fetcher: (page: number) => Promise<PageResponse<T>>;

	constructor(fetcher: (page: number) => Promise<PageResponse<T>>) {
		this.#fetcher = fetcher;
	}

	async load(): Promise<void> {
		this.page = 0;
		this.items = [];
		this.hasNext = false;
		this.error = null;
		this.isLoading = true;
		try {
			const res = await this.#fetcher(0);
			this.items = res.content;
			this.hasNext = res.hasNext;
		} catch (err) {
			this.error = err instanceof Error ? err.message : 'Failed to load.';
		} finally {
			this.isLoading = false;
		}
	}

	async loadMore(): Promise<void> {
		if (!this.hasNext || this.isLoadingMore) return;
		this.isLoadingMore = true;
		try {
			const nextPage = this.page + 1;
			const res = await this.#fetcher(nextPage);
			this.items = [...this.items, ...res.content];
			this.page = nextPage;
			this.hasNext = res.hasNext;
		} catch {
			// Non-fatal: leave state as-is
		} finally {
			this.isLoadingMore = false;
		}
	}
}
