import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { Hotel } from '../models/hotel.model';

@Injectable({
  providedIn: 'root'
})
export class HotelService {
  private readonly baseUrl = environment.apiUrl;

  constructor(private http: HttpClient) {}

  /**
   * Create a new hotel
   */
  createHotel(hotel: Hotel): Observable<Hotel> {
    const url = `${this.baseUrl}${API_ENDPOINTS.HOTEL.CREATE}`;
    return this.http.post<Hotel>(url, hotel);
  }

  /**
   * Get all hotels
   */
  getAllHotels(): Observable<Hotel[]> {
    const url = `${this.baseUrl}${API_ENDPOINTS.HOTEL.GET_ALL}`;
    return this.http.get<Hotel[]>(url);
  }

  /**
   * Get hotel by ID
   */
  getHotelById(id: number): Observable<Hotel> {
    const url = `${this.baseUrl}${API_ENDPOINTS.HOTEL.GET_BY_ID(id)}`;
    return this.http.get<Hotel>(url);
  }

  /**
   * Update hotel
   */
  updateHotel(id: number, hotel: Partial<Hotel>): Observable<string> {
    const url = `${this.baseUrl}${API_ENDPOINTS.HOTEL.UPDATE(id)}`;
    return this.http.put(url, hotel, { responseType: 'text' });
  }

  /**
   * Delete hotel
   */
  deleteHotel(id: number): Observable<string> {
    const url = `${this.baseUrl}${API_ENDPOINTS.HOTEL.DELETE(id)}`;
    return this.http.delete(url, { responseType: 'text' });
  }
}
