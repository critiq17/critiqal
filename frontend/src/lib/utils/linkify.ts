// Splits text into plain-text and link tokens so the renderer can emit real
// anchors without ever using {@html}. Svelte escapes every token value, so this
// stays XSS-safe by construction.

export interface TextToken {
	type: 'text';
	value: string;
}

export interface LinkToken {
	type: 'link';
	value: string;
	href: string;
}

export type ContentToken = TextToken | LinkToken;

// http(s):// or www.-prefixed URLs. Stops at whitespace or an angle bracket.
const URL_RE = /(?:https?:\/\/|www\.)[^\s<>]+/gi;

// Punctuation that commonly trails a URL in prose but is not part of it.
const TRAILING_RE = /[.,!?;:'")\]}>]+$/;

export function linkify(text: string): ContentToken[] {
	const tokens: ContentToken[] = [];
	let cursor = 0;

	for (const match of text.matchAll(URL_RE)) {
		const start = match.index ?? 0;
		const url = match[0].replace(TRAILING_RE, '');
		if (!url) {
			continue;
		}

		if (start > cursor) {
			tokens.push({ type: 'text', value: text.slice(cursor, start) });
		}
		tokens.push({
			type: 'link',
			value: url,
			href: url.startsWith('www.') ? `https://${url}` : url
		});
		cursor = start + url.length;
	}

	if (cursor < text.length) {
		tokens.push({ type: 'text', value: text.slice(cursor) });
	}
	return tokens;
}
