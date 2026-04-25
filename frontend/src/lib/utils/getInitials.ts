/**
 * Returns up to two uppercase initials from a display name, falling back to
 * a username when the display name is absent or blank.
 * Falls back to '?' when both inputs are empty.
 */
export function getInitials(name: string | null | undefined, fallback?: string): string {
  const source = (name?.trim() ? name : (fallback ?? '')).trim();
  if (!source) return '?';

  const parts = source.split(/\s+/);
  if (parts.length === 1) {
    return (parts[0]?.[0] ?? '?').toUpperCase();
  }

  const first = parts[0]?.[0] ?? '';
  const last = parts[parts.length - 1]?.[0] ?? '';
  return (first + last).toUpperCase() || '?';
}
