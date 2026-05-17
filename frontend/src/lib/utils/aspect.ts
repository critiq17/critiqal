// Aspect ratio helpers for photo display.
// Strategy: clamp a photo's natural ratio into a feed-friendly band so the
// gallery container has a predictable shape and never crops the image (use
// object-fit: contain inside the container).

export const TALLEST_RATIO = 4 / 5; // 0.8  — Instagram-style portrait cap
export const WIDEST_RATIO = 1.91; //         — Facebook-link landscape cap
export const DEFAULT_RATIO = TALLEST_RATIO; // best for mobile feeds when unknown

export function isFiniteRatio(value: number): boolean {
  return Number.isFinite(value) && value > 0;
}

export function clampFeedRatio(ratio: number): number {
  if (!isFiniteRatio(ratio)) return DEFAULT_RATIO;
  if (ratio < TALLEST_RATIO) return TALLEST_RATIO;
  if (ratio > WIDEST_RATIO) return WIDEST_RATIO;
  return ratio;
}

export function naturalRatio(img: HTMLImageElement): number {
  const { naturalWidth: w, naturalHeight: h } = img;
  if (!w || !h) return DEFAULT_RATIO;
  return w / h;
}

// CSS `aspect-ratio` value, formatted with limited precision to avoid layout jitter.
export function toAspectRatioCss(ratio: number): string {
  return clampFeedRatio(ratio).toFixed(4);
}
