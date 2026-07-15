import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import {
  PaymentInitiateRequest,
  PaymentInitiateResponse,
  PaymentSimulateRequest,
  PaymentSimulateResponse,
  PaymentStatusDTO
} from '../models/payment.model';

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  initiate(request: PaymentInitiateRequest): Observable<PaymentInitiateResponse> {
    return this.http.post<PaymentInitiateResponse>(`${this.base}${API_ENDPOINTS.PAYMENT.INITIATE}`, request);
  }

  getStatus(txId: string): Observable<PaymentStatusDTO> {
    return this.http.get<PaymentStatusDTO>(`${this.base}${API_ENDPOINTS.PAYMENT.STATUS(txId)}`);
  }

  simulate(request: PaymentSimulateRequest): Observable<PaymentSimulateResponse> {
    return this.http.post<PaymentSimulateResponse>(`${this.base}${API_ENDPOINTS.PAYMENT.SIMULATE}`, request);
  }
}
