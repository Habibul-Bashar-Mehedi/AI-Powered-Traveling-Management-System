import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { Banner } from '../models/banner.model';

@Injectable({ providedIn: 'root' })
export class BannerService {
  private base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getActiveBanners(): Observable<Banner[]> {
    return this.http.get<Banner[]>(`${this.base}${API_ENDPOINTS.BANNER.ACTIVE}`);
  }
}
