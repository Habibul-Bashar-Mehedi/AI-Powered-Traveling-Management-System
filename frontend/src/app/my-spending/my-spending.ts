import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserExpenseService } from '../services/user-expense.service';
import { DestinationExpenseSummary } from '../models/user-expense.model';
import { BookingHistoryEntry } from '../models/booking-history.model';
import { BookingHistoryList } from '../booking-history/booking-history-list';
import { LoadingState } from '../shared/loading-state/loading-state';

@Component({
  selector: 'app-my-spending',
  imports: [CommonModule, BookingHistoryList, LoadingState],
  templateUrl: './my-spending.html',
  styleUrl: './my-spending.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MySpending implements OnInit {
  private readonly userExpenseService = inject(UserExpenseService);

  readonly summaries = signal<DestinationExpenseSummary[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly sort = signal<'totalSpent' | 'latest'>('totalSpent');

  readonly selectedDestination = signal<DestinationExpenseSummary | null>(null);
  readonly drilldownEntries = signal<BookingHistoryEntry[]>([]);
  readonly drilldownLoading = signal(false);
  readonly drilldownError = signal('');

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.userExpenseService.getSummary(this.sort()).subscribe({
      next: (summaries) => {
        this.summaries.set(summaries);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Could not load your spending summary. Please try again.');
        this.loading.set(false);
      }
    });
  }

  onSortChange(value: string): void {
    this.sort.set(value as 'totalSpent' | 'latest');
    this.load();
  }

  openDrilldown(summary: DestinationExpenseSummary): void {
    this.selectedDestination.set(summary);
    this.drilldownEntries.set([]);
    this.drilldownError.set('');
    this.drilldownLoading.set(true);

    this.userExpenseService.getDrilldown(summary.destinationId).subscribe({
      next: (entries) => {
        this.drilldownEntries.set(entries);
        this.drilldownLoading.set(false);
      },
      error: () => {
        this.drilldownError.set('Could not load bookings for this destination.');
        this.drilldownLoading.set(false);
      }
    });
  }

  closeDrilldown(): void {
    this.selectedDestination.set(null);
  }
}
