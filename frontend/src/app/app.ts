import { Component, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AuthService } from './services/auth.service';
import { DestinationService } from './services/destination.service';
import { ServiceCatalogService } from './services/service-catalog.service';
import { ThemeService } from './services/theme.service';
import { Destination } from './models/destination.model';
import { PublicServiceListing } from './models/vendor.model';
import { filter, debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Subject } from 'rxjs';
import { FooterComponent } from './shared/app-footer/app-footer';

interface SearchResult {
  type: 'destination' | 'service';
  label: string;
  sublabel: string;
  fragment: string;
}

@Component({
  selector: 'app-root',
  imports: [CommonModule, FormsModule, RouterOutlet, RouterLink, RouterLinkActive, FooterComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('AIPTMS');
  showMainNavbar = true;
  accountMenuOpen = false;

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
    private themeService: ThemeService
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
