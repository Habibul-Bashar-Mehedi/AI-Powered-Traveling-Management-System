import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VendorAnalyticsService } from '../../services/vendor-analytics.service';
import { AnalyticsSummary } from '../../models/vendor.model';

@Component({
  selector: 'app-vendor-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './vendor-analytics.html',
  styleUrls: ['../shared-vendor.css', './vendor-analytics.css']
})
export class VendorAnalytics implements OnInit {
  summary: AnalyticsSummary | null = null;
  loading = true;
  fromDate = '';
  toDate = '';
  error = '';

  constructor(private analyticsService: VendorAnalyticsService) {}

  ngOnInit(): void {
    // Default: this month
    const now = new Date();
    this.fromDate = new Date(now.getFullYear(), now.getMonth(), 1).toISOString().split('T')[0];
    this.toDate = now.toISOString().split('T')[0];
    this.load();
  }

  load(): void {
    this.loading = true;
    this.analyticsService.getSummary(this.fromDate, this.toDate).subscribe({
      next: (s) => { this.summary = s; this.loading = false; },
      error: (err) => { this.error = 'Failed to load analytics'; this.loading = false; }
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
}

