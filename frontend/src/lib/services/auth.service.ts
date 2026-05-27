import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';
import type {
  LoginResponse,
  LoginRequest,
  RegisterRequest,
  RegisterResponse,
  TwoFactorVerifyRequest,
  User,
} from '$lib/types';

export const authService = {
  register(req: RegisterRequest): Promise<RegisterResponse> {
    return apiClient.post<RegisterResponse>(API.auth.register, req);
  },

  login(req: LoginRequest): Promise<LoginResponse> {
    return apiClient.post<LoginResponse>(API.auth.login, req);
  },

  verifyTwoFactor(req: TwoFactorVerifyRequest): Promise<User> {
    return apiClient.post<User>(API.auth.login2fa, req);
  },

  verifyEmailLogin(req: TwoFactorVerifyRequest): Promise<User> {
    return apiClient.post<User>(API.auth.loginEmail, req);
  },
};
