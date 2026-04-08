import { apiClient } from '$lib/api/client';
import { API } from '$lib/api/endpoints';
import type { AuthResponse, LoginRequest, RegisterRequest } from '$lib/types';

export const authService = {
	register(req: RegisterRequest): Promise<AuthResponse> {
		return apiClient.post<AuthResponse>(API.auth.register, req);
	},

	login(req: LoginRequest): Promise<AuthResponse> {
		return apiClient.post<AuthResponse>(API.auth.login, req);
	}
};
