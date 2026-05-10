import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { APP_CONSTANTS } from '../constants/app-constants';
import { User, LoginRequest, RegisterRequest, LoginResponse } from '../models/user.model';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = environment.apiUrl;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) {
    this.loadUserFromStorage();
  }

  /**
   * User registration
   */
  register(userData: RegisterRequest): Observable<User> {
    const url = `${this.baseUrl}${API_ENDPOINTS.AUTH.REGISTER}`;
    return this.http.post<User>(url, userData);
  }

  /**
   * User login
   */
  login(credentials: LoginRequest): Observable<string> {
    const url = `${this.baseUrl}${API_ENDPOINTS.AUTH.LOGIN}`;
    return this.http.post(url, credentials, {
      responseType: 'text'
    }).pipe(
      tap(response => {
        // Store user info if login successful
        if (response.includes('Successful')) {
          // In a real app, you'd parse the response and extract user data
          // For now, we'll store the credentials (not recommended in production)
          const user: User = {
            email: credentials.email,
            username: credentials.email.split('@')[0],
            role: 'USER' as any // This should come from the backend response
          };
          this.setCurrentUser(user);
        }
      })
    );
  }

  /**
   * User logout
   */
  logout(): void {
    localStorage.removeItem(APP_CONSTANTS.STORAGE_KEYS.TOKEN);
    localStorage.removeItem(APP_CONSTANTS.STORAGE_KEYS.USER);
    this.currentUserSubject.next(null);
  }

  /**
   * Get all users (admin only)
   */
  getAllUsers(): Observable<User[]> {
    const url = `${this.baseUrl}${API_ENDPOINTS.AUTH.GET_ALL_USERS}`;
    return this.http.get<User[]>(url);
  }

  /**
   * Update user
   */
  updateUser(id: number, userData: Partial<User>): Observable<string> {
    const url = `${this.baseUrl}${API_ENDPOINTS.AUTH.UPDATE_USER(id)}`;
    return this.http.put(url, userData, { responseType: 'text' });
  }

  /**
   * Delete user
   */
  deleteUser(id: number): Observable<string> {
    const url = `${this.baseUrl}${API_ENDPOINTS.AUTH.DELETE_USER(id)}`;
    return this.http.delete(url, { responseType: 'text' });
  }

  /**
   * Check if user is logged in
   */
  isLoggedIn(): boolean {
    return this.currentUserSubject.value !== null;
  }

  /**
   * Get current user
   */
  getCurrentUser(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Set current user
   */
  private setCurrentUser(user: User): void {
    localStorage.setItem(APP_CONSTANTS.STORAGE_KEYS.USER, JSON.stringify(user));
    this.currentUserSubject.next(user);
  }

  /**
   * Load user from storage
   */
  private loadUserFromStorage(): void {
    const userJson = localStorage.getItem(APP_CONSTANTS.STORAGE_KEYS.USER);
    if (userJson) {
      try {
        const user = JSON.parse(userJson);
        this.currentUserSubject.next(user);
      } catch (error) {
        console.error('Error parsing user from storage:', error);
        localStorage.removeItem(APP_CONSTANTS.STORAGE_KEYS.USER);
      }
    }
  }

  /**
   * Get authorization headers
   */
  getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem(APP_CONSTANTS.STORAGE_KEYS.TOKEN);
    return new HttpHeaders({
      'Content-Type': 'application/json',
      ...(token && { 'Authorization': `Bearer ${token}` })
    });
  }
}
