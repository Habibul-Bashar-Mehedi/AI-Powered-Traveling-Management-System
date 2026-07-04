import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';

export interface AiChatTurn {
  sender: string;
  text: string;
}

export interface AiChatRequest {
  username: string | null;
  message: string;
  history: AiChatTurn[];
}

export interface AiChatResponse {
  reply: string;
}

@Injectable({ providedIn: 'root' })
export class AiChatService {
  private http = inject(HttpClient);
  private base = environment.apiUrl;

  sendMessage(request: AiChatRequest): Observable<AiChatResponse> {
    return this.http.post<AiChatResponse>(`${this.base}${API_ENDPOINTS.AI.CHAT}`, request);
  }
}
