import type { BadgeCode } from '$lib/types';

// Visual tier of a badge — drives ring color and glow accent.
export type BadgeTier = 'bronze' | 'silver' | 'gold' | 'legendary';

export interface BadgeTierStyle {
  // CSS color for the medallion ring / accent.
  accent: string;
  // Soft glow color (rgba) used behind the medallion.
  glow: string;
  // Gradient for the medallion face.
  gradient: string;
  // Human label for the tier, shown in the detail view.
  label: string;
}

export interface BadgeMeta {
  tier: BadgeTier;
  // Glyph key rendered by BadgeMedallion.
  glyph: BadgeGlyph;
}

export type BadgeGlyph =
  | 'star'
  | 'laurel'
  | 'swords'
  | 'eagle'
  | 'quill'
  | 'column'
  | 'scroll';

export const TIER_STYLES: Record<BadgeTier, BadgeTierStyle> = {
  bronze: {
    accent: '#c87f43',
    glow: 'rgba(200, 127, 67, 0.35)',
    gradient: 'linear-gradient(145deg, #e7a86b 0%, #a85f2e 100%)',
    label: 'Bronze'
  },
  silver: {
    accent: '#b9c2cc',
    glow: 'rgba(185, 194, 204, 0.38)',
    gradient: 'linear-gradient(145deg, #eef2f6 0%, #9aa6b2 100%)',
    label: 'Silver'
  },
  gold: {
    accent: '#f2c14e',
    glow: 'rgba(242, 193, 78, 0.42)',
    gradient: 'linear-gradient(145deg, #ffe08a 0%, #d39a26 100%)',
    label: 'Gold'
  },
  legendary: {
    accent: '#9b8cff',
    glow: 'rgba(155, 140, 255, 0.45)',
    gradient: 'linear-gradient(145deg, #b8a6ff 0%, #5ad1e6 100%)',
    label: 'Legendary'
  }
};

// Maps each backend BadgeCode to its visual treatment.
// Tenure ladder: CENTURION(bronze) -> GLADIATOR(silver) -> LEGATUS(gold); ORIGIN is legendary.
// Contribution: SCRIBE(bronze) -> TRIBUNE(silver) -> ORATOR(gold).
export const BADGE_META: Record<BadgeCode, BadgeMeta> = {
  ORIGIN: { tier: 'legendary', glyph: 'star' },
  CENTURION: { tier: 'bronze', glyph: 'laurel' },
  GLADIATOR: { tier: 'silver', glyph: 'swords' },
  LEGATUS: { tier: 'gold', glyph: 'eagle' },
  SCRIBE: { tier: 'bronze', glyph: 'quill' },
  ORATOR: { tier: 'gold', glyph: 'column' },
  TRIBUNE: { tier: 'silver', glyph: 'scroll' }
};

export function badgeMeta(code: BadgeCode): BadgeMeta {
  return BADGE_META[code] ?? { tier: 'bronze', glyph: 'star' };
}

export function tierStyle(tier: BadgeTier): BadgeTierStyle {
  return TIER_STYLES[tier];
}
