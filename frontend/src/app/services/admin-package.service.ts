import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { PackageStatus, TravelPackage } from '../models/package.model';

@Injectable({ providedIn: 'root' })
export class AdminPackageService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getAllPackages(): Observable<TravelPackage[]> {
    return this.http.get<TravelPackage[]>(`${this.base}${API_ENDPOINTS.ADMIN_PACKAGE.ALL}`);
  }

  getPackage(id: string): Observable<TravelPackage> {
    return this.http.get<TravelPackage>(`${this.base}${API_ENDPOINTS.ADMIN_PACKAGE.BY_ID(id)}`);
  }

  createPackage(pkg: TravelPackage): Observable<TravelPackage> {
    return this.http.post<TravelPackage>(`${this.base}${API_ENDPOINTS.ADMIN_PACKAGE.ALL}`, pkg);
  }

  updatePackage(id: string, pkg: TravelPackage): Observable<TravelPackage> {
    return this.http.put<TravelPackage>(`${this.base}${API_ENDPOINTS.ADMIN_PACKAGE.BY_ID(id)}`, pkg);
  }

  deletePackage(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}${API_ENDPOINTS.ADMIN_PACKAGE.BY_ID(id)}`);
  }

  setStatus(id: string, status: PackageStatus): Observable<TravelPackage> {
    return this.http.patch<TravelPackage>(`${this.base}${API_ENDPOINTS.ADMIN_PACKAGE.STATUS(id)}?status=${status}`, {});
  }

  uploadImage(file: File): Observable<{ url: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ url: string }>(`${this.base}${API_ENDPOINTS.ADMIN_PACKAGE.IMAGES}`, formData);
  }
}
