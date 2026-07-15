import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { BookingHistoryEntry } from '../models/booking-history.model';
import { BookingSource } from '../enums/booking-source.enum';
import { Page } from './service-catalog.service';

export interface BookingHistoryQuery {
  type?: BookingSource;
  status?: string;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class BookingHistoryService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getHistory(query: BookingHistoryQuery = {}): Observable<Page<BookingHistoryEntry>> {
    let params = new HttpParams()
      .set('page', query.page ?? 0)
      .set('size', query.size ?? 10);
    if (query.type) params = params.set('type', query.type);
    if (query.status) params = params.set('status', query.status);

    return this.http.get<Page<BookingHistoryEntry>>(`${this.base}${API_ENDPOINTS.BOOKING_HISTORY.LIST}`, { params });
  }

  downloadReceipt(id: string): Observable<Blob> {
    return this.http.get(`${this.base}${API_ENDPOINTS.BOOKING_HISTORY.RECEIPT(id)}`, { responseType: 'blob' });
  }
}
