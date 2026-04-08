export type ReactionType = 'GIGACHAD' | 'THE_ROCK' | 'DAVID';

export type ReactionsMap = Record<ReactionType, number>;

export interface ReactionRequest {
	type: ReactionType;
}
