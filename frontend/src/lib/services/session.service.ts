import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';
import type { AuthSession } from '$lib/types';

export const sessionService = {
  list(): Promise<AuthSession[]> {
    return apiClient.get<AuthSession[]>(API.auth.sessions.list);
  },

  revoke(id: string): Promise<void> {
    return apiClient.delete<void>(API.auth.sessions.revoke(id));
  },
};
