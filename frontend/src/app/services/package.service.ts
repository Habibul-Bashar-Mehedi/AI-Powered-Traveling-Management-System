import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { PackageBookRequest, PackageBooking, TravelPackage } from '../models/package.model';
import { Page } from './service-catalog.service';

@Injectable({ providedIn: 'root' })
export class PackageService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getPublishedPackages(page = 0, size = 20): Observable<Page<TravelPackage>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<TravelPackage>>(`${this.base}${API_ENDPOINTS.PACKAGE_CATALOG.LIST}`, { params });
  }

  getPackage(id: string): Observable<TravelPackage> {
    return this.http.get<TravelPackage>(`${this.base}${API_ENDPOINTS.PACKAGE_CATALOG.BY_ID(id)}`);
  }

  bookPackage(id: string, request: PackageBookRequest): Observable<PackageBooking> {
    return this.http.post<PackageBooking>(`${this.base}${API_ENDPOINTS.PACKAGE_CATALOG.BOOK(id)}`, request);
  }
}
