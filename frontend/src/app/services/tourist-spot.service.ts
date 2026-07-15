import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { TouristSpot } from '../models/tourist-spot.model';
import { NearbyQuery } from '../models/destination.model';
import { buildNearbyParams } from './destination.service';

@Injectable({ providedIn: 'root' })
export class TouristSpotService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getAll(): Observable<TouristSpot[]> {
    return this.http.get<TouristSpot[]>(`${this.base}${API_ENDPOINTS.TOURIST_SPOT.GET_ALL}`);
  }

  getNearby(query: NearbyQuery): Observable<TouristSpot[]> {
    return this.http.get<TouristSpot[]>(`${this.base}${API_ENDPOINTS.TOURIST_SPOT.GET_ALL}`, {
      params: buildNearbyParams(query)
    });
  }
}
