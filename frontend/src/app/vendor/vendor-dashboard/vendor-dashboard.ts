import { Component, OnInit, OnDestroy, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavigationEnd, Router, RouterModule, RouterOutlet } from '@angular/router';
import { filter, Subscription } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { VendorService } from '../../services/vendor.service';
import { VendorProfile, ReinstatementRequest } from '../../models/vendor.model';
import { ReinstatementStatus } from '../../enums/vendor.enums';
import { FooterComponent } from '../../shared/app-footer/app-footer';

@Component({
  selector: 'app-vendor-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, RouterOutlet, FooterComponent],
  templateUrl: './vendor-dashboard.html',
  styleUrls: ['./vendor-dashboard.css']
})
export class VendorDashboard implements OnInit, OnDestroy {
  vendor: VendorProfile | null = null;
  activeNav = 'overview';
  loading = false;   // start false to avoid SSR/hydration mismatch; set true in browser ngOnInit
  error: string | null = null;
  private navSyncSub?: Subscription;

  // ─── Reinstatement request (suspended vendors only) ────────────────
  reinstatementRequests: ReinstatementRequest[] = [];
  showReinstatementForm = false;
  reinstatementMessage = '';
  reinstatementSubmitting = false;
  reinstatementError = '';
  ReinstatementStatus = ReinstatementStatus;

  get hasPendingReinstatementRequest(): boolean {
    return this.reinstatementRequests.some(r => r.status === ReinstatementStatus.PENDING);
  }

  /** Requests are loaded ordered by submittedAt desc, so index 0 is the most recent. */
  get lastReinstatementRequest(): ReinstatementRequest | null {
    return this.reinstatementRequests[0] ?? null;
  }

  navItems = [
    { id: 'overview',  label: 'Overview',   path: '/vendor/dashboard' },
    { id: 'services',  label: 'Services',   path: '/vendor/services'  },
    { id: 'bookings',  label: 'Bookings',   path: '/vendor/bookings'  },
    { id: 'wallet',    label: 'Wallet',     path: '/vendor/wallet'    },
    { id: 'analytics', label: 'Analytics',  path: '/vendor/analytics' },
  ];

  constructor(
    private vendorService: VendorService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // Skip authenticated API calls during SSR — tokens aren't available server-side
    // and the resulting state change would trigger NG0100 during hydration.
    if (!isPlatformBrowser(this.platformId)) return;

    this.loading = true;
    this.vendorService.getProfile().subscribe({
      next: (v) => {
        this.vendor = v;
        this.loading = false;
        this.error = null;
        this.cdr.markForCheck();
        if (v.status === 'SUSPENDED') {
          this.loadReinstatementRequests();
        }
      },
      error: (err) => {
        this.loading = false;
        // 404 means vendor profile not yet created — redirect to registration
        if (err?.status === 404) {
          this.router.navigate(['/vendor/register']);
          return;
        }
        this.error = err?.error?.message || 'Failed to load vendor profile.';
        this.cdr.markForCheck();
      }
    });

    this.syncActiveNav(this.router.url);
    this.navSyncSub = this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe((event) => this.syncActiveNav((event as NavigationEnd).urlAfterRedirects));
  }

  ngOnDestroy(): void {
    this.navSyncSub?.unsubscribe();
  }

  navigate(item: any): void {
    this.activeNav = item.id;
    this.router.navigate([item.path]);
  }

  logout(): void {
    this.authService.logout().subscribe({ complete: () => this.router.navigate(['/login']) });
  }

  get currentNavLabel(): string {
    return this.navItems.find((item) => item.id === this.activeNav)?.label ?? 'Dashboard';
  }

  get commissionRateDisplay(): string {
    const value = this.vendor?.commissionRate;
    if (value === undefined || value === null) return 'Not set';
    const normalized = value > 1 ? value : value * 100;
    return `${normalized.toFixed(1)}%`;
  }

  private syncActiveNav(url: string): void {
    const match = this.navItems.find((item) => url.includes(item.path));
    if (match) this.activeNav = match.id;
  }

  get statusColor(): string {
    switch (this.vendor?.status) {
      case 'APPROVED': return '#10b981';
      case 'PENDING_REVIEW': return '#f59e0b';
      case 'REJECTED': return '#f43f5e';
      case 'SUSPENDED': return '#7c3aed';
      default: return '#9ca3af';
    }
  }

  trackByNavItem(index: number, item: { id: string; label: string; path: string }): string {
    return item.id;
  }

  // ─── Reinstatement request ──────────────────────────────────────────
  loadReinstatementRequests(): void {
    this.vendorService.getMyReinstatementRequests().subscribe({
      next: (requests) => {
        this.reinstatementRequests = requests || [];
        this.cdr.markForCheck();
      },
      error: () => {
        // Non-critical — banner still works without request history
      }
    });
  }

  openReinstatementForm(): void {
    this.reinstatementMessage = '';
    this.reinstatementError = '';
    this.showReinstatementForm = true;
  }

  closeReinstatementForm(): void {
    this.showReinstatementForm = false;
  }

  submitReinstatementRequest(): void {
    if (this.reinstatementMessage.trim().length < 10 || this.reinstatementSubmitting) return;
    this.reinstatementSubmitting = true;
    this.reinstatementError = '';
    this.vendorService.createReinstatementRequest(this.reinstatementMessage.trim()).subscribe({
      next: () => {
        this.reinstatementSubmitting = false;
        this.showReinstatementForm = false;
        this.loadReinstatementRequests();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.reinstatementError = err?.error?.message || 'Failed to submit reinstatement request.';
        this.reinstatementSubmitting = false;
        this.cdr.markForCheck();
      }
    });
  }
}

