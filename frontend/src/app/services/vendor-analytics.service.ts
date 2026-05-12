import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { AnalyticsSummary } from '../models/vendor.model';

@Injectable({ providedIn: 'root' })
export class VendorAnalyticsService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getSummary(from?: string, to?: string): Observable<AnalyticsSummary> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to) params = params.set('to', to);
    return this.http.get<AnalyticsSummary>(`${this.base}${API_ENDPOINTS.VENDOR.ANALYTICS_SUMMARY}`, { params });
  }
}

