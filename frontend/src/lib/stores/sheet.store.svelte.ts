class SheetStore {
	private openCount = $state(0);

	get anyOpen(): boolean {
		return this.openCount > 0;
	}

	open(): () => void {
		this.openCount++;
		return () => {
			this.openCount = Math.max(0, this.openCount - 1);
		};
	}
}

export const sheetStore = new SheetStore();

export function openSheet(): () => void {
	return sheetStore.open();
}
