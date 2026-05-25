export interface AuthSession {
  id: string;
  platform: string | null;
  browser: string | null;
  countryCode: string | null;
  countryName: string | null;
  city: string | null;
  createdAt: string;
  lastSeenAt: string;
  current: boolean;
}
