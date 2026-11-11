export interface LoginCredentials {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  expiresAt: string;
}

export interface ApiErrorResponse {
  message?: string;
}
