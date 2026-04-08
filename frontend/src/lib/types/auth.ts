import type { User } from './user';

export interface AuthResponse {
	token: string;
	user: User;
}

export interface LoginRequest {
	username: string;
	password: string;
}

export interface RegisterRequest {
	username: string;
	password: string;
}
