import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { AdminUser, AdminUserRequest, PageResponse } from '../models/admin-management.model';
import { UserRole } from '../enums/user-role.enum';

@Injectable({ providedIn: 'root' })
export class AdminManagementService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getUsers(page: number, size: number, search?: string, role?: UserRole | ''): Observable<PageResponse<AdminUser>> {
    let params = new HttpParams().set('page', page).set('size', size);

    if (search?.trim()) {
      params = params.set('search', search.trim());
    }

    if (role) {
      params = params.set('role', role);
    }

    return this.http.get<PageResponse<AdminUser>>(
      `${this.baseUrl}${API_ENDPOINTS.ADMIN_MANAGEMENT.USERS}`,
      { params }
    );
  }

  createUser(payload: AdminUserRequest): Observable<AdminUser> {
    return this.http.post<AdminUser>(`${this.baseUrl}${API_ENDPOINTS.ADMIN_MANAGEMENT.USERS}`, payload);
  }

  updateUser(userId: string, payload: AdminUserRequest): Observable<AdminUser> {
    return this.http.put<AdminUser>(`${this.baseUrl}${API_ENDPOINTS.ADMIN_MANAGEMENT.USER_BY_ID(userId)}`, payload);
  }

  deleteUser(userId: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}${API_ENDPOINTS.ADMIN_MANAGEMENT.USER_BY_ID(userId)}`);
  }
}
