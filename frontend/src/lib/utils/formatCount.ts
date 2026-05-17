// Compact count formatter for social-style stats ("1.2K", "3.4M").
// Returns "—" for null so UI can show a placeholder while counts load.

export function formatCount(n: number | null | undefined): string {
  if (n === null || n === undefined) return '—';
  if (!Number.isFinite(n)) return '—';
  const abs = Math.abs(n);
  if (abs >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`;
  if (abs >= 1_000) return `${(n / 1_000).toFixed(1)}K`;
  return String(n);
}
