import { Injectable, Injector } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject } from 'rxjs';
import { catchError, filter, take, switchMap } from 'rxjs/operators';
import { TokenStorageService } from '../services/token-storage.service';
import { AuthService } from '../services/auth.service';
import { AuthResponse } from '../models/user.model';

/**
 * JwtInterceptor
 *
 * Automatically attaches JWT Bearer tokens to outgoing HTTP requests and handles token refresh on 401 errors.
 *
 * Features:
 * - Adds Authorization: Bearer <token> header to all requests except auth endpoints
 * - Intercepts 401 errors and attempts token refresh
 * - Queues failed requests during refresh and retries after new token obtained
 * - Prevents multiple simultaneous refresh calls using BehaviorSubject
 * - Logs out user if refresh fails
 *
 * Requirements: 3.2.2
 */
@Injectable()
export class JwtInterceptor implements HttpInterceptor {

  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<string | null> = new BehaviorSubject<string | null>(null);

  /**
   * AuthService is resolved lazily via Injector to avoid the circular dependency:
   * JwtInterceptor → AuthService → HttpClient → HTTP_INTERCEPTORS → JwtInterceptor
   */
  private get authService(): AuthService {
    return this.injector.get(AuthService);
  }

  constructor(
    private tokenStorage: TokenStorageService,
    private injector: Injector
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const isAuthRequest = this.isAuthEndpoint(request.url);
    const shouldSkipToken = this.isPublicAuthEndpoint(request.url);

    // Skip token attachment only for public auth endpoints.
    if (shouldSkipToken) {
      return next.handle(request);
    }

    // Add token to request
    const accessToken = this.tokenStorage.getAccessToken();
    if (accessToken) {
      request = this.addToken(request, accessToken);
    }

    return next.handle(request).pipe(
      catchError(error => {
        // Never attempt refresh for /auth/* endpoints; this avoids auth loops.
        if (!isAuthRequest && error instanceof HttpErrorResponse && error.status === 401) {
          return this.handle401Error(request, next);
        }
        return throwError(() => error);
      })
    );
  }

  /**
   * Add Bearer token to request headers
   */
  private addToken(request: HttpRequest<any>, token: string): HttpRequest<any> {
    return request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
  }

  /**
   * Check if URL is a public auth endpoint that should NOT have a token attached.
   * NOTE: /auth/logout is intentionally excluded — it requires a Bearer token so
   *       the backend can blacklist the JWT. Omitting it here caused logout to silently
   *       fail server-side while appearing successful client-side.
   */
  private isPublicAuthEndpoint(url: string): boolean {
    const publicEndpoints = ['/auth/login', '/auth/register', '/auth/refresh'];
    return publicEndpoints.some(endpoint => url.includes(endpoint));
  }

  private isAuthEndpoint(url: string): boolean {
    return url.includes('/auth/');
  }

  /**
   * Handle 401 error by attempting token refresh
   */
  private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      return this.authService.refreshToken().pipe(
        switchMap((response: AuthResponse | null) => {
          if (!response?.accessToken) {
            // Refresh itself returned 401 / no token — session is fully gone.
            // Do all cleanup here so catchError is only a safety-net for
            // unexpected Observable errors (e.g. network failure before emission).
            this.isRefreshing = false;

            const sessionErr = new HttpErrorResponse({
              error: { message: 'Session expired. Please log in again.' },
              status: 401,
              statusText: 'Unauthorized'
            });

            // Error the subject (not .complete()) so every queued request
            // receives a real error instead of completing silently with no value.
            this.refreshTokenSubject.error(sessionErr);
            this.refreshTokenSubject = new BehaviorSubject<string | null>(null);

            console.error('Refresh flow: token endpoint returned no access token — session expired');
            // Invalidate state and redirect to login — clearAuthState was already
            // called inside authService.refreshToken(), but handleSessionExpiry()
            // additionally navigates to /login so the user sees the login page.
            this.authService.handleSessionExpiry();
            return throwError(() => sessionErr);
          }

          // Refresh succeeded — unblock queued requests with the new token.
          this.isRefreshing = false;
          this.refreshTokenSubject.next(response.accessToken);

          // Retry the original request with the fresh token.
          return next.handle(this.addToken(request, response.accessToken));
        }),
        catchError(error => {
          // Safety-net: only reached when the refresh Observable itself errors
          // unexpectedly (e.g. a network failure before any emission).
          // The null-response case is already fully handled in switchMap above.
          if (this.isRefreshing) {
            // State was never cleaned up in switchMap — do it now.
            this.isRefreshing = false;
            const sessionErr = new HttpErrorResponse({
              error: { message: 'Session expired. Please log in again.' },
              status: 401,
              statusText: 'Unauthorized'
            });
            this.refreshTokenSubject.error(sessionErr);
            this.refreshTokenSubject = new BehaviorSubject<string | null>(null);
            this.authService.handleSessionExpiry();
          }
          console.error('Refresh flow failed:', error);
          return throwError(() => error);
        })
      );
    } else {
      // Refresh already in progress — queue this request until the new token arrives.
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap(token => next.handle(this.addToken(request, token!))),
        catchError(error => throwError(() => error))
      );
    }
  }
}
