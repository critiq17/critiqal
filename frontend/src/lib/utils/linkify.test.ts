import { describe, it, expect } from 'vitest';
import { linkify } from './linkify';

describe('linkify', () => {
	it('returns a single text token when there is no URL', () => {
		expect(linkify('just some text')).toEqual([{ type: 'text', value: 'just some text' }]);
	});

	it('detects an https URL', () => {
		expect(linkify('see https://youtube.com')).toEqual([
			{ type: 'text', value: 'see ' },
			{ type: 'link', value: 'https://youtube.com', href: 'https://youtube.com' }
		]);
	});

	it('prefixes www. links with https', () => {
		const [token] = linkify('www.example.com');
		expect(token).toEqual({ type: 'link', value: 'www.example.com', href: 'https://www.example.com' });
	});

	it('strips trailing punctuation from the link but keeps it as text', () => {
		expect(linkify('go to https://a.com.')).toEqual([
			{ type: 'text', value: 'go to ' },
			{ type: 'link', value: 'https://a.com', href: 'https://a.com' },
			{ type: 'text', value: '.' }
		]);
	});

	it('handles multiple links and surrounding text', () => {
		const tokens = linkify('a https://x.com b http://y.io c');
		expect(tokens).toEqual([
			{ type: 'text', value: 'a ' },
			{ type: 'link', value: 'https://x.com', href: 'https://x.com' },
			{ type: 'text', value: ' b ' },
			{ type: 'link', value: 'http://y.io', href: 'http://y.io' },
			{ type: 'text', value: ' c' }
		]);
	});

	it('keeps query strings and paths intact', () => {
		const [token] = linkify('https://youtube.com/watch?v=abc123&t=5');
		expect(token).toEqual({
			type: 'link',
			value: 'https://youtube.com/watch?v=abc123&t=5',
			href: 'https://youtube.com/watch?v=abc123&t=5'
		});
	});
});
