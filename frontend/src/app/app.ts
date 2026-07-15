import { Component, signal, Inject, PLATFORM_ID } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from './services/auth.service';
import { DestinationService } from './services/destination.service';
import { ServiceCatalogService } from './services/service-catalog.service';
import { ThemeService } from './services/theme.service';
import { VendorBookingService, UserBookingStatusSummary } from './services/vendor-booking.service';
import { Destination } from './models/destination.model';
import { PublicServiceListing } from './models/vendor.model';
import { filter, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { FooterComponent } from './shared/app-footer/app-footer';
import { ConfirmDialog } from './shared/confirm-dialog/confirm-dialog';

interface SearchResult {
  type: 'destination' | 'service';
  label: string;
  sublabel: string;
  fragment: string;
}

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink, RouterLinkActive, FooterComponent, ConfirmDialog],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('SMTS');
  showMainNavbar = true;
  accountMenuOpen = false;
  notificationCount = 0;

  readonly currentUser$;

  // ─── Search ─────────────────────────────────────────────────────
  searchQuery = '';
  searchOpen = false;
  searchResults: SearchResult[] = [];
  private searchInput$ = new Subject<string>();
  private destinationsCache: Destination[] = [];
  private servicesCache: PublicServiceListing[] = [];
  private searchDataLoaded = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    private destinationService: DestinationService,
    private serviceCatalogService: ServiceCatalogService,
    private themeService: ThemeService,
    private vendorBookingService: VendorBookingService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.currentUser$ = this.authService.currentUser$;

    // Hide main navbar on vendor and admin routes
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: NavigationEnd) => {
      this.showMainNavbar = !event.url.startsWith('/vendor') && !event.url.startsWith('/admin');
      this.accountMenuOpen = false;
      this.closeSearch();
    });

    this.searchInput$.pipe(
      debounceTime(200),
      distinctUntilChanged()
    ).subscribe(query => this.runSearch(query));

    // Only fetch the notification count for a logged-in user, and only in the
    // browser — this endpoint requires a Bearer token, so calling it during SSR
    // (no token yet) or while logged out produced an unhandled 401 that crashed
    // out as an uncaught exception in the SSR process. Re-fires on login/logout
    // too, since currentUser$ is a BehaviorSubject.
    if (isPlatformBrowser(this.platformId)) {
      this.currentUser$.subscribe(user => {
        if (user) {
          this.loadNotificationCount();
        } else {
          this.notificationCount = 0;
        }
      });
    }
  }

  private loadNotificationCount(): void {
    this.vendorBookingService.getMyBookingStatusSummary().subscribe({
      next: (summary: UserBookingStatusSummary) => {
        this.notificationCount = summary.counts?.PENDING ?? 0;
      },
      error: () => {
        // Non-critical — the notification badge just stays at its last value.
      }
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
    this.router.navigate([this.authService.getPostAuthRedirectUrl(user?.role)]);
  }

  toggleAccountMenu(): void {
    this.accountMenuOpen = !this.accountMenuOpen;
  }

  logout(): void {
    this.accountMenuOpen = false;
    this.authService.logout().subscribe({ complete: () => this.router.navigate(['/login']) });
  }

  get isDarkTheme(): boolean {
    return this.themeService.theme() === 'dark';
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  // ─── Search ─────────────────────────────────────────────────────
  onSearchFocus(): void {
    this.searchOpen = true;
    this.loadSearchDataOnce();
  }

  onSearchInput(): void {
    this.searchInput$.next(this.searchQuery);
  }

  closeSearch(): void {
    this.searchOpen = false;
  }

  /** Delayed so a result's (mousedown) has a chance to fire before the input's blur hides the dropdown. */
  closeSearchDelayed(): void {
    setTimeout(() => this.closeSearch(), 150);
  }

  selectSearchResult(result: SearchResult): void {
    this.searchQuery = '';
    this.searchResults = [];
    this.closeSearch();
    this.router.navigate(['/dashboard'], { fragment: result.fragment });
  }

  private loadSearchDataOnce(): void {
    if (this.searchDataLoaded) return;
    this.searchDataLoaded = true;

    this.destinationService.getAll().subscribe({
      next: (destinations) => { this.destinationsCache = destinations; },
      error: () => { /* search still works with whatever loaded */ }
    });
    this.serviceCatalogService.getActiveServices(0, 50).subscribe({
      next: (page) => { this.servicesCache = page.content; },
      error: () => { /* search still works with whatever loaded */ }
    });
  }

  private runSearch(rawQuery: string): void {
    const query = rawQuery.trim().toLowerCase();
    if (!query) {
      this.searchResults = [];
      return;
    }

    const destinationMatches: SearchResult[] = this.destinationsCache
      .filter(d => d.name.toLowerCase().includes(query) || d.region.toLowerCase().includes(query))
      .slice(0, 5)
      .map(d => ({ type: 'destination', label: d.name, sublabel: d.region, fragment: 'destinations' }));

    const serviceMatches: SearchResult[] = this.servicesCache
      .filter(s => s.serviceName.toLowerCase().includes(query) || s.vendorBusinessName.toLowerCase().includes(query))
      .slice(0, 5)
      .map(s => ({ type: 'service', label: s.serviceName, sublabel: s.vendorBusinessName, fragment: 'services' }));

    this.searchResults = [...destinationMatches, ...serviceMatches].slice(0, 8);
  }
}
