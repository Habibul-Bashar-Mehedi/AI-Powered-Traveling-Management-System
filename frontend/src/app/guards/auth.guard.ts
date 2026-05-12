import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router, UrlTree } from '@angular/router';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

/**
 * AuthGuard
 *
 * Protects routes from unauthorized access.
 * Redirects to login page if user is not authenticated.
 *
 * Requirements: BR-2
 */
@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {
  private readonly isBrowser: boolean;

  constructor(
    private authService: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean | UrlTree> | Promise<boolean | UrlTree> | boolean | UrlTree {
    // In SSR we have no token state; skip redirects and let the browser
    // enforce authentication after hydration.
    if (!this.isBrowser) return true;

    if (this.authService.isAuthenticated()) {
      return true;
    }

    return this.authService.restoreSession().pipe(
      map((restored) => {
        if (restored) {
          return true;
        }

        // Not authenticated, redirect to login with return URL
        return this.router.createUrlTree(['/login'], {
          queryParams: { returnUrl: state.url }
        });
      })
    );
  }
}
