import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { VendorProfile, PayoutRequest } from '../models/vendor.model';

export interface AdminVendorAction {
  reason?: string;
  action?: string;
}

@Injectable({ providedIn: 'root' })
export class AdminVendorService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getAllVendors(): Observable<VendorProfile[]> {
    return this.http.get<VendorProfile[]>(`${this.base}${API_ENDPOINTS.ADMIN_VENDOR.ALL}`);
  }

  getPendingVendors(): Observable<VendorProfile[]> {
    return this.http.get<VendorProfile[]>(`${this.base}${API_ENDPOINTS.ADMIN_VENDOR.PENDING}`);
  }

  approveVendor(id: string): Observable<VendorProfile> {
    return this.http.post<VendorProfile>(`${this.base}${API_ENDPOINTS.ADMIN_VENDOR.APPROVE(id)}`, {});
  }

  rejectVendor(id: string, reason: string): Observable<VendorProfile> {
    const body: AdminVendorAction = { reason, action: 'REJECT' };
    return this.http.post<VendorProfile>(`${this.base}${API_ENDPOINTS.ADMIN_VENDOR.REJECT(id)}`, body);
  }

  suspendVendor(id: string, reason: string): Observable<VendorProfile> {
    const body: AdminVendorAction = { reason, action: 'SUSPEND' };
    return this.http.post<VendorProfile>(`${this.base}${API_ENDPOINTS.ADMIN_VENDOR.SUSPEND(id)}`, body);
  }

  reinstateVendor(id: string): Observable<VendorProfile> {
    return this.http.post<VendorProfile>(`${this.base}${API_ENDPOINTS.ADMIN_VENDOR.REINSTATE(id)}`, {});
  }

  getPendingPayouts(): Observable<PayoutRequest[]> {
    return this.http.get<PayoutRequest[]>(`${this.base}${API_ENDPOINTS.ADMIN_VENDOR.PENDING_PAYOUTS}`);
  }

  processPayout(payoutId: string, approve: boolean, note?: string): Observable<PayoutRequest> {
    const body: AdminVendorAction = note ? { reason: note } : {};
    return this.http.post<PayoutRequest>(
      `${this.base}${API_ENDPOINTS.ADMIN_VENDOR.PROCESS_PAYOUT(payoutId)}?approve=${approve}`,
      body
    );
  }
}
