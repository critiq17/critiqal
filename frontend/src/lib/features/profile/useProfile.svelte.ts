import { userService } from '$lib/services/user.service';
import { mediaService } from '$lib/services/media.service';
import { authStore } from '$lib/stores/auth.store.svelte';
import type { User, Post } from '$lib/types';

export class UseProfile {
	profile = $state<User | null>(null);
	posts = $state<Post[]>([]);
	followersList = $state<User[]>([]);
	followingList = $state<User[]>([]);

	isLoading = $state(true);
	profileError = $state<string | null>(null);
	listsLoading = $state(false);

	isEditing = $state(false);
	editName = $state('');
	editBio = $state('');
	isSaving = $state(false);
	saveError = $state<string | null>(null);

	isUploadingAvatar = $state(false);
	avatarError = $state<string | null>(null);

	get followersCount(): number {
		return this.followersList.length;
	}

	get followingCount(): number {
		return this.followingList.length;
	}

	async load(): Promise<void> {
		const username = authStore.user?.username;
		if (!username) {
			this.isLoading = false;
			return;
		}

		this.isLoading = true;
		this.profileError = null;

		try {
			const [user, postsPage] = await Promise.all([
				userService.getProfile(username),
				userService.getUserPosts(username)
			]);
			this.profile = user;
			this.posts = postsPage.content;
			this.loadFollowLists(user.id);
		} catch (err: unknown) {
			this.profileError = err instanceof Error ? err.message : 'Failed to load profile';
		} finally {
			this.isLoading = false;
		}
	}

	async loadFollowLists(userId: number): Promise<void> {
		this.listsLoading = true;
		try {
			const [followers, following] = await Promise.all([
				userService.getFollowers(userId),
				userService.getFollowing(userId)
			]);
			this.followersList = followers;
			this.followingList = following;
		} catch {
			// non-critical — counts stay at 0
		} finally {
			this.listsLoading = false;
		}
	}

	startEdit(): void {
		if (!this.profile) return;
		this.editName = this.profile.name ?? '';
		this.editBio = this.profile.bio ?? '';
		this.isEditing = true;
		this.saveError = null;
	}

	cancelEdit(): void {
		this.isEditing = false;
		this.saveError = null;
	}

	async saveEdit(): Promise<void> {
		this.isSaving = true;
		this.saveError = null;
		try {
			const updated = await userService.updateProfile({
				name: this.editName.trim() || undefined,
				bio: this.editBio.trim() || undefined,
				avatarUrl: this.profile?.avatarUrl ?? undefined
			});
			this.profile = updated;
			authStore.updateUser(updated);
			this.isEditing = false;
		} catch (err: unknown) {
			this.saveError = err instanceof Error ? err.message : 'Failed to save changes';
		} finally {
			this.isSaving = false;
		}
	}

	async uploadAvatar(file: File): Promise<void> {
		if (!this.profile) return;
		this.isUploadingAvatar = true;
		this.avatarError = null;
		try {
			const result = await mediaService.uploadAvatar(file);
			this.profile = { ...this.profile, avatarUrl: result.avatarUrl };
			authStore.updateUser({ ...authStore.user!, avatarUrl: result.avatarUrl });
		} catch (err: unknown) {
			this.avatarError = err instanceof Error ? err.message : 'Upload failed';
		} finally {
			this.isUploadingAvatar = false;
		}
	}
}
