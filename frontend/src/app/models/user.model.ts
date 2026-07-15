import { UserRole } from '../enums/user-role.enum';

export interface User {
  id?: string; // UUID from backend
  username: string;
  email: string;
  password?: string;
  role: UserRole;
  countryId?: string;
  version?: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  role: UserRole;
  countryId?: string;
}

export interface LoginResponse {
  message: string;
  token?: string;
}

export interface RegisterResponse {
  email: string;
  message: string;
}

export interface VerifyOtpRequest {
  email: string;
  otp: string;
}

export interface ResendOtpRequest {
  email: string;
}

export interface ResendOtpResponse {
  message: string;
}

export interface AuthResponse {
  user: {
    id: string;
    username: string;
    email: string;
    roles: string[];
    createdAt: string;
    lastLoginAt: string | null;
  };
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}
