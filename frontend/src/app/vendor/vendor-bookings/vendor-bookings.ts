import { ChangeDetectorRef, Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { VendorBookingService } from '../../services/vendor-booking.service';
import { VendorBooking } from '../../models/vendor.model';
import { VendorBookingStatus } from '../../enums/vendor.enums';

@Component({
  selector: 'app-vendor-bookings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './vendor-bookings.html',
  styleUrls: ['../shared-vendor.css', './vendor-bookings.css']
})
export class VendorBookings implements OnInit {
  bookings: VendorBooking[] = [];
  loading = false;
  selectedStatus: VendorBookingStatus | '' = '';
  actionLoading: string | null = null;
  rejectReason = '';
  rejectingId: string | null = null;
  error = '';

  statuses = ['', ...Object.values(VendorBookingStatus)];

  constructor(
    private bookingService: VendorBookingService,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.load();
  }

  private applyViewState(update: () => void): void {
    setTimeout(() => {
      update();
      this.cdr.markForCheck();
    });
  }

  load(): void {
    this.loading = true;
    const status = this.selectedStatus ? this.selectedStatus as VendorBookingStatus : undefined;
    this.bookingService.getBookings(status).subscribe({
      next: (b) => this.applyViewState(() => {
        this.bookings = b;
        this.loading = false;
      }),
      error: () => this.applyViewState(() => {
        this.loading = false;
      })
    });
  }

  onStatusFilter(): void { this.load(); }

  confirm(id: string): void {
    this.actionLoading = id;
    this.bookingService.confirmBooking(id).subscribe({
      next: () => this.applyViewState(() => {
        this.actionLoading = null;
        this.load();
      }),
      error: () => this.applyViewState(() => {
        this.actionLoading = null;
      })
    });
  }

  startReject(id: string): void { this.rejectingId = id; this.rejectReason = ''; }

  confirmReject(): void {
    if (!this.rejectingId) return;
    this.actionLoading = this.rejectingId;
    this.bookingService.rejectBooking(this.rejectingId, this.rejectReason).subscribe({
      next: () => this.applyViewState(() => {
        this.actionLoading = null;
        this.rejectingId = null;
        this.load();
      }),
      error: () => this.applyViewState(() => {
        this.actionLoading = null;
      })
    });
  }

  get pendingCount(): number {
    return this.bookings.filter((b) => b.bookingStatus === VendorBookingStatus.PENDING).length;
  }

  get confirmedCount(): number {
    return this.bookings.filter((b) => b.bookingStatus === VendorBookingStatus.CONFIRMED).length;
  }

  get totalNetAmount(): number {
    return this.bookings.reduce((sum, booking) => sum + (booking.netAmount || 0), 0);
  }

  statusBadgeClass(status: VendorBookingStatus): string {
    switch (status) {
      case VendorBookingStatus.CONFIRMED: return 'status-badge-confirmed';
      case VendorBookingStatus.PENDING: return 'status-badge-pending';
      case VendorBookingStatus.COMPLETED: return 'status-badge-completed';
      case VendorBookingStatus.CANCELLED: return 'status-badge-cancelled';
      case VendorBookingStatus.REJECTED: return 'status-badge-rejected';
      default: return 'status-badge-cancelled';
    }
  }

  trackByBooking(index: number, booking: VendorBooking): string {
    return booking.bookingId;
  }

  trackByValue(index: number, value: string): string {
    return value;
  }
}

