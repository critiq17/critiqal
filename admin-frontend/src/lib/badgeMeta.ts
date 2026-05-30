import type { BadgeCode } from '$lib/types';

export type BadgeTier = 'bronze' | 'silver' | 'gold' | 'legendary';

export interface BadgeTierStyle {
	accent: string;
	glow: string;
	gradient: string;
	label: string;
}

export interface BadgeMeta {
	tier: BadgeTier;
}

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

export const BADGE_META: Record<BadgeCode, BadgeMeta> = {
	ORIGIN: { tier: 'legendary' },
	CENTURION: { tier: 'bronze' },
	GLADIATOR: { tier: 'silver' },
	LEGATUS: { tier: 'gold' },
	SCRIBE: { tier: 'bronze' },
	ORATOR: { tier: 'gold' },
	TRIBUNE: { tier: 'legendary' },
};

export function badgeMeta(code: BadgeCode): BadgeMeta {
	return BADGE_META[code] ?? { tier: 'bronze' };
}

export function tierStyle(tier: BadgeTier): BadgeTierStyle {
	return TIER_STYLES[tier];
}
