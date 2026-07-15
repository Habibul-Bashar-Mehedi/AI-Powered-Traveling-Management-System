import { ChangeDetectorRef, Component, Inject, OnDestroy, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VendorBookingService, UserBookingStatusSummary } from '../services/vendor-booking.service';
import { BookingHistoryService } from '../services/booking-history.service';
import { VendorBooking } from '../models/vendor.model';
import { VendorBookingStatus } from '../enums/vendor.enums';
import { LoadingState } from '../shared/loading-state/loading-state';

/**
 * Standalone "My Vendor Requests" panel — extracted from the dashboard so it can
 * be opened from the Profile page's side drawer instead of living inline on the
 * main dashboard page.
 */
@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule, FormsModule, LoadingState],
  templateUrl: './my-bookings.html',
  styleUrl: './my-bookings.css',
})
export class MyBookings implements OnInit, OnDestroy {
  constructor(
    private cdr: ChangeDetectorRef,
    private vendorBookingService: VendorBookingService,
    private bookingHistoryService: BookingHistoryService,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  myBookings: VendorBooking[] = [];
  myBookingsLoading = false;
  bookingStatusFilter: VendorBookingStatus | '' = '';
  bookingStatusSummary: UserBookingStatusSummary | null = null;
  bookingCancelLoading: string | null = null;
  receiptDownloadLoading: string | null = null;
  bookingStatusOptions = Object.values(VendorBookingStatus);

  private bookingsPollId?: ReturnType<typeof setInterval>;
  private readonly bookingsPollMs = 15000;

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.loadBookingStatusSummary();
    this.loadMyBookings();
    this.startBookingsPolling();
  }

  ngOnDestroy(): void {
    this.stopBookingsPolling();
  }

  loadBookingStatusSummary(): void {
    this.vendorBookingService.getMyBookingStatusSummary().subscribe({
      next: (summary) => {
        this.bookingStatusSummary = summary;
        this.cdr.detectChanges();
      },
      error: () => {
        // Non-critical — panel still works without summary counts
      }
    });
  }

  loadMyBookings(silent = false): void {
    if (!silent) {
      this.myBookingsLoading = true;
      this.cdr.detectChanges();
    }
    const status = this.bookingStatusFilter ? (this.bookingStatusFilter as VendorBookingStatus) : undefined;

    this.vendorBookingService.getMyBookings(status).subscribe({
      next: (bookings) => {
        this.myBookings = bookings;
        this.myBookingsLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.myBookingsLoading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onBookingStatusFilterChange(): void {
    this.loadMyBookings();
  }

  onStatusChipClick(status: VendorBookingStatus): void {
    this.bookingStatusFilter = this.bookingStatusFilter === status ? '' : status;
    this.loadMyBookings();
  }

  refreshBookings(): void {
    this.loadBookingStatusSummary();
    this.loadMyBookings();
  }

  statusCount(status: VendorBookingStatus): number {
    return this.bookingStatusSummary?.counts?.[status] ?? 0;
  }

  canCancelBooking(status: VendorBookingStatus): boolean {
    return status === VendorBookingStatus.PENDING || status === VendorBookingStatus.CONFIRMED;
  }

  cancelMyBooking(booking: VendorBooking): void {
    const reason = 'Cancelled from user profile';
    this.bookingCancelLoading = booking.bookingId;
    this.vendorBookingService.cancelMyBooking(booking.bookingId, reason).subscribe({
      next: () => {
        this.bookingCancelLoading = null;
        this.loadBookingStatusSummary();
        this.loadMyBookings(true);
        this.cdr.detectChanges();
      },
      error: () => {
        this.bookingCancelLoading = null;
        this.cdr.detectChanges();
      }
    });
  }

  canDownloadReceipt(status: VendorBookingStatus): boolean {
    return status === VendorBookingStatus.CONFIRMED || status === VendorBookingStatus.COMPLETED;
  }

  downloadReceipt(booking: VendorBooking): void {
    this.receiptDownloadLoading = booking.bookingId;
    this.bookingHistoryService.downloadReceipt(booking.bookingId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `receipt-${booking.bookingId}.pdf`;
        anchor.click();
        URL.revokeObjectURL(url);
        this.receiptDownloadLoading = null;
        this.cdr.detectChanges();
      },
      error: () => {
        this.receiptDownloadLoading = null;
        this.cdr.detectChanges();
      }
    });
  }

  bookingStatusLabel(status: string): string {
    return status.charAt(0) + status.slice(1).toLowerCase();
  }

  bookingStatusClass(status: string): string {
    switch (status) {
      case VendorBookingStatus.CONFIRMED: return 'badge-confirmed';
      case VendorBookingStatus.PENDING: return 'badge-pending';
      case VendorBookingStatus.COMPLETED: return 'badge-completed';
      case VendorBookingStatus.CANCELLED: return 'badge-cancelled';
      case VendorBookingStatus.REJECTED: return 'badge-rejected';
      default: return 'badge-pending';
    }
  }

  private startBookingsPolling(): void {
    this.stopBookingsPolling();
    if (!isPlatformBrowser(this.platformId)) return;
    this.bookingsPollId = setInterval(() => {
      this.loadMyBookings(true);
      this.loadBookingStatusSummary();
    }, this.bookingsPollMs);
  }

  private stopBookingsPolling(): void {
    if (this.bookingsPollId) {
      clearInterval(this.bookingsPollId);
      this.bookingsPollId = undefined;
    }
  }
}
