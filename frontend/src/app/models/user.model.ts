import { UserRole } from '../enums/user-role.enum';

export interface User {
  id?: number;
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
