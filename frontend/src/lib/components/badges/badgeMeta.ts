import type { BadgeCode } from '$lib/types';

// Visual tier of a badge; drives ring color and glow accent.
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
  // Optional visual surface override for a specific badge.
  surface?: BadgeSurface;
}

export type BadgeGlyph =
  | 'numeral-i'
  | 'helmet'
  | 'centurion'
  | 'legatus'
  | 'scribe'
  | 'orator'
  | 'laurel'
  | 'eagle'
  | 'quill'
  | 'column'
  | 'scroll';
export type BadgeSurface = 'glass';

// Muted, antique-metal palette; reads as struck coinage, not neon.
export const TIER_STYLES: Record<BadgeTier, BadgeTierStyle> = {
  bronze: {
    accent: '#8c5a32',
    glow: 'rgba(140, 90, 50, 0.22)',
    gradient: 'linear-gradient(150deg, #b07a4a 0%, #6e4423 100%)',
    label: 'Bronze',
  },
  silver: {
    accent: '#8f99a4',
    glow: 'rgba(143, 153, 164, 0.22)',
    gradient: 'linear-gradient(150deg, #c2c9d1 0%, #828d99 100%)',
    label: 'Silver',
  },
  gold: {
    accent: '#b08a32',
    glow: 'rgba(176, 138, 50, 0.24)',
    gradient: 'linear-gradient(150deg, #d8b15e 0%, #9c7320 100%)',
    label: 'Gold',
  },
  legendary: {
    accent: '#6f5fa0',
    glow: 'rgba(111, 95, 160, 0.28)',
    gradient: 'linear-gradient(150deg, #8a79bd 0%, #463a68 100%)',
    label: 'Legendary',
  },
};

// Maps each backend BadgeCode to its visual treatment.
// Tenure ladder: CENTURION(bronze) -> GLADIATOR(silver) -> LEGATUS(gold); ORIGIN is legendary.
// Contribution: SCRIBE(bronze) -> ORATOR(gold) -> TRIBUNE(legendary).
export const BADGE_META: Record<BadgeCode, BadgeMeta> = {
  ORIGIN: { tier: 'legendary', glyph: 'numeral-i' },
  CENTURION: { tier: 'bronze', glyph: 'centurion' },
  GLADIATOR: { tier: 'silver', glyph: 'helmet', surface: 'glass' },
  LEGATUS: { tier: 'gold', glyph: 'legatus' },
  SCRIBE: { tier: 'bronze', glyph: 'scribe' },
  ORATOR: { tier: 'gold', glyph: 'orator' },
  TRIBUNE: { tier: 'legendary', glyph: 'scroll' },
};

export function badgeMeta(code: BadgeCode): BadgeMeta {
  return BADGE_META[code] ?? { tier: 'bronze', glyph: 'numeral-i' };
}

export function tierStyle(tier: BadgeTier): BadgeTierStyle {
  return TIER_STYLES[tier];
}
