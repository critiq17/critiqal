export interface User {
	id: string;
	username: string;
	name: string | null;
	bio: string | null;
	avatarUrl: string | null;
	email: string | null;
	emailVerified: boolean;
	twoFactorEnabled: boolean;
	createdAt: string;
}

export interface UpdateProfileRequest {
	name?: string;
	bio?: string;
	avatarUrl?: string;
}
