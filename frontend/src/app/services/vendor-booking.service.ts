import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { VendorBooking } from '../models/vendor.model';
import { VendorBookingStatus } from '../enums/vendor.enums';

@Injectable({ providedIn: 'root' })
export class VendorBookingService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getBookings(status?: VendorBookingStatus): Observable<VendorBooking[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<VendorBooking[]>(`${this.base}${API_ENDPOINTS.VENDOR.BOOKINGS}`, { params });
  }

  getBookingDetail(id: string): Observable<VendorBooking> {
    return this.http.get<VendorBooking>(`${this.base}${API_ENDPOINTS.VENDOR.BOOKING_BY_ID(id)}`);
  }

  confirmBooking(id: string): Observable<VendorBooking> {
    return this.http.post<VendorBooking>(`${this.base}${API_ENDPOINTS.VENDOR.BOOKING_CONFIRM(id)}`, {});
  }

  rejectBooking(id: string, reason: string): Observable<VendorBooking> {
    return this.http.post<VendorBooking>(`${this.base}${API_ENDPOINTS.VENDOR.BOOKING_REJECT(id)}`, { reason });
  }

  cancelBooking(id: string, reason: string): Observable<VendorBooking> {
    return this.http.post<VendorBooking>(`${this.base}${API_ENDPOINTS.VENDOR.BOOKING_CANCEL(id)}`, { reason });
  }
}

