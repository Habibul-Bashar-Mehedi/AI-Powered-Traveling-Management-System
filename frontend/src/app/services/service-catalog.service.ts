import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { BookServiceRequest, PublicServiceListing, VendorBooking } from '../models/vendor.model';

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
}

@Injectable({ providedIn: 'root' })
export class ServiceCatalogService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getActiveServices(page = 0, size = 20): Observable<Page<PublicServiceListing>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<PublicServiceListing>>(`${this.base}${API_ENDPOINTS.SERVICE_CATALOG.LIST}`, { params });
  }

  bookService(serviceId: string, request: BookServiceRequest): Observable<VendorBooking> {
    return this.http.post<VendorBooking>(`${this.base}${API_ENDPOINTS.SERVICE_CATALOG.BOOK(serviceId)}`, request);
  }
}
