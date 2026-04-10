import type { User } from './user';

export interface Comment {
	id: number;
	postId: number;
	author: User;
	content: string;
	createdAt: string;
	parentId?: number;
	replies?: Comment[];
}

export interface AddCommentRequest {
	content: string;
}
