import { ChangeDetectorRef, Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError, defaultIfEmpty } from 'rxjs/operators';
import { VendorService } from '../../services/vendor.service';
import { VendorWalletService } from '../../services/vendor-wallet.service';
import { VendorAnalyticsService } from '../../services/vendor-analytics.service';
import { VendorProfile, WalletSummary, AnalyticsSummary, ServicePerformance } from '../../models/vendor.model';

@Component({
  selector: 'app-vendor-overview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './vendor-overview.html',
  styleUrls: ['../shared-vendor.css', './vendor-overview.css']
})
export class VendorOverview implements OnInit {
  vendor: VendorProfile | null = null;
  wallet: WalletSummary | null = null;
  analytics: AnalyticsSummary | null = null;
  loading = false;

  constructor(
    private vendorService: VendorService,
    private walletService: VendorWalletService,
    private analyticsService: VendorAnalyticsService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.loadOverview();
  }

  private applyViewState(update: () => void): void {
    setTimeout(() => {
      update();
      this.cdr.markForCheck();
    });
  }

  private loadOverview(): void {
    this.loading = true;

    const now = new Date();
    const from = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0];
    const to = now.toISOString().split('T')[0];

    forkJoin({
      vendor: this.vendorService.getProfile().pipe(
        defaultIfEmpty(null as VendorProfile | null),
        catchError(() => of(null as VendorProfile | null))
      ),
      wallet: this.walletService.getWalletSummary().pipe(
        defaultIfEmpty(null as WalletSummary | null),
        catchError(() => of(null as WalletSummary | null))
      ),
      analytics: this.analyticsService.getSummary(from, to).pipe(
        defaultIfEmpty(null as AnalyticsSummary | null),
        catchError(() => of(null as AnalyticsSummary | null))
      ),
    }).subscribe({
      next: ({ vendor, wallet, analytics }: {
        vendor: VendorProfile | null;
        wallet: WalletSummary | null;
        analytics: AnalyticsSummary | null;
      }) => this.applyViewState(() => {
        this.vendor = vendor;
        this.wallet = wallet;
        this.analytics = analytics;
        this.loading = false;
      }),
      error: () => this.applyViewState(() => {
        this.loading = false;
      }),
    });
  }

  navigate(path: string): void { this.router.navigate([path]); }

  trackByService(index: number, service: ServicePerformance): string {
    return service.serviceId;
  }
}

