import { Injectable, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, of } from 'rxjs';
import { tap, catchError, map, switchMap, finalize, shareReplay, take } from 'rxjs/operators';
import { Router } from '@angular/router';
import { environment } from '../../environments/environment';
import { API_ENDPOINTS } from '../constants/api-endpoints';
import { User, LoginRequest, RegisterRequest, AuthResponse } from '../models/user.model';
import { TokenStorageService } from './token-storage.service';
import { UserRole } from '../enums/user-role.enum';

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
  private sessionRestoreInFlight$: Observable<boolean> | null = null;

  constructor(
    private http: HttpClient,
    private tokenStorage: TokenStorageService,
    private router: Router,
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
   * @returns Observable<AuthResponse | null> with new tokens (or null when refresh is not possible)
   */
  refreshToken(): Observable<AuthResponse | null> {
    if (!this.isBrowser) {
      return of(null);
    }

    const refreshToken = this.tokenStorage.getRefreshToken();

    if (!refreshToken || !refreshToken.trim()) {
      this.clearAuthState();
      return of(null);
    }

    const url = `${this.baseUrl}${API_ENDPOINTS.AUTH.REFRESH}`;
    return this.http.post<AuthResponse>(url, { refreshToken }).pipe(
      map(response => {
        // Guard against a 200 OK that carries no access token (e.g. already-rotated
        // refresh token or a server-side session invalidation).
        if (!response?.accessToken) {
          console.warn('Refresh flow: token endpoint returned no access token — session expired');
          this.clearAuthState();
          return null;
        }
        return response;
      }),
      tap(response => {
        if (response) {
          this.handleAuthResponse(response);
        }
      }),
      catchError(error => {
        // Suppress noisy logs for expected cases:
        // status 0  = network error / backend not running (ERR_CONNECTION_REFUSED)
        // status 401 = refresh token expired — handled silently
        const isExpectedError =
          error instanceof HttpErrorResponse &&
          (error.status === 0 || error.status === 401);
        if (!isExpectedError) {
          console.error('Token refresh failed:', error);
        }
        this.clearAuthState();
        return of(null);
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
      catchError(error => {
        const isSilent =
          error instanceof HttpErrorResponse &&
          (error.status === 0 || error.status === 401);
        if (!isSilent) {
          console.error('Logout failed:', error);
        }
        return of(void 0);
      }),
      finalize(() => this.clearAuthState())
    );
  }

  /**
   * Logout all sessions for current user
   * @returns Observable<void>
   */
  logoutAll(): Observable<void> {
    const url = `${this.baseUrl}${API_ENDPOINTS.AUTH.LOGOUT_ALL}`;
    return this.http.post<void>(url, {}).pipe(
      catchError(error => {
        const isSilent =
          error instanceof HttpErrorResponse &&
          (error.status === 0 || error.status === 401);
        if (!isSilent) {
          console.error('Logout all failed:', error);
        }
        return of(void 0);
      }),
      finalize(() => this.clearAuthState())
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
    return this.http.get<unknown>(url).pipe(
      map(apiUser => this.mapApiUser(apiUser)),
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
   * Restore browser session after hard refresh using refresh token.
   * Returns true on SSR to avoid server-side auth redirects.
   */
  restoreSession(): Observable<boolean> {
    if (!this.isBrowser) {
      return of(true);
    }

    if (this.isAuthenticated()) {
      return of(true);
    }

    const refreshToken = this.tokenStorage.getRefreshToken();
    if (!refreshToken) {
      return of(false);
    }

    if (this.sessionRestoreInFlight$) {
      return this.sessionRestoreInFlight$;
    }

    this.sessionRestoreInFlight$ = this.refreshToken().pipe(
      switchMap((response) => {
        if (!response) {
          return of(false);
        }
        return this.getCurrentUser().pipe(map((user) => !!user));
      }),
      catchError(() => of(false)),
      finalize(() => {
        this.sessionRestoreInFlight$ = null;
      }),
      shareReplay(1)
    );

    return this.sessionRestoreInFlight$;
  }

  /**
   * Handle authentication response (store tokens and update user state)
   * @param response AuthResponse from server
   */
  private handleAuthResponse(response: AuthResponse): void {
    // Store tokens
    this.tokenStorage.setTokens(response.accessToken, response.refreshToken);

    // Update current user
    const user = this.mapApiUser(response.user);
    this.currentUserSubject.next(user);
  }

  private mapApiUser(apiUser: any): User {
    return {
      id: apiUser?.id,
      username: apiUser?.username ?? '',
      email: apiUser?.email ?? '',
      role: this.normalizeRole(Array.isArray(apiUser?.roles) ? apiUser.roles[0] : apiUser?.role)
    };
  }

  private normalizeRole(rawRole: unknown): UserRole {
    const role = String(rawRole ?? '').replace(/^ROLE_/, '').toUpperCase();
    if (role === UserRole.ADMIN) return UserRole.ADMIN;
    if (role === UserRole.SUPER_ADMIN) return UserRole.SUPER_ADMIN;
    if (role === UserRole.VENDOR) return UserRole.VENDOR;
    return UserRole.USER;
  }

  /**
   * Invalidate the current session and redirect to the login page.
   *
   * Call this from interceptors or anywhere that detects a definitive
   * authentication failure (e.g. refresh token is expired / revoked).
   * Unlike logout(), this does NOT hit the backend logout endpoint — the
   * session is already gone on the server side.
   *
   * @param returnUrl Optional URL to redirect back to after login (defaults to current path)
   */
  handleSessionExpiry(returnUrl?: string): void {
    if (!this.isBrowser) return;

    this.clearAuthState();

    const target = returnUrl ?? (window.location.pathname + window.location.search);
    // Avoid redirect loops when already on the login page.
    if (target && target !== '/login') {
      this.router.navigate(['/login'], { queryParams: { returnUrl: target } });
    } else {
      this.router.navigate(['/login']);
    }
  }

  /**
   * Clear authentication state (tokens and user)
   */
  private clearAuthState(): void {
    this.tokenStorage.clearTokens();
    this.currentUserSubject.next(null);
  }

  /**
   * Initialize user state on service creation.
   *
   * IMPORTANT: must go through restoreSession() (not refreshToken() directly)
   * so that sessionRestoreInFlight$ is set before the Angular Router evaluates
   * any guards. Guards that call restoreSession() will then receive the same
   * in-flight Observable (via shareReplay) instead of issuing a second refresh
   * request that the backend would reject due to token rotation.
   */
  private initializeUserState(): void {
    const existingRefreshToken = this.tokenStorage.getRefreshToken();
    if (!existingRefreshToken) {
      return;
    }

    this.restoreSession().pipe(
      take(1),
      catchError(() => {
        this.clearAuthState();
        return of(false);
      })
    ).subscribe();
  }
}
