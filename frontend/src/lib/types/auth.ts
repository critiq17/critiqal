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
  email: string;
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

// 2FA
export interface TotpSetupResponse {
  qrCodeUri: string;
  secret: string;
  recoveryCodes: string[];
}

export interface ConfirmTotpRequest {
  code: string;
}

export interface TwoFactorStatusResponse {
  enabled: boolean;
}

// Email
export interface SetEmailRequest {
  email: string;
}

export interface VerifyEmailRequest {
  code: string;
}

// Recovery
export interface PasswordResetRequest {
  token: string;
  newPassword: string;
}

export interface RecoveryCodeRequest {
  username: string;
  recoveryCode: string;
}

export interface RecoveryCodesCountResponse {
  activeCount: number;
}

export interface RecoveryCodesRegenerateResponse {
  codes: string[];
  warning: string;
}
