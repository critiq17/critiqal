import { describe, it, expect, vi, beforeEach } from 'vitest';

vi.mock('$lib/services/admin.service', () => ({
	adminService: {
		me: vi.fn(),
		logout: vi.fn(),
	},
}));

import { adminAuthStore } from './admin-auth.store.svelte';
import { adminService } from '$lib/services/admin.service';

const me = adminService.me as ReturnType<typeof vi.fn>;
const logout = adminService.logout as ReturnType<typeof vi.fn>;

describe('adminAuthStore', () => {
	beforeEach(() => {
		vi.clearAllMocks();
		adminAuthStore.reset();
	});

	it('starts in the unknown state', () => {
		expect(adminAuthStore.status).toBe('unknown');
	});

	it('load() sets authed when me() resolves', async () => {
		me.mockResolvedValue({ admin: true });
		const status = await adminAuthStore.load();
		expect(status).toBe('authed');
		expect(adminAuthStore.status).toBe('authed');
	});

	it('load() sets anon when me() rejects', async () => {
		me.mockRejectedValue(new Error('404'));
		const status = await adminAuthStore.load();
		expect(status).toBe('anon');
		expect(adminAuthStore.status).toBe('anon');
	});

	it('setAuthed() flips status to authed', () => {
		adminAuthStore.setAuthed();
		expect(adminAuthStore.status).toBe('authed');
	});

	it('logout() calls the service and sets anon', async () => {
		adminAuthStore.setAuthed();
		logout.mockResolvedValue(undefined);
		await adminAuthStore.logout();
		expect(logout).toHaveBeenCalledOnce();
		expect(adminAuthStore.status).toBe('anon');
	});

	it('logout() sets anon even if the service throws', async () => {
		adminAuthStore.setAuthed();
		logout.mockRejectedValue(new Error('network'));
		await adminAuthStore.logout();
		expect(adminAuthStore.status).toBe('anon');
	});
});
