class SettingsNavStore {
	open = $state(false);

	show(): void {
		this.open = true;
	}

	hide(): void {
		this.open = false;
	}
}

export const settingsNavStore = new SettingsNavStore();

export function openSettings(): void {
	settingsNavStore.show();
}

export function closeSettings(): void {
	settingsNavStore.hide();
}
