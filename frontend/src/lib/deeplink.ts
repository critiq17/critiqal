// Telegram Mini App deep links. A shared post or profile becomes a
// `https://t.me/<bot>?startapp=<payload>` link: tapping it anywhere in Telegram
// launches the mini app, and the boot handler (MobileLayout) reads the payload
// back from `start_param` and opens the post focus / profile.
//
// Payload encoding is intentionally tiny and separator-free so usernames (which
// may contain `_`) and UUIDs (which contain `-`) survive intact — the first
// char is the kind, the rest is the id. Both are inside the startapp-safe set
// [A-Za-z0-9_-].

const DEFAULT_SHARE_BASE = 'https://t.me/critiqal_test_bot';

function shareBase(): string {
  const base = import.meta.env.VITE_TG_SHARE_BASE;
  return typeof base === 'string' && base.length > 0 ? base : DEFAULT_SHARE_BASE;
}

export type DeepLinkTarget =
  | { kind: 'post'; id: string }
  | { kind: 'user'; username: string };

export function encodePostPayload(id: string): string {
  return `p${id}`;
}

export function encodeUserPayload(username: string): string {
  return `u${username}`;
}

// `https://t.me/<bot>?startapp=<payload>` — opens the mini app with start_param.
export function buildShareLink(payload: string): string {
  return `${shareBase()}?startapp=${encodeURIComponent(payload)}`;
}

export function buildPostShareLink(id: string): string {
  return buildShareLink(encodePostPayload(id));
}

export function buildUserShareLink(username: string): string {
  return buildShareLink(encodeUserPayload(username));
}

// Inverse of the encoders — used by the boot handler on `start_param`.
export function parseStartParam(param: string | null | undefined): DeepLinkTarget | null {
  if (!param) return null;
  const kind = param[0];
  const rest = param.slice(1);
  if (!rest) return null;
  if (kind === 'p') return { kind: 'post', id: rest };
  if (kind === 'u') return { kind: 'user', username: rest };
  return null;
}
