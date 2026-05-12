import { Inject, Injectable, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CanActivate, Router, UrlTree } from '@angular/router';
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

  canActivate(): boolean | UrlTree {
    // Avoid SSR redirects; browser-side auth guard will enforce access after hydration.
    if (!this.isBrowser) return true;

    const user = this.authService.getCurrentUserValue();
    if (user && user.role === UserRole.ADMIN) return true;
    if (!this.authService.isAuthenticated()) return this.router.createUrlTree(['/login']);
    return this.router.createUrlTree(['/dashboard']);
  }
}

