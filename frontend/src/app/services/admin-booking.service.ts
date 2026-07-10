import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { VendorBooking } from '../models/vendor.model';
import { VendorBookingStatus } from '../enums/vendor.enums';
import { UserBookingStatusSummary } from './vendor-booking.service';

@Injectable({ providedIn: 'root' })
export class AdminBookingService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAllBookings(status?: VendorBookingStatus): Observable<VendorBooking[]> {
    let params = new HttpParams();
    if (status) params = params.set('status', status);
    return this.http.get<VendorBooking[]>(`${this.base}${API_ENDPOINTS.ADMIN_BOOKING.ALL}`, { params });
  }

  getBookingStatusSummary(): Observable<UserBookingStatusSummary> {
    return this.http.get<UserBookingStatusSummary>(`${this.base}${API_ENDPOINTS.ADMIN_BOOKING.STATUS_SUMMARY}`);
  }
}
