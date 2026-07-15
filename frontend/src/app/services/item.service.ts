import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { TraditionalItem } from '../models/item.model';
import { NearbyQuery } from '../models/destination.model';
import { buildNearbyParams } from './destination.service';

@Injectable({ providedIn: 'root' })
export class ItemService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getAll(): Observable<TraditionalItem[]> {
    return this.http.get<TraditionalItem[]>(`${this.base}${API_ENDPOINTS.ITEM.GET_ALL}`);
  }

  getNearby(query: NearbyQuery): Observable<TraditionalItem[]> {
    return this.http.get<TraditionalItem[]>(`${this.base}${API_ENDPOINTS.ITEM.GET_ALL}`, {
      params: buildNearbyParams(query)
    });
  }
}
