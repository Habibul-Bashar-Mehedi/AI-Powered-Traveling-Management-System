import { Component, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from './services/auth.service';
import { UserRole } from './enums/user-role.enum';
import { filter } from 'rxjs/operators';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('AIPTMS');
  showMainNavbar = true;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    // Hide main navbar on vendor and admin routes
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.showMainNavbar = !event.url.startsWith('/vendor') && !event.url.startsWith('/admin');
    });
  }

  /**
   * Navigate to the appropriate dashboard based on user role
   */
  navigateToDashboard(event: Event): void {
    event.preventDefault();

    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    const user = this.authService.getCurrentUserValue();
    const role = user?.role;

    if (role === UserRole.VENDOR) {
      this.router.navigate(['/vendor/dashboard']);
    } else if (role === UserRole.ADMIN || role === UserRole.SUPER_ADMIN) {
      this.router.navigate(['/admin/vendors']);
    } else {
      this.router.navigate(['/dashboard']);
    }
  }
}
