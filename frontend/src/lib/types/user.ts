export interface User {
	id: number;
	username: string;
	name: string | null;
	bio: string | null;
	avatarUrl: string | null;
	createdAt: string;
}

export interface UpdateProfileRequest {
	name?: string;
	bio?: string;
	avatarUrl?: string;
}
