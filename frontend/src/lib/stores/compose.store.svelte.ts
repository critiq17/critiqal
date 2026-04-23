class ComposeStore {
	open = $state(false);

	openCompose(): void {
		this.open = true;
	}

	closeCompose(): void {
		this.open = false;
	}
}

export const composeStore = new ComposeStore();

export function openCompose(): void {
	composeStore.openCompose();
}

export function closeCompose(): void {
	composeStore.closeCompose();
}
