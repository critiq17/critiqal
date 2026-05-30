import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/svelte';

vi.mock('$lib/services/admin.service', () => ({
  adminService: {
    getUser: vi.fn(),
    grantBadge: vi.fn(),
    revokeBadge: vi.fn(),
  },
}));

import GrantBadgePanel from './GrantBadgePanel.svelte';
import { adminService } from '$lib/services/admin.service';
import type { AdminUser, AdminBadge } from '$lib/types';

const grantBadge = adminService.grantBadge as ReturnType<typeof vi.fn>;
const revokeBadge = adminService.revokeBadge as ReturnType<typeof vi.fn>;
const getUser = adminService.getUser as ReturnType<typeof vi.fn>;

const baseUser: AdminUser = {
  id: 'u1',
  username: 'alice',
  name: 'Alice',
  avatarUrl: null,
  badges: [],
};

const badges: AdminBadge[] = [
  { code: 'ORIGIN', name: 'Origin', description: 'First members', iconUrl: null },
  { code: 'SCRIBE', name: 'Scribe', description: 'First post', iconUrl: null },
];

describe('GrantBadgePanel grant flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('grants a badge, refetches the user, and emits success feedback', async () => {
    grantBadge.mockResolvedValue({ granted: 'ORIGIN', user: 'u1' });
    getUser.mockResolvedValue({
      ...baseUser,
      badges: [
        {
          id: 'ub1',
          code: 'ORIGIN',
          name: 'Origin',
          description: 'First members',
          iconUrl: null,
          metadata: null,
          awardedAt: '2026-01-01T00:00:00Z',
        },
      ],
    });

    const onfeedback = vi.fn();
    const onchanged = vi.fn();

    render(GrantBadgePanel, {
      props: { user: baseUser, badges, onclose: vi.fn(), onchanged, onfeedback },
    });

    // The Origin option starts as "Grant" since the user owns no badges.
    const buttons = screen.getAllByRole('button');
    const originGrant = buttons.find((b) => b.textContent?.includes('Origin'));
    expect(originGrant).toBeTruthy();

    await fireEvent.click(originGrant as HTMLElement);

    await waitFor(() => {
      expect(grantBadge).toHaveBeenCalledWith('u1', 'ORIGIN');
    });
    expect(getUser).toHaveBeenCalledWith('u1');
    expect(onfeedback).toHaveBeenCalledWith('Granted Origin', 'success');
    expect(onchanged).toHaveBeenCalled();
  });

  it('emits error feedback when grant fails', async () => {
    grantBadge.mockRejectedValue(new Error('boom'));
    const onfeedback = vi.fn();

    render(GrantBadgePanel, {
      props: { user: baseUser, badges, onclose: vi.fn(), onchanged: vi.fn(), onfeedback },
    });

    const buttons = screen.getAllByRole('button');
    const originGrant = buttons.find((b) => b.textContent?.includes('Origin'));
    await fireEvent.click(originGrant as HTMLElement);

    await waitFor(() => {
      expect(onfeedback).toHaveBeenCalledWith('Could not grant Origin', 'error');
    });
    expect(getUser).not.toHaveBeenCalled();
  });

  it('revokes a badge the user already owns', async () => {
    const owned: AdminUser = {
      ...baseUser,
      badges: [
        {
          id: 'ub1',
          code: 'ORIGIN',
          name: 'Origin',
          description: 'First members',
          iconUrl: null,
          metadata: null,
          awardedAt: '2026-01-01T00:00:00Z',
        },
      ],
    };
    revokeBadge.mockResolvedValue({ revoked: 'ORIGIN', user: 'u1', removed: true });
    getUser.mockResolvedValue({ ...baseUser, badges: [] });

    const onfeedback = vi.fn();
    render(GrantBadgePanel, {
      props: { user: owned, badges, onclose: vi.fn(), onchanged: vi.fn(), onfeedback },
    });

    const buttons = screen.getAllByRole('button');
    const originBtn = buttons.find((b) => b.textContent?.includes('Origin'));
    await fireEvent.click(originBtn as HTMLElement);

    await waitFor(() => {
      expect(revokeBadge).toHaveBeenCalledWith('u1', 'ORIGIN');
    });
    expect(onfeedback).toHaveBeenCalledWith('Revoked Origin', 'success');
  });
});
