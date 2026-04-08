import type { User } from './user';

export type PostStatus = 'DRAFT' | 'PUBLISHED' | 'DELETED' | 'ARCHIVED';

export interface Post {
	id: number;
	author: User;
	content: string;
	photoUrl: string | null;
	photoThumbnailUrl: string | null;
	viewCount: number;
	status: PostStatus;
	createdAt: string;
}

export interface CreatePostRequest {
	content: string;
	photoUrl?: string;
}
