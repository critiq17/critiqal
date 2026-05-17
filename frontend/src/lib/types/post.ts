import type { User } from './user';

export type PostStatus = 'DRAFT' | 'PUBLISHED' | 'DELETED' | 'ARCHIVED';

export interface PostPhoto {
  id: string;
  url: string;
  thumbnailUrl: string;
  position: number;
}

export interface Post {
  id: string;
  author: User;
  content: string;
  photos: PostPhoto[];
  viewCount: number;
  likeCount: number;
  likedByMe: boolean;
  status: PostStatus;
  createdAt: string;
}

export interface CreatePostRequest {
  content: string;
}
