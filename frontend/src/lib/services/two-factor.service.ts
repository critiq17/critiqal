import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';
import type { TotpSetupResponse, ConfirmTotpRequest, TwoFactorStatusResponse } from '$lib/types';

export const twoFactorService = {
  setup(): Promise<TotpSetupResponse> {
    return apiClient.post<TotpSetupResponse>(API.auth.twoFactor.setup, {});
  },

  confirm(req: ConfirmTotpRequest): Promise<void> {
    return apiClient.post<void>(API.auth.twoFactor.confirm, req);
  },

  disable(req: ConfirmTotpRequest): Promise<void> {
    return apiClient.deleteWithBody<void>(API.auth.twoFactor.disable, req);
  },

  status(): Promise<TwoFactorStatusResponse> {
    return apiClient.get<TwoFactorStatusResponse>(API.auth.twoFactor.status);
  },
};
