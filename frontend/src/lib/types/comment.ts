import type { User } from './user';

export interface Comment {
  id: string;
  postId: string;
  author: User;
  content: string;
  likeCount: number;
  likedByMe: boolean;
  createdAt: string;
  parentId?: string;
  replies?: Comment[];
}

export interface AddCommentRequest {
  content: string;
}
