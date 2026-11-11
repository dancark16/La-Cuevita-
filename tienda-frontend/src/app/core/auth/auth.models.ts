export interface LoginRequest {
  email: string;
  password: string;
}

export interface AccessTokenResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export type UserRole = 'ADMIN' | 'CUSTOMER';

export interface SessionUser {
  email: string;
  role: UserRole;
}
