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
		return apiClient.upload<AvatarUploadResult>(API.media.avatar, formData, true);
	},

	deleteAvatar(): Promise<void> {
		return apiClient.delete<void>(API.media.avatar, true);
	},

	uploadPostPhoto(postId: number, file: File): Promise<PostPhoto> {
		const formData = new FormData();
		formData.append('file', file);
		return apiClient.upload<PostPhoto>(API.media.postPhotos(postId), formData, true);
	},

	deletePostPhoto(postId: number, photoId: number): Promise<void> {
		return apiClient.delete<void>(API.media.postPhoto(postId, photoId), true);
	},

	deleteAllPostPhotos(postId: number): Promise<void> {
		return apiClient.delete<void>(API.media.postPhotos(postId), true);
	}
};
