import { ChangeDetectorRef, Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VendorAnalyticsService } from '../../services/vendor-analytics.service';
import { AnalyticsSummary, ServicePerformance } from '../../models/vendor.model';

@Component({
  selector: 'app-vendor-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './vendor-analytics.html',
  styleUrls: ['../shared-vendor.css', './vendor-analytics.css']
})
export class VendorAnalytics implements OnInit {
  summary: AnalyticsSummary | null = null;
  loading = false;
  fromDate = '';
  toDate = '';
  error = '';

  constructor(
    private analyticsService: VendorAnalyticsService,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    const now = new Date();
    this.fromDate = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0];
    this.toDate = now.toISOString().split('T')[0];
    if (!isPlatformBrowser(this.platformId)) return;
    this.load();
  }

  private applyViewState(update: () => void): void {
    setTimeout(() => {
      update();
      this.cdr.markForCheck();
    }, 0);
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.analyticsService.getSummary(this.fromDate, this.toDate).subscribe({
      next: (s) => this.applyViewState(() => {
        this.summary = s;
        this.loading = false;
      }),
      error: () => this.applyViewState(() => {
        this.error = 'Failed to load analytics';
        this.loading = false;
      })
    });
  }

  get revenueEntries(): { date: string; value: number }[] {
    if (!this.summary?.revenueTimeSeries) return [];
    return Object.entries(this.summary.revenueTimeSeries)
      .map(([date, value]) => ({ date, value: Number(value) }))
      .sort((a, b) => a.date.localeCompare(b.date));
  }

  get maxRevenue(): number {
    const entries = this.revenueEntries;
    return entries.length ? Math.max(...entries.map(e => e.value)) : 1;
  }

  barHeight(value: number): number {
    return Math.max(4, (value / this.maxRevenue) * 120);
  }

  barHeightClass(value: number): string {
    const bucket = Math.max(1, Math.min(12, Math.round(this.barHeight(value) / 10)));
    return `bar-h-${bucket}`;
  }

  trackByRevenueEntry(index: number, entry: { date: string; value: number }): string {
    return entry.date;
  }

  trackByService(index: number, service: ServicePerformance): string {
    return service.serviceId;
  }
}

