import { adminService } from '$lib/services/admin.service';

export type AdminStatus = 'unknown' | 'authed' | 'anon';

let status = $state<AdminStatus>('unknown');

// Independent of the main authStore: admin auth is a separate cookie session and
// must never touch the normal user's login state.
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
    } finally {
      status = 'anon';
    }
  },
  reset(): void {
    status = 'unknown';
  },
};
