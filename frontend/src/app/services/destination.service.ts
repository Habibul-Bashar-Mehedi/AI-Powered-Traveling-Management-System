import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { Destination, NearbyQuery } from '../models/destination.model';

@Injectable({ providedIn: 'root' })
export class DestinationService {
  private base = environment.apiUrl;
  private all$?: Observable<Destination[]>;

  constructor(private http: HttpClient) {}

  getAll(): Observable<Destination[]> {
    if (!this.all$) {
      this.all$ = this.http
        .get<Destination[]>(`${this.base}${API_ENDPOINTS.DESTINATION.GET_ALL}`)
        .pipe(shareReplay({ bufferSize: 1, refCount: false }));
    }
    return this.all$;
  }

  /** Destinations within radiusKm of (lat, lng), nearest first. Uncached — used by Explore Nearby. */
  getNearby(query: NearbyQuery): Observable<Destination[]> {
    return this.http.get<Destination[]>(`${this.base}${API_ENDPOINTS.DESTINATION.GET_ALL}`, {
      params: buildNearbyParams(query)
    });
  }
}

export function buildNearbyParams(query: NearbyQuery): HttpParams {
  let params = new HttpParams();
  if (query.lat != null) params = params.set('lat', query.lat);
  if (query.lng != null) params = params.set('lng', query.lng);
  if (query.radiusKm != null) params = params.set('radiusKm', query.radiusKm);
  if (query.destinationId != null) params = params.set('destinationId', query.destinationId);
  return params;
}
