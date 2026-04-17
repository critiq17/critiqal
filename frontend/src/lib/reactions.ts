import type { ReactionType, ReactionsMap } from '$lib/types';

export interface ReactionVisual {
	assetPath?: string;
	fallbackEmoji: string;
	label: string;
}

export const REACTION_TYPES: ReactionType[] = ['GIGACHAD', 'THE_ROCK', 'DAVID'];

export const DEFAULT_REACTIONS: ReactionsMap = {
	GIGACHAD: 0,
	THE_ROCK: 0,
	DAVID: 0
};

export const REACTION_VISUALS: Record<ReactionType, ReactionVisual> = {
	GIGACHAD: {
		assetPath: '/assets/reactions/GIGACHAD.png',
		fallbackEmoji: '💪',
		label: 'GIGACHAD'
	},
	THE_ROCK: {
		assetPath: '/assets/reactions/THEROCK.png',
		fallbackEmoji: '🪨',
		label: 'THE ROCK'
	},
	DAVID: {
		fallbackEmoji: '🗿',
		label: 'DAVID'
	}
};
