import { adminService } from '$lib/services/admin.service';

export type AdminStatus = 'unknown' | 'authed' | 'anon';

let status = $state<AdminStatus>('unknown');

// Independent session cookie; never touches normal user auth state.
export const adminAuthStore = {
	get status(): AdminStatus {
		return status;
	},
	async load(): Promise<AdminStatus> {
		try {
			await adminService.me();
			status = 'authed';
		} catch {
			status = 'anon';
		}
		return status;
	},
	setAuthed(): void {
		status = 'authed';
	},
	async logout(): Promise<void> {
		try {
			await adminService.logout();
		} catch {
			// Service failure is non-fatal — clear local state regardless.
		}
		status = 'anon';
	},
	reset(): void {
		status = 'unknown';
	},
};
