import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';
import type { User, UpdateProfileRequest, Post, PageResponse } from '$lib/types';

export const userService = {
  getProfile(username: string): Promise<User> {
    return apiClient.get<User>(API.users.profile(username));
  },

  search(query: string): Promise<User[]> {
    return apiClient.get<User[]>(`${API.users.search}?q=${encodeURIComponent(query)}`);
  },

  updateProfile(req: UpdateProfileRequest): Promise<User> {
    return apiClient.put<User>(API.users.me, req);
  },

  follow(targetId: string): Promise<void> {
    return apiClient.post<void>(API.users.follow(targetId), {});
  },

  unfollow(targetId: string): Promise<void> {
    return apiClient.delete(API.users.follow(targetId));
  },

  getFollowers(userId: string): Promise<User[]> {
    return apiClient.get<User[]>(API.users.followers(userId));
  },

  getFollowing(userId: string): Promise<User[]> {
    return apiClient.get<User[]>(API.users.following(userId));
  },

  getUserPosts(username: string, page = 0, size = 20): Promise<PageResponse<Post>> {
    return apiClient.get<PageResponse<Post>>(
      `${API.users.posts(username)}?page=${page}&size=${size}`
    );
  },
};
