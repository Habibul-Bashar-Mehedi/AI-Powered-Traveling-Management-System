import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BookingHistoryService } from '../services/booking-history.service';
import { BookingHistoryEntry } from '../models/booking-history.model';
import { BookingSource } from '../enums/booking-source.enum';
import { BookingHistoryList } from './booking-history-list';
import { LoadingState } from '../shared/loading-state/loading-state';

const STATUS_OPTIONS = ['PENDING', 'CONFIRMED', 'COMPLETED', 'CHECKED_IN', 'CHECKED_OUT', 'CANCELLED', 'REJECTED'];

@Component({
  selector: 'app-booking-history',
  imports: [CommonModule, BookingHistoryList, LoadingState],
  templateUrl: './booking-history.html',
  styleUrl: './booking-history.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingHistory implements OnInit {
  private readonly bookingHistoryService = inject(BookingHistoryService);

  readonly BookingSource = BookingSource;
  readonly statusOptions = STATUS_OPTIONS;
  readonly pageSize = 10;

  readonly entries = signal<BookingHistoryEntry[]>([]);
  readonly loading = signal(false);
  readonly error = signal('');
  readonly page = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly typeFilter = signal<BookingSource | ''>('');
  readonly statusFilter = signal('');
  readonly loaded = signal(false);
  readonly downloadingId = signal<string | null>(null);

  readonly hasPrev = computed(() => this.page() > 0);
  readonly hasNext = computed(() => this.page() + 1 < this.totalPages());

  ngOnInit(): void {
    this.load(0);
  }

  load(page = 0): void {
    this.loading.set(true);
    this.error.set('');
    this.bookingHistoryService.getHistory({
      type: this.typeFilter() || undefined,
      status: this.statusFilter() || undefined,
      page,
      size: this.pageSize,
    }).subscribe({
      next: (result) => {
        this.entries.set(result.content);
        this.page.set(result.number);
        this.totalPages.set(result.totalPages);
        this.totalElements.set(result.totalElements);
        this.loading.set(false);
        this.loaded.set(true);
      },
      error: () => {
        this.error.set('Could not load booking history. Please try again.');
        this.loading.set(false);
        this.loaded.set(true);
      }
    });
  }

  onTypeFilterChange(value: string): void {
    this.typeFilter.set(value as BookingSource | '');
    this.load(0);
  }

  onStatusFilterChange(value: string): void {
    this.statusFilter.set(value);
    this.load(0);
  }

  nextPage(): void {
    if (this.hasNext()) this.load(this.page() + 1);
  }

  prevPage(): void {
    if (this.hasPrev()) this.load(this.page() - 1);
  }

  statusLabel(status: string): string {
    return status.charAt(0) + status.slice(1).toLowerCase();
  }

  downloadReceipt(entry: BookingHistoryEntry): void {
    this.downloadingId.set(entry.id);
    this.bookingHistoryService.downloadReceipt(entry.id).subscribe({
      next: (blob) => {
        this.saveBlob(blob, `receipt-${entry.id}.pdf`);
        this.downloadingId.set(null);
      },
      error: () => {
        this.error.set('Could not download the receipt. Please try again.');
        this.downloadingId.set(null);
      }
    });
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
