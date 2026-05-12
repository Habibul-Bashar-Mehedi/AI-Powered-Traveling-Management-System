import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { VendorService } from '../../services/vendor.service';
import { VendorWalletService } from '../../services/vendor-wallet.service';
import { VendorAnalyticsService } from '../../services/vendor-analytics.service';
import { VendorProfile, WalletSummary, AnalyticsSummary } from '../../models/vendor.model';

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
  loading = true;

  constructor(
    private vendorService: VendorService,
    private walletService: VendorWalletService,
    private analyticsService: VendorAnalyticsService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.vendorService.getProfile().subscribe({ next: (v) => { this.vendor = v; } });
    this.walletService.getWalletSummary().subscribe({ next: (w) => { this.wallet = w; } });

    const now = new Date();
    const from = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0];
    const to = now.toISOString().split('T')[0];
    this.analyticsService.getSummary(from, to).subscribe({
      next: (a) => { this.analytics = a; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  navigate(path: string): void { this.router.navigate([path]); }
}

