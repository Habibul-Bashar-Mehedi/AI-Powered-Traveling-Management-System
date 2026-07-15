import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { DestinationExpenseSummary } from '../models/user-expense.model';
import { BookingHistoryEntry } from '../models/booking-history.model';

@Injectable({ providedIn: 'root' })
export class UserExpenseService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getSummary(sort?: 'totalSpent' | 'latest'): Observable<DestinationExpenseSummary[]> {
    const url = sort
      ? `${this.base}${API_ENDPOINTS.USER_EXPENSES.SUMMARY}?sort=${sort}`
      : `${this.base}${API_ENDPOINTS.USER_EXPENSES.SUMMARY}`;
    return this.http.get<DestinationExpenseSummary[]>(url);
  }

  getDrilldown(destinationId: number | null): Observable<BookingHistoryEntry[]> {
    const key = destinationId === null ? 'other' : String(destinationId);
    return this.http.get<BookingHistoryEntry[]>(`${this.base}${API_ENDPOINTS.USER_EXPENSES.DRILLDOWN(key)}`);
  }
}
