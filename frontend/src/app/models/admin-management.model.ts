import { UserRole } from '../enums/user-role.enum';

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface AdminUser {
  id: string;
  username: string;
  email: string;
  role: UserRole;
  countryId: string | null;
  failedLoginAttempts: number;
  lockoutUntil: string | null;
  createdAt: string;
  updatedAt: string;
  vendorId?: string;
  vendorType?: string;
}

export interface AdminUserRequest {
  username: string;
  email: string;
  password?: string;
  role: UserRole;
  countryId?: string;
  vendorType?: string;
}

