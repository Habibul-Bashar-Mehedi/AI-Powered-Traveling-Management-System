import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError } from 'rxjs';
import { tap, catchError, map } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { User, LoginRequest, RegisterRequest, AuthResponse } from '../models/user.model';
import { TokenStorageService } from './token-storage.service';

/**
 * AuthService
 * 
 * Manages JWT authentication including:
 * - User registration and login
 * - Token refresh and rotation
 * - Logout (single session and all sessions)
 * - Current user state management
 * 
 * Requirements: 3.2.1
 */
@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly baseUrl = environment.apiUrl;
  private currentUserSubject = new BehaviorSubject<User | null>(null);
  public currentUser$ = this.currentUserSubject.asObservable();
  private isBrowser: boolean;

  constructor(
    private http: HttpClient,
    private tokenStorage: TokenStorageService,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
    // Initialize user state only in browser
    if (this.isBrowser) {
      this.initializeUserState();
    }
  }

  /**
   * Register new user and receive JWT tokens
   * @param request Registration request data
   * @returns Observable<AuthResponse> with user info and tokens
   */
  register(request: RegisterRequest): Observable<AuthResponse> {
    const url = `${this.baseUrl}${API_ENDPOINTS.AUTH.REGISTER}`;
    return this.http.post<AuthResponse>(url, request).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(error => {
        console.error('Registration failed:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Login user and receive JWT tokens
   * @param request Login credentials
   * @returns Observable<AuthResponse> with user info and tokens
   */
  login(request: LoginRequest): Observable<AuthResponse> {
    const url = `${this.baseUrl}${API_ENDPOINTS.AUTH.LOGIN}`;
    return this.http.post<AuthResponse>(url, request).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(error => {
        console.error('Login failed:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Refresh access token using refresh token
   * @returns Observable<AuthResponse> with new tokens
   */
  refreshToken(): Observable<AuthResponse> {
    const refreshToken = this.tokenStorage.getRefreshToken();
    
    if (!refreshToken) {
      return throwError(() => new Error('No refresh token available'));
    }

    const url = `${this.baseUrl}${API_ENDPOINTS.AUTH.REFRESH}`;
    return this.http.post<AuthResponse>(url, { refreshToken: refreshToken }).pipe(
      tap(response => this.handleAuthResponse(response)),
      catchError(error => {
        console.error('Token refresh failed:', error);
        // Clear tokens on refresh failure
        this.clearAuthState();
        return throwError(() => error);
      })
    );
  }

  /**
   * Logout current session
   * @returns Observable<void>
   */
  logout(): Observable<void> {
    const url = `${this.baseUrl}${API_ENDPOINTS.AUTH.LOGOUT}`;
    return this.http.post<void>(url, {}).pipe(
      tap(() => this.clearAuthState()),
      catchError(error => {
        console.error('Logout failed:', error);
        // Clear local state even if server request fails
        this.clearAuthState();
        return throwError(() => error);
      })
    );
  }

  /**
   * Logout all sessions for current user
   * @returns Observable<void>
   */
  logoutAll(): Observable<void> {
    const url = `${this.baseUrl}${API_ENDPOINTS.AUTH.LOGOUT_ALL}`;
    return this.http.post<void>(url, {}).pipe(
      tap(() => this.clearAuthState()),
      catchError(error => {
        console.error('Logout all failed:', error);
        // Clear local state even if server request fails
        this.clearAuthState();
        return throwError(() => error);
      })
    );
  }

  /**
   * Get current user profile from server
   * @returns Observable<User | null>
   */
  getCurrentUser(): Observable<User | null> {
    if (!this.isAuthenticated()) {
      return throwError(() => new Error('Not authenticated'));
    }

    const url = `${this.baseUrl}${API_ENDPOINTS.AUTH.ME}`;
    return this.http.get<User>(url).pipe(
      tap(user => this.currentUserSubject.next(user)),
      catchError(error => {
        console.error('Get current user failed:', error);
        return throwError(() => error);
      })
    );
  }

  /**
   * Check if user is authenticated (has valid access token)
   * @returns boolean
   */
  isAuthenticated(): boolean {
    return this.tokenStorage.getAccessToken() !== null;
  }

  /**
   * Get access token
   * @returns string | null
   */
  getAccessToken(): string | null {
    return this.tokenStorage.getAccessToken();
  }

  /**
   * Get current user value (synchronous)
   * @returns User | null
   */
  getCurrentUserValue(): User | null {
    return this.currentUserSubject.value;
  }

  /**
   * Handle authentication response (store tokens and update user state)
   * @param response AuthResponse from server
   */
  private handleAuthResponse(response: AuthResponse): void {
    // Store tokens
    this.tokenStorage.setTokens(response.accessToken, response.refreshToken);
    
    // Update current user
    const user: User = {
      id: response.user.id,
      username: response.user.username,
      email: response.user.email,
      role: response.user.roles[0] as any // Use first role
    };
    this.currentUserSubject.next(user);
  }

  /**
   * Clear authentication state (tokens and user)
   */
  private clearAuthState(): void {
    this.tokenStorage.clearTokens();
    this.currentUserSubject.next(null);
  }

  /**
   * Initialize user state on service creation
   * If refresh token exists, attempt to get current user
   */
  private initializeUserState(): void {
    const refreshToken = this.tokenStorage.getRefreshToken();
    
    if (refreshToken) {
      // Attempt to refresh token on app initialization
      this.refreshToken().subscribe({
        next: () => {
          // Token refreshed successfully, fetch current user
          this.getCurrentUser().subscribe();
        },
        error: () => {
          // Refresh failed, clear state
          this.clearAuthState();
        }
      });
    }
  }
}
