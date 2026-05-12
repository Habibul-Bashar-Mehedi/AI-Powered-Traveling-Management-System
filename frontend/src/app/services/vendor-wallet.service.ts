import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { WalletSummary, WalletTransaction, PayoutRequest } from '../models/vendor.model';

@Injectable({ providedIn: 'root' })
export class VendorWalletService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getWalletSummary(): Observable<WalletSummary> {
    return this.http.get<WalletSummary>(`${this.base}${API_ENDPOINTS.VENDOR.WALLET}`);
  }

  getTransactions(page = 0, size = 20): Observable<{ content: WalletTransaction[]; totalElements: number }> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<{ content: WalletTransaction[]; totalElements: number }>(
      `${this.base}${API_ENDPOINTS.VENDOR.WALLET_TRANSACTIONS}`, { params });
  }

  requestPayout(request: PayoutRequest): Observable<PayoutRequest> {
    return this.http.post<PayoutRequest>(`${this.base}${API_ENDPOINTS.VENDOR.WALLET_PAYOUT}`, request);
  }
}

