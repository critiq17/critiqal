import { describe, it, expect } from 'vitest';
import { formatCount } from './formatCount';

describe('formatCount', () => {
	it('returns "—" for null / undefined / NaN', () => {
		expect(formatCount(null)).toBe('—');
		expect(formatCount(undefined)).toBe('—');
		expect(formatCount(Number.NaN)).toBe('—');
	});

	it('returns plain integers under 1000', () => {
		expect(formatCount(0)).toBe('0');
		expect(formatCount(999)).toBe('999');
	});

	it('uses K suffix between 1K and 1M', () => {
		expect(formatCount(1_000)).toBe('1.0K');
		expect(formatCount(12_345)).toBe('12.3K');
	});

	it('uses M suffix at and above 1M', () => {
		expect(formatCount(1_000_000)).toBe('1.0M');
		expect(formatCount(2_500_000)).toBe('2.5M');
	});
});
