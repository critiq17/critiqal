import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';
import type { PostPhoto } from '$lib/types';

export interface AvatarUploadResult {
	avatarUrl: string;
}

export const mediaService = {
	uploadAvatar(file: File): Promise<AvatarUploadResult> {
		const formData = new FormData();
		formData.append('file', file);
		return apiClient.upload<AvatarUploadResult>(API.media.avatar, formData);
	},

	deleteAvatar(): Promise<void> {
		return apiClient.delete<void>(API.media.avatar);
	},

	uploadPostPhoto(postId: string, file: File): Promise<PostPhoto> {
		const formData = new FormData();
		formData.append('file', file);
		return apiClient.upload<PostPhoto>(API.media.postPhotos(postId), formData);
	},

	deletePostPhoto(postId: string, photoId: string): Promise<void> {
		return apiClient.delete<void>(API.media.postPhoto(postId, photoId));
	},

	deleteAllPostPhotos(postId: string): Promise<void> {
		return apiClient.delete<void>(API.media.postPhotos(postId));
	},
};
