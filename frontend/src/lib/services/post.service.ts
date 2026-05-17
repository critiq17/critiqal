import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';
import type {
  Post,
  CreatePostRequest,
  Comment,
  AddCommentRequest,
  LikeResponse,
  PageResponse,
} from '$lib/types';

export const postService = {
  getFeed(page = 0, size = 20): Promise<PageResponse<Post>> {
    return apiClient.get<PageResponse<Post>>(`${API.posts.feed}?page=${page}&size=${size}`);
  },

  getFollowingFeed(page = 0, size = 20): Promise<PageResponse<Post>> {
    return apiClient.get<PageResponse<Post>>(
      `${API.users.followingFeed}?page=${page}&size=${size}`
    );
  },

  search(query: string, page = 0, size = 20): Promise<PageResponse<Post>> {
    return apiClient.get<PageResponse<Post>>(
      `${API.posts.search}?q=${encodeURIComponent(query)}&page=${page}&size=${size}`
    );
  },

  getById(id: string): Promise<Post> {
    return apiClient.get<Post>(API.posts.byId(id));
  },

  create(req: CreatePostRequest): Promise<Post> {
    return apiClient.post<Post>(API.posts.feed, req);
  },

  delete(id: string): Promise<void> {
    return apiClient.delete(API.posts.byId(id));
  },

  // --- Comments ---

  getComments(postId: string): Promise<Comment[]> {
    return apiClient.get<Comment[]>(API.posts.comments(postId));
  },

  addComment(postId: string, req: AddCommentRequest): Promise<Comment> {
    return apiClient.post<Comment>(API.posts.comments(postId), req);
  },

  deleteComment(postId: string, commentId: string): Promise<void> {
    return apiClient.delete(API.posts.comment(postId, commentId));
  },

  getReplies(postId: string, commentId: string): Promise<Comment[]> {
    return apiClient.get<Comment[]>(API.posts.replies(postId, commentId));
  },

  addReply(postId: string, commentId: string, req: AddCommentRequest): Promise<Comment> {
    return apiClient.post<Comment>(API.posts.replies(postId, commentId), req);
  },

  // --- Likes ---

  toggleLike(postId: string): Promise<LikeResponse> {
    return apiClient.post<LikeResponse>(API.posts.likes(postId), {});
  },

  toggleCommentLike(postId: string, commentId: string): Promise<LikeResponse> {
    return apiClient.post<LikeResponse>(API.posts.commentLikes(postId, commentId), {});
  },
};
