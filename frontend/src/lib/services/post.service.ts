import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';
import type {
	Post,
	CreatePostRequest,
	Comment,
	AddCommentRequest,
	ReactionsMap,
	ReactionType,
	PageResponse
} from '$lib/types';

export const postService = {
	getFeed(page = 0, size = 20): Promise<PageResponse<Post>> {
		return apiClient.get<PageResponse<Post>>(`${API.posts.feed}?page=${page}&size=${size}`);
	},

	search(query: string, page = 0, size = 20): Promise<PageResponse<Post>> {
		return apiClient.get<PageResponse<Post>>(
			`${API.posts.search}?q=${encodeURIComponent(query)}&page=${page}&size=${size}`
		);
	},

	getById(id: number): Promise<Post> {
		return apiClient.get<Post>(API.posts.byId(id));
	},

	create(req: CreatePostRequest): Promise<Post> {
		return apiClient.post<Post>(API.posts.feed, req, true);
	},

	delete(id: number): Promise<void> {
		return apiClient.delete(API.posts.byId(id), true);
	},

	// --- Comments ---

	getComments(postId: number): Promise<Comment[]> {
		return apiClient.get<Comment[]>(API.posts.comments(postId));
	},

	addComment(postId: number, req: AddCommentRequest): Promise<Comment> {
		return apiClient.post<Comment>(API.posts.comments(postId), req, true);
	},

	deleteComment(postId: number, commentId: number): Promise<void> {
		return apiClient.delete(API.posts.comment(postId, commentId), true);
	},

	getReplies(postId: number, commentId: number): Promise<Comment[]> {
		return apiClient.get<Comment[]>(API.posts.replies(postId, commentId));
	},

	addReply(postId: number, commentId: number, req: AddCommentRequest): Promise<Comment> {
		return apiClient.post<Comment>(API.posts.replies(postId, commentId), req, true);
	},

	// --- Reactions ---

	getReactions(postId: number): Promise<ReactionsMap> {
		return apiClient.get<ReactionsMap>(API.posts.reactions(postId));
	},

	getMyReaction(postId: number): Promise<ReactionType | undefined> {
		return apiClient.get<ReactionType | undefined>(`${API.posts.reactions(postId)}/mine`, true);
	},

	react(postId: number, type: ReactionType): Promise<void> {
		return apiClient.post<void>(API.posts.reactions(postId), { type }, true);
	},

	removeReaction(postId: number): Promise<void> {
		return apiClient.delete(API.posts.reactions(postId), true);
	}
};
