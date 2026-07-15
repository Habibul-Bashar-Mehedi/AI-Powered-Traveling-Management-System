import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { BookingHistoryEntry } from '../models/booking-history.model';
import { BookingSource } from '../enums/booking-source.enum';

const RECEIPT_ELIGIBLE_STATUSES = new Set(['CONFIRMED', 'COMPLETED', 'CHECKED_IN', 'CHECKED_OUT']);

/**
 * Presentational booking card list — shared by the full "Booking History" feed
 * and the "My Spending" per-destination drill-down, so both render bookings identically.
 */
@Component({
  selector: 'app-booking-history-list',
  imports: [CommonModule],
  templateUrl: './booking-history-list.html',
  styleUrl: './booking-history.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class BookingHistoryList {
  readonly entries = input.required<BookingHistoryEntry[]>();
  readonly emptyMessage = input('No bookings found.');
  readonly downloadingId = input<string | null>(null);

  readonly downloadReceipt = output<BookingHistoryEntry>();

  sourceIcon(entry: BookingHistoryEntry): string {
    return entry.source === BookingSource.HOTEL_DIRECT ? '🏨' : '🧭';
  }

  canDownloadReceipt(entry: BookingHistoryEntry): boolean {
    return RECEIPT_ELIGIBLE_STATUSES.has(entry.status);
  }

  statusLabel(status: string): string {
    return status.charAt(0) + status.slice(1).toLowerCase();
  }

  statusBadgeClass(status: string): string {
    switch (status) {
      case 'CONFIRMED': return 'badge-confirmed';
      case 'PENDING': return 'badge-pending';
      case 'COMPLETED':
      case 'CHECKED_IN':
      case 'CHECKED_OUT': return 'badge-completed';
      case 'CANCELLED': return 'badge-cancelled';
      case 'REJECTED': return 'badge-rejected';
      default: return 'badge-pending';
    }
  }
}
