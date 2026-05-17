import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';
import type { SetEmailRequest, VerifyEmailRequest } from '$lib/types';

export const emailVerificationService = {
  setEmail(req: SetEmailRequest): Promise<{ message: string }> {
    return apiClient.post<{ message: string }>(API.auth.setEmail, req);
  },

  verifyEmail(req: VerifyEmailRequest): Promise<{ message: string }> {
    return apiClient.post<{ message: string }>(API.auth.verifyEmail, req);
  },
};
