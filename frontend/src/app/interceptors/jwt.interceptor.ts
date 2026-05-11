import { Injectable } from '@angular/core';
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

  constructor(
    private tokenStorage: TokenStorageService,
    private authService: AuthService
  ) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    // Skip token attachment for auth endpoints
    if (this.isAuthEndpoint(request.url)) {
      return next.handle(request);
    }

    // Add token to request
    const accessToken = this.tokenStorage.getAccessToken();
    if (accessToken) {
      request = this.addToken(request, accessToken);
    }

    return next.handle(request).pipe(
      catchError(error => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
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
  private isAuthEndpoint(url: string): boolean {
    const authEndpoints = ['/auth/login', '/auth/register'];
    return authEndpoints.some(endpoint => url.includes(endpoint));
  }

  /**
   * Handle 401 error by attempting token refresh
   */
  private handle401Error(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      return this.authService.refreshToken().pipe(
        switchMap((response: AuthResponse) => {
          this.isRefreshing = false;
          this.refreshTokenSubject.next(response.accessToken);
          
          // Retry original request with new token
          return next.handle(this.addToken(request, response.accessToken));
        }),
        catchError(error => {
          this.isRefreshing = false;
          
          // Refresh failed, logout user
          this.authService.logout().subscribe({
            error: (logoutError) => console.error('Logout after refresh failure failed:', logoutError)
          });
          
          return throwError(() => error);
        })
      );
    } else {
      // Refresh is already in progress, queue this request
      return this.refreshTokenSubject.pipe(
        filter(token => token !== null),
        take(1),
        switchMap(token => {
          return next.handle(this.addToken(request, token!));
        })
      );
    }
  }
}
