import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { VendorProfile, VendorServiceListing } from '../models/vendor.model';

@Injectable({ providedIn: 'root' })
export class VendorService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  register(profile: VendorProfile): Observable<VendorProfile> {
    return this.http.post<VendorProfile>(`${this.base}${API_ENDPOINTS.VENDOR.REGISTER}`, profile);
  }

  getProfile(): Observable<VendorProfile> {
    return this.http.get<VendorProfile>(`${this.base}${API_ENDPOINTS.VENDOR.PROFILE}`);
  }

  updateProfile(profile: VendorProfile): Observable<VendorProfile> {
    return this.http.put<VendorProfile>(`${this.base}${API_ENDPOINTS.VENDOR.PROFILE}`, profile);
  }

  getServices(): Observable<VendorServiceListing[]> {
    return this.http.get<VendorServiceListing[]>(`${this.base}${API_ENDPOINTS.VENDOR.SERVICES}`);
  }

  createService(service: VendorServiceListing): Observable<VendorServiceListing> {
    return this.http.post<VendorServiceListing>(`${this.base}${API_ENDPOINTS.VENDOR.SERVICES}`, service);
  }

  updateService(id: string, service: VendorServiceListing): Observable<VendorServiceListing> {
    return this.http.put<VendorServiceListing>(`${this.base}${API_ENDPOINTS.VENDOR.SERVICE_BY_ID(id)}`, service);
  }

  deleteService(id: string): Observable<void> {
    return this.http.delete<void>(`${this.base}${API_ENDPOINTS.VENDOR.SERVICE_BY_ID(id)}`);
  }

  toggleServiceStatus(id: string, status: string): Observable<VendorServiceListing> {
    return this.http.patch<VendorServiceListing>(
      `${this.base}${API_ENDPOINTS.VENDOR.SERVICE_STATUS(id)}?status=${status}`, {});
  }

  uploadServiceImage(file: File): Observable<{ url: string }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<{ url: string }>(`${this.base}${API_ENDPOINTS.VENDOR.SERVICE_IMAGES}`, formData);
  }
}

