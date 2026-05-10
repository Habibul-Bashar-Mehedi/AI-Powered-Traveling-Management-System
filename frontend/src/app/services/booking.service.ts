import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { Booking, BookingRequest } from '../models/booking.model';

@Injectable({
  providedIn: 'root'
})
export class BookingService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /**
   * Create a new booking
   */
  createBooking(booking: Booking): Observable<Booking> {
    const url = `${this.baseUrl}${API_ENDPOINTS.BOOKING.CREATE}`;
    return this.http.post<Booking>(url, booking);
  }

  /**
   * Get all bookings
   */
  getAllBookings(): Observable<Booking[]> {
    const url = `${this.baseUrl}${API_ENDPOINTS.BOOKING.GET_ALL}`;
    return this.http.get<Booking[]>(url);
  }

  /**
   * Update booking
   */
  updateBooking(id: number, booking: Partial<Booking>): Observable<string> {
    const url = `${this.baseUrl}${API_ENDPOINTS.BOOKING.UPDATE(id)}`;
    return this.http.put(url, booking, { responseType: 'text' });
  }

  /**
   * Delete booking
   */
  deleteBooking(id: number): Observable<string> {
    const url = `${this.baseUrl}${API_ENDPOINTS.BOOKING.DELETE(id)}`;
    return this.http.delete(url, { responseType: 'text' });
  }
}
