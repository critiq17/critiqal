import { describe, it, expect } from 'vitest';
import { formatRelativeTime } from './formatRelativeTime';

describe('formatRelativeTime', () => {
	const now = new Date();

	it('returns "just now" for less than a minute', () => {
		const recent = new Date(now.getTime() - 30_000).toISOString();
		expect(formatRelativeTime(recent)).toBe('just now');
	});

	it('returns minutes for less than an hour', () => {
		const minutesAgo = new Date(now.getTime() - 5 * 60_000).toISOString();
		expect(formatRelativeTime(minutesAgo)).toBe('5m');
	});

	it('returns hours for less than a day', () => {
		const hoursAgo = new Date(now.getTime() - 3 * 60 * 60_000).toISOString();
		expect(formatRelativeTime(hoursAgo)).toBe('3h');
	});

	it('returns days for less than a week', () => {
		const daysAgo = new Date(now.getTime() - 2 * 24 * 60 * 60_000).toISOString();
		expect(formatRelativeTime(daysAgo)).toBe('2d');
	});

	it('returns formatted date for older dates', () => {
		const oldDate = new Date(now.getTime() - 10 * 24 * 60 * 60_000).toISOString();
		const result = formatRelativeTime(oldDate);
		expect(result).toMatch(/^\w+ \d+$/);
	});
});