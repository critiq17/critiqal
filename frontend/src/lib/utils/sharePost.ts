import { getTelegramWebApp } from '$lib/telegram';
import { buildPostShareLink } from '$lib/deeplink';
import type { Post } from '$lib/types';

export interface ShareResult {
  /** true when the link was copied to the clipboard (caller can flash a toast). */
  copied: boolean;
}

function postUrl(post: Post): string {
  // A Telegram Mini App deep link: tapping it (from any chat, any client)
  // launches the app and the boot handler focuses this exact post.
  return buildPostShareLink(post.id);
}

function shareText(post: Post): string {
  const who = post.author.name?.trim() || `@${post.author.username}`;
  const snippet = post.content.trim().replace(/\s+/g, ' ').slice(0, 80);
  return snippet
    ? `${who} on Critiqal: “${snippet}${post.content.length > 80 ? '…' : ''}”`
    : `${who} on Critiqal`;
}

async function copyToClipboard(url: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(url);
    return true;
  } catch {
    return false;
  }
}

/**
 * Shares a post link. Prefers Telegram's native share sheet inside the
 * mini-app, then the Web Share API, falling back to clipboard.
 */
export async function sharePost(post: Post): Promise<ShareResult> {
  const url = postUrl(post);
  const text = shareText(post);

  const tg = getTelegramWebApp();
  if (tg) {
    const link = `https://t.me/share/url?url=${encodeURIComponent(url)}&text=${encodeURIComponent(text)}`;
    tg.openTelegramLink(link);
    return { copied: false };
  }

  if (typeof navigator !== 'undefined' && typeof navigator.share === 'function') {
    try {
      await navigator.share({ title: text, text, url });
      return { copied: false };
    } catch {
      // dismissed — fall through to clipboard
    }
  }

  return { copied: await copyToClipboard(url) };
}
