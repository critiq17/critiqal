export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  total: number;
  hasNext: boolean;
}

export interface BanInfo {
  banned: true;
  reason: string;
  expiresAt: string; // ISO datetime string, or "" for permanent ban
}

export function isBanResponse(body: unknown): body is BanInfo {
  return (
    typeof body === 'object' &&
    body !== null &&
    (body as Record<string, unknown>).banned === true &&
    typeof (body as Record<string, unknown>).reason === 'string'
  );
}

export class ApiError extends Error {
  readonly status: number;
  readonly body?: Record<string, unknown>;

  constructor(status: number, message: string, body?: Record<string, unknown>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.body = body;
  }

  get isUnauthorized(): boolean {
    return this.status === 401;
  }

  get isForbidden(): boolean {
    return this.status === 403;
  }

  get isNotFound(): boolean {
    return this.status === 404;
  }
}
