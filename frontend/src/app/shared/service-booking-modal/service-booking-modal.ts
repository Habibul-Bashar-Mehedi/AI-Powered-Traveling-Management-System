import { ChangeDetectorRef, Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Modal } from '../modal/modal';
import { ServiceCatalogService } from '../../services/service-catalog.service';
import { PublicServiceListing, VendorBooking } from '../../models/vendor.model';
import { ServiceType } from '../../enums/vendor.enums';

/**
 * Reserves capacity on one VendorService — date/quantity picker, live availability
 * check, then a pessimistic-locked booking call. The booking is created PENDING/unpaid;
 * payment now happens via a separate real SSLCommerz redirect (PayNowModal), triggered
 * by the parent page after this emits `booked`. Extracted out of Dashboard so any page
 * (dashboard catalog cards, Explore Nearby tourist-spot tickets/food) books through the
 * exact same, already-tested flow.
 */
@Component({
  selector: 'app-service-booking-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, Modal],
  templateUrl: './service-booking-modal.html',
  styleUrls: ['./service-booking-modal.css']
})
export class ServiceBookingModal implements OnInit {
  @Input({ required: true }) service!: PublicServiceListing;
  @Output() closed = new EventEmitter<void>();
  @Output() booked = new EventEmitter<VendorBooking>();

  private static readonly DEFAULT_MAX_QUANTITY = 20;

  quantity = 1;
  notes = '';
  deliveryAddress = '';
  contactPhone = '';
  submitting = false;
  error = '';

  startDate = '';
  endDate = '';
  availability: number | null = null;
  availabilityLoading = false;
  availabilityError = '';
  private availabilityRequestId = 0;

  constructor(private cdr: ChangeDetectorRef, private serviceCatalogService: ServiceCatalogService) {}

  ngOnInit(): void {
    this.startDate = this.minBookingDate;
    this.onDateChange();
  }

  get isHotel(): boolean {
    return this.service.serviceType === ServiceType.HOTEL_ROOM;
  }

  get isDeliveredOrder(): boolean {
    return this.service.serviceType === ServiceType.TRADITIONAL_FOOD
      || this.service.serviceType === ServiceType.TRADITIONAL_ITEM;
  }

  get minBookingDate(): string {
    return new Date().toISOString().slice(0, 10);
  }

  get maxQuantity(): number {
    const serviceMax = this.service.maxCapacity && this.service.maxCapacity > 0
      ? this.service.maxCapacity
      : ServiceBookingModal.DEFAULT_MAX_QUANTITY;
    return this.availability !== null ? Math.min(serviceMax, this.availability) : serviceMax;
  }

  get unitLabel(): string {
    switch (this.service.pricingUnit) {
      case 'PER_NIGHT': return 'Night(s)';
      case 'PER_PERSON': return 'Traveler(s)';
      case 'PER_SEAT': return 'Seat(s)';
      case 'PER_TRIP': return 'Trip(s)';
      case 'PER_PLATE': return 'Plate(s)';
      case 'PER_COMBO_PACK': return 'Combo Pack(s)';
      default: return 'Unit(s)';
    }
  }

  get total(): number {
    return (this.service.basePrice ?? 0) * this.quantity;
  }

  get isDateValid(): boolean {
    if (!this.startDate) return false;
    if (this.isHotel) {
      return !!this.endDate && this.endDate >= this.startDate;
    }
    return true;
  }

  onDateChange(): void {
    this.error = '';
    if (!this.isDateValid) {
      this.availability = null;
      return;
    }
    this.refreshAvailability();
  }

  private refreshAvailability(): void {
    if (!this.isDateValid) return;

    const requestId = ++this.availabilityRequestId;
    this.availabilityLoading = true;
    this.availabilityError = '';
    this.cdr.detectChanges();

    const endDate = this.isHotel ? this.endDate : undefined;
    this.serviceCatalogService.getAvailability(this.service.serviceId, this.startDate, endDate).subscribe({
      next: (res) => {
        if (requestId !== this.availabilityRequestId) return;
        this.availabilityLoading = false;
        this.availability = res.available;
        this.setQuantity(this.quantity);
        this.cdr.detectChanges();
      },
      error: (err) => {
        if (requestId !== this.availabilityRequestId) return;
        this.availabilityLoading = false;
        this.availability = null;
        this.availabilityError = err?.error?.message || 'Could not check availability for this date.';
        this.cdr.detectChanges();
      }
    });
  }

  decrementQuantity(): void {
    this.setQuantity(this.quantity - 1);
  }

  incrementQuantity(): void {
    this.setQuantity(this.quantity + 1);
  }

  setQuantity(value: number): void {
    const max = Math.max(this.maxQuantity, 0);
    if (!Number.isFinite(value)) value = 1;
    this.quantity = max === 0 ? 0 : Math.min(Math.max(Math.round(value), 1), max);
  }

  get canConfirm(): boolean {
    return this.isDateValid
      && !this.availabilityLoading
      && this.availability !== null
      && this.availability > 0
      && this.quantity > 0
      && this.quantity <= this.availability
      && !this.submitting
      && (!this.isDeliveredOrder || (!!this.deliveryAddress.trim() && !!this.contactPhone.trim()));
  }

  confirm(): void {
    if (this.submitting) return;

    if (!this.isDateValid) {
      this.error = this.isHotel
        ? 'Please select a valid check-in and check-out date.'
        : 'Please select a booking date.';
      return;
    }
    if (this.availability === null) {
      this.error = 'Still checking availability for this date — please wait a moment.';
      return;
    }
    if (this.availability <= 0) {
      this.error = 'This service is fully booked for the selected date(s). Try another date.';
      return;
    }
    if (this.quantity > this.availability) {
      this.error = `Only ${this.availability} left for the selected date(s).`;
      return;
    }
    if (this.isDeliveredOrder && (!this.deliveryAddress.trim() || !this.contactPhone.trim())) {
      this.error = 'Please provide a delivery address and contact phone for this order.';
      return;
    }

    this.submitting = true;
    this.error = '';
    this.cdr.detectChanges();

    this.serviceCatalogService.bookService(this.service.serviceId, {
      startDate: this.startDate,
      endDate: this.isHotel ? this.endDate : undefined,
      quantity: this.quantity,
      specialRequests: this.notes.trim() || undefined,
      deliveryAddress: this.isDeliveredOrder ? this.deliveryAddress.trim() : undefined,
      contactPhone: this.isDeliveredOrder ? this.contactPhone.trim() : undefined
    }).subscribe({
      next: (booking) => {
        this.submitting = false;
        this.booked.emit(booking);
      },
      error: (err) => {
        this.submitting = false;
        this.error = err?.error?.message || 'Failed to book this service. Please try again.';
        // Someone else may have booked the remaining capacity between our last check and
        // this submit — re-check so the quantity cap on screen reflects reality.
        this.refreshAvailability();
        this.cdr.detectChanges();
      }
    });
  }

  packageItemIcon(itemType: string): string {
    switch (itemType) {
      case 'TRANSPORT': return '🚌';
      case 'HOTEL': return '🏨';
      case 'ACTIVITY': return '🎟️';
      case 'MEAL': return '🍽️';
      default: return '📍';
    }
  }

  requestClose(): void {
    if (this.submitting) return;
    this.closed.emit();
  }
}
