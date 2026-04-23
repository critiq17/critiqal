export type QueryStatus = 'idle' | 'loading' | 'success' | 'error';

export class Query<T> {
	data = $state<T | null>(null);
	error = $state<Error | null>(null);
	status = $state<QueryStatus>('idle');

	private loadedAt: number | null = null;
	private abortController: AbortController | null = null;

	constructor(
		private fetcher: (signal: AbortSignal) => Promise<T>,
		private staleTime = 30_000
	) {}

	get isLoading(): boolean {
		return this.status === 'loading';
	}

	get isStale(): boolean {
		return this.loadedAt === null || Date.now() - this.loadedAt > this.staleTime;
	}

	async fetch(force = false): Promise<T | null> {
		if (!force && !this.isStale && this.status === 'success') return this.data;

		this.abortController?.abort();
		this.abortController = new AbortController();
		this.status = 'loading';
		this.error = null;

		try {
			const result = await this.fetcher(this.abortController.signal);
			this.data = result;
			this.loadedAt = Date.now();
			this.status = 'success';
			return result;
		} catch (err) {
			if (err instanceof Error && err.name === 'AbortError') return this.data;
			this.error = err instanceof Error ? err : new Error(String(err));
			this.status = 'error';
			return null;
		}
	}

	abort(): void {
		this.abortController?.abort();
	}

	invalidate(): void {
		this.loadedAt = null;
	}

	setData(data: T): void {
		this.data = data;
		this.loadedAt = Date.now();
		this.status = 'success';
	}
}
