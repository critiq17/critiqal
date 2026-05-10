import type { User } from './user';

export interface TwoFactorChallenge {
	challengeToken: string;
	method: 'TOTP' | string;
}

export type LoginResponse = User | TwoFactorChallenge;
export type RegisterResponse = User;

export interface LoginRequest {
	username: string;
	password: string;
}

export interface RegisterRequest {
	username: string;
	password: string;
}

export interface TwoFactorVerifyRequest {
	challengeToken: string;
	code: string;
}

export function isTwoFactorChallenge(value: LoginResponse): value is TwoFactorChallenge {
	return (
		typeof value === 'object' &&
		value !== null &&
		'challengeToken' in value &&
		typeof value.challengeToken === 'string'
	);
}
