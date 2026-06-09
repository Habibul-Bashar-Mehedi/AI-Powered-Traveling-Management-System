import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CanActivate, Router, UrlTree } from '@angular/router';
import { catchError, map, Observable, of, switchMap } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../enums/user-role.enum';

@Injectable({ providedIn: 'root' })
export class AdminGuard implements CanActivate {
  private readonly isBrowser: boolean;

  constructor(
    private authService: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  canActivate(): boolean | UrlTree | Observable<boolean | UrlTree> {
    // Avoid SSR redirects; browser-side auth guard will enforce access after hydration.
    if (!this.isBrowser) return true;

    const user = this.authService.getCurrentUserValue();
    if (user && (user.role === UserRole.ADMIN || user.role === UserRole.SUPER_ADMIN)) return true;
    if (user && user.role !== UserRole.ADMIN && user.role !== UserRole.SUPER_ADMIN) {
      return this.router.createUrlTree(['/dashboard']);
    }

    return this.authService.restoreSession().pipe(
      switchMap((restored) => {
        if (!restored) {
          return of(this.router.createUrlTree(['/login']));
        }

        const freshUser = this.authService.getCurrentUserValue();
        if (freshUser) {
          return of(
            freshUser.role === UserRole.ADMIN || freshUser.role === UserRole.SUPER_ADMIN
              ? true
              : this.router.createUrlTree(['/dashboard'])
          );
        }

        return this.authService.getCurrentUser().pipe(
          map((resolvedUser) =>
            resolvedUser?.role === UserRole.ADMIN || resolvedUser?.role === UserRole.SUPER_ADMIN
              ? true
              : this.router.createUrlTree(['/dashboard'])
          ),
          catchError(() => of(this.router.createUrlTree(['/login'])))
        );
      }),
      catchError(() => of(this.router.createUrlTree(['/login'])))
    );
  }
}

