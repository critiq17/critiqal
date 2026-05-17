import { describe, it, expect } from 'vitest';
import {
	TALLEST_RATIO,
	WIDEST_RATIO,
	DEFAULT_RATIO,
	clampFeedRatio,
	isFiniteRatio,
	toAspectRatioCss,
} from './aspect';

describe('clampFeedRatio', () => {
	it('returns the ratio unchanged when inside the band', () => {
		expect(clampFeedRatio(1)).toBe(1);
		expect(clampFeedRatio(1.5)).toBe(1.5);
	});

	it('clamps very tall portraits up to 4:5', () => {
		expect(clampFeedRatio(9 / 16)).toBe(TALLEST_RATIO);
		expect(clampFeedRatio(0.5)).toBe(TALLEST_RATIO);
	});

	it('clamps very wide landscapes down to 1.91:1', () => {
		expect(clampFeedRatio(3)).toBe(WIDEST_RATIO);
		expect(clampFeedRatio(2.5)).toBe(WIDEST_RATIO);
	});

	it('falls back to default for invalid inputs', () => {
		expect(clampFeedRatio(Number.NaN)).toBe(DEFAULT_RATIO);
		expect(clampFeedRatio(0)).toBe(DEFAULT_RATIO);
		expect(clampFeedRatio(-1)).toBe(DEFAULT_RATIO);
		expect(clampFeedRatio(Number.POSITIVE_INFINITY)).toBe(DEFAULT_RATIO);
	});

	it('preserves common social ratios within the band', () => {
		expect(clampFeedRatio(4 / 5)).toBe(4 / 5);
		expect(clampFeedRatio(1.91)).toBe(1.91);
	});
});

describe('isFiniteRatio', () => {
	it('rejects non-positive and non-finite values', () => {
		expect(isFiniteRatio(0)).toBe(false);
		expect(isFiniteRatio(-1)).toBe(false);
		expect(isFiniteRatio(Number.NaN)).toBe(false);
		expect(isFiniteRatio(Number.POSITIVE_INFINITY)).toBe(false);
	});

	it('accepts positive finite numbers', () => {
		expect(isFiniteRatio(0.5)).toBe(true);
		expect(isFiniteRatio(1.91)).toBe(true);
	});
});

describe('toAspectRatioCss', () => {
	it('returns a clamped numeric string with limited precision', () => {
		expect(toAspectRatioCss(1)).toBe('1.0000');
		expect(toAspectRatioCss(9 / 16)).toBe(TALLEST_RATIO.toFixed(4));
		expect(toAspectRatioCss(3)).toBe(WIDEST_RATIO.toFixed(4));
	});
});
