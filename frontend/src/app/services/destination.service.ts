import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, shareReplay } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { Destination } from '../models/destination.model';

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
}
