import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { Banner } from '../models/banner.model';

@Injectable({ providedIn: 'root' })
export class AdminBannerService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAllBanners(): Observable<Banner[]> {
    return this.http.get<Banner[]>(`${this.base}${API_ENDPOINTS.ADMIN_BANNER.ALL}`);
  }

  createBanner(banner: Banner): Observable<Banner> {
    return this.http.post<Banner>(`${this.base}${API_ENDPOINTS.ADMIN_BANNER.ALL}`, banner);
  }

  updateBanner(id: string, banner: Banner): Observable<Banner> {
    return this.http.put<Banner>(`${this.base}${API_ENDPOINTS.ADMIN_BANNER.BY_ID(id)}`, banner);
  }

  deleteBanner(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}${API_ENDPOINTS.ADMIN_BANNER.BY_ID(id)}`);
  }

  setActive(id: string, active: boolean): Observable<Banner> {
    return this.http.patch<Banner>(
      `${this.base}${API_ENDPOINTS.ADMIN_BANNER.ACTIVE_TOGGLE(id)}?active=${active}`, {});
  }

  uploadBannerImage(file: File): Observable<{ url: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ url: string }>(`${this.base}${API_ENDPOINTS.ADMIN_BANNER.IMAGES}`, formData);
  }
}
