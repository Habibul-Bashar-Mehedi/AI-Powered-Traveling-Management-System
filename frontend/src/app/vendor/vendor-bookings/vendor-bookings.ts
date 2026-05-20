import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
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
  loading = true;
  selectedStatus: VendorBookingStatus | '' = '';
  actionLoading: string | null = null;
  rejectReason = '';
  rejectingId: string | null = null;
  error = '';

  statuses = ['', ...Object.values(VendorBookingStatus)];

  constructor(private bookingService: VendorBookingService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    const status = this.selectedStatus ? this.selectedStatus as VendorBookingStatus : undefined;
    this.bookingService.getBookings(status).subscribe({
      next: (b) => { this.bookings = b; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  onStatusFilter(): void { this.load(); }

  confirm(id: string): void {
    this.actionLoading = id;
    this.bookingService.confirmBooking(id).subscribe({
      next: () => { this.actionLoading = null; this.load(); },
      error: () => { this.actionLoading = null; }
    });
  }

  startReject(id: string): void { this.rejectingId = id; this.rejectReason = ''; }

  confirmReject(): void {
    if (!this.rejectingId) return;
    this.actionLoading = this.rejectingId;
    this.bookingService.rejectBooking(this.rejectingId, this.rejectReason).subscribe({
      next: () => { this.actionLoading = null; this.rejectingId = null; this.load(); },
      error: () => { this.actionLoading = null; }
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
}

