import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { Market } from '../models/market.model';
import { NearbyQuery } from '../models/destination.model';
import { buildNearbyParams } from './destination.service';

@Injectable({ providedIn: 'root' })
export class MarketService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getAll(): Observable<Market[]> {
    return this.http.get<Market[]>(`${this.base}${API_ENDPOINTS.MARKET.GET_ALL}`);
  }

  getNearby(query: NearbyQuery): Observable<Market[]> {
    return this.http.get<Market[]>(`${this.base}${API_ENDPOINTS.MARKET.GET_ALL}`, {
      params: buildNearbyParams(query)
    });
  }
}
