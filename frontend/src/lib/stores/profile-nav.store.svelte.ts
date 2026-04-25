class ProfileNavStore {
	username = $state<string | null>(null);

	open(u: string): void {
		this.username = u;
	}

	close(): void {
		this.username = null;
	}
}

export const profileNavStore = new ProfileNavStore();

export function openProfile(username: string): void {
	profileNavStore.open(username);
}

export function closeProfile(): void {
	profileNavStore.close();
}
