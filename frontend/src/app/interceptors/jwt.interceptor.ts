import { Injectable, Injector } from '@angular/core';
import { HttpInterceptor, HttpRequest, HttpHandler, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError, BehaviorSubject, EMPTY } from 'rxjs';
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
   * Check if URL is an auth endpoint that should not have token attached
   */
  private isPublicAuthEndpoint(url: string): boolean {
    const authEndpoints = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout'];
    return authEndpoints.some(endpoint => url.includes(endpoint));
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
            this.isRefreshing = false;

            // End queued retries and clear local auth state without throwing hard errors.
            this.refreshTokenSubject.complete();
            this.refreshTokenSubject = new BehaviorSubject<string | null>(null);
            this.authService.logout().subscribe();
            return EMPTY;
          }

          this.isRefreshing = false;
          this.refreshTokenSubject.next(response.accessToken);

          // Retry original request with new token
          return next.handle(this.addToken(request, response.accessToken));
        }),
        catchError(error => {
          this.isRefreshing = false;

          // End queued retries and clear local auth state without bubbling uncaught exceptions.
          this.refreshTokenSubject.complete();
          this.refreshTokenSubject = new BehaviorSubject<string | null>(null);
          this.authService.logout().subscribe();

          console.error('Refresh flow failed:', error);
          return EMPTY;
        })
      );
    } else {
      // Refresh is already in progress, queue this request
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap(token => {
          return next.handle(this.addToken(request, token!));
        }),
        catchError(() => EMPTY)
      );
    }
  }
}
