export class Mutation<TArgs, T> {
	isPending = $state(false);
	error = $state<Error | null>(null);

	constructor(private fn: (args: TArgs) => Promise<T>) {}

	async mutate(args: TArgs): Promise<T> {
		this.isPending = true;
		this.error = null;
		try {
			return await this.fn(args);
		} catch (err) {
			this.error = err instanceof Error ? err : new Error(String(err));
			throw err;
		} finally {
			this.isPending = false;
		}
	}
}
