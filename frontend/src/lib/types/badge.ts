export type BadgeCode =
	| 'ORIGIN'
	| 'CENTURION'
	| 'GLADIATOR'
	| 'LEGATUS'
	| 'SCRIBE'
	| 'ORATOR'
	| 'TRIBUNE';

export interface UserBadge {
	id: string;
	code: BadgeCode;
	name: string;
	description: string;
	iconUrl: string | null;
	metadata: Record<string, unknown> | null;
	awardedAt: string;
}
