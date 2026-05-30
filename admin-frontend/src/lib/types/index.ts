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

export interface AdminUser {
	id: string;
	username: string;
	name: string | null;
	avatarUrl: string | null;
	badges: UserBadge[];
}

export interface AdminBadge {
	code: BadgeCode;
	name: string;
	description: string;
	iconUrl: string | null;
}

export interface AdminMe {
	admin: boolean;
}

export interface AdminGrantResult {
	granted: string;
	user: string;
}

export interface AdminRevokeResult {
	revoked: string;
	user: string;
	removed: boolean;
}

export interface PageResponse<T> {
	content: T[];
	page: number;
	size: number;
	total: number;
	hasNext: boolean;
}

export interface Post {
	id: string;
	author: {
		id: string;
		username: string;
		name: string | null;
		avatarUrl: string | null;
	};
	content: string;
	viewCount: number;
	likeCount: number;
	status: string;
	createdAt: string;
}

export interface TwoFactorChallenge {
	challengeToken: string;
	method: 'TOTP';
}

export class ApiError extends Error {
	readonly status: number;

	constructor(status: number, message: string) {
		super(message);
		this.name = 'ApiError';
		this.status = status;
	}

	get isUnauthorized(): boolean {
		return this.status === 401;
	}

	get isForbidden(): boolean {
		return this.status === 403;
	}

	get isNotFound(): boolean {
		return this.status === 404;
	}
}
