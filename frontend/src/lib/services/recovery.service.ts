import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';
import type {
  SetEmailRequest,
  PasswordResetRequest,
  RecoveryCodeRequest,
  RecoveryCodesCountResponse,
  RecoveryCodesRegenerateResponse,
} from '$lib/types';
import type { User } from '$lib/types';

export const recoveryService = {
  requestReset(req: SetEmailRequest): Promise<{ message: string }> {
    return apiClient.post<{ message: string }>(API.auth.recovery.request, req);
  },

  resetPassword(req: PasswordResetRequest): Promise<void> {
    return apiClient.post<void>(API.auth.recovery.reset, req);
  },

  useRecoveryCode(req: RecoveryCodeRequest): Promise<User> {
    return apiClient.post<User>(API.auth.recovery.useCode, req);
  },

  getCodesCount(): Promise<RecoveryCodesCountResponse> {
    return apiClient.get<RecoveryCodesCountResponse>(API.auth.recovery.codesCount);
  },

  regenerateCodes(): Promise<RecoveryCodesRegenerateResponse> {
    return apiClient.post<RecoveryCodesRegenerateResponse>(API.auth.recovery.regenerateCodes, {});
  },
};
