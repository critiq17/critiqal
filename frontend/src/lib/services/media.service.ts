import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';

export interface AvatarUploadResult {
	avatarUrl: string;
}

export interface PostPhotoUploadResult {
	photoUrl: string;
	thumbnailUrl: string;
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

	uploadPostPhoto(postId: number, file: File): Promise<PostPhotoUploadResult> {
		const formData = new FormData();
		formData.append('file', file);
		return apiClient.upload<PostPhotoUploadResult>(API.media.postPhoto(postId), formData, true);
	},

	deletePostPhoto(postId: number): Promise<void> {
		return apiClient.delete<void>(API.media.postPhoto(postId), true);
	}
};
