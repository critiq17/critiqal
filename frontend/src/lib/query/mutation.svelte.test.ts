import { describe, it, expect, vi } from 'vitest';
import { Mutation } from './mutation.svelte';

describe('Mutation', () => {
	it('starts not pending', () => {
		const m = new Mutation(async () => 'ok');
		expect(m.isPending).toBe(false);
		expect(m.error).toBeNull();
	});

	it('sets isPending during execution and clears it after', async () => {
		let resolveFn!: () => void;
		const fn = vi.fn(
			() =>
				new Promise<string>((resolve) => {
					resolveFn = () => resolve('done');
				})
		);
		const m = new Mutation(fn);

		const promise = m.mutate(undefined);
		expect(m.isPending).toBe(true);

		resolveFn();
		await promise;
		expect(m.isPending).toBe(false);
	});

	it('returns the resolved value', async () => {
		const m = new Mutation(async (n: number) => n * 2);
		const result = await m.mutate(5);
		expect(result).toBe(10);
	});

	it('captures error and re-throws on failure', async () => {
		const m = new Mutation(async () => {
			throw new Error('fail');
		});

		await expect(m.mutate(undefined)).rejects.toThrow('fail');
		expect(m.error?.message).toBe('fail');
		expect(m.isPending).toBe(false);
	});

	it('wraps non-Error thrown values', async () => {
		const m = new Mutation(async () => {
			throw 'raw string';
		});

		await expect(m.mutate(undefined)).rejects.toBeDefined();
		expect(m.error).toBeInstanceOf(Error);
	});

	it('clears previous error on new mutation', async () => {
		const m = new Mutation(vi.fn().mockRejectedValueOnce(new Error('first')).mockResolvedValue('ok'));

		await m.mutate(undefined).catch(() => {});
		expect(m.error?.message).toBe('first');

		await m.mutate(undefined);
		expect(m.error).toBeNull();
	});
});
