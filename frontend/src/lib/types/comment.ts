import type { User } from './user';

export interface Comment {
	id: number;
	postId: number;
	author: User;
	content: string;
	createdAt: string;
}

export interface AddCommentRequest {
	content: string;
}
