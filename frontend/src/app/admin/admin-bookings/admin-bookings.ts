import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AdminBookingService } from '../../services/admin-booking.service';
import { UserBookingStatusSummary } from '../../services/vendor-booking.service';
import { AuthService } from '../../services/auth.service';
import { VendorBooking } from '../../models/vendor.model';
import { VendorBookingStatus } from '../../enums/vendor.enums';
import { FooterComponent } from '../../shared/app-footer/app-footer';

@Component({
  selector: 'app-admin-bookings',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, FooterComponent],
  templateUrl: './admin-bookings.html',
  styleUrls: ['./admin-bookings.css']
})
export class AdminBookings implements OnInit {
  bookings: VendorBooking[] = [];
  summary: UserBookingStatusSummary | null = null;
  statusOptions = Object.values(VendorBookingStatus);
  selectedStatus: VendorBookingStatus | '' = '';
  loading = false;
  error = '';

  readonly VendorBookingStatus = VendorBookingStatus;

  constructor(
    private adminBookingService: AdminBookingService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // Skip authenticated API calls during SSR — tokens aren't available server-side,
    // and Angular's non-destructive hydration never re-runs ngOnInit on the client,
    // so an SSR-time failure here would leave the page stuck until the component
    // is destroyed and recreated by a later client-side navigation.
    if (!isPlatformBrowser(this.platformId)) return;
    this.loadSummary();
    this.loadBookings();
  }

  loadSummary(): void {
    this.adminBookingService.getBookingStatusSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.cdr.detectChanges();
      },
      error: () => this.cdr.detectChanges()
    });
  }

  loadBookings(): void {
    this.loading = true;
    this.error = '';
    const status = this.selectedStatus || undefined;

    this.adminBookingService.getAllBookings(status).subscribe({
      next: (bookings) => {
        this.bookings = bookings || [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load bookings';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  onStatusFilterChange(): void {
    this.loadBookings();
  }

  refresh(): void {
    this.loadSummary();
    this.loadBookings();
  }

  statusCount(status: VendorBookingStatus): number {
    return this.summary?.counts?.[status] ?? 0;
  }

  logout(): void {
    this.authService.logout().subscribe({ complete: () => this.router.navigate(['/login']) });
  }

  trackByBooking(index: number, booking: VendorBooking): string {
    return booking.bookingId ?? String(index);
  }

  trackByValue(index: number, value: string): string {
    return value;
  }
}
