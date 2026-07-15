import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { Food } from '../models/food.model';
import { NearbyQuery } from '../models/destination.model';
import { buildNearbyParams } from './destination.service';

@Injectable({ providedIn: 'root' })
export class FoodService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  getAll(): Observable<Food[]> {
    return this.http.get<Food[]>(`${this.base}${API_ENDPOINTS.FOOD.GET_ALL}`);
  }

  getNearby(query: NearbyQuery): Observable<Food[]> {
    return this.http.get<Food[]>(`${this.base}${API_ENDPOINTS.FOOD.GET_ALL}`, {
      params: buildNearbyParams(query)
    });
  }
}
