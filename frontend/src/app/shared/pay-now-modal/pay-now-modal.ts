import { ChangeDetectorRef, Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentService } from '../../services/payment.service';
import { PaymentBookingType } from '../../models/payment.model';

const BD_MOBILE_PATTERN = /^01[3-9][0-9]{8}$/;

/**
 * Original glassmorphism "Pay Now" panel — shown right after a booking is
 * created (now unpaid/PENDING). Mobile Banking is the only supported payment
 * method (Card/Net Banking were removed by request). Never collects PIN/OTP
 * data itself: it only kicks off a real SSLCommerz checkout session and
 * redirects the whole page to SSLCommerz's own hosted payment page, where the
 * user actually confirms the transaction with their mobile wallet.
 */
@Component({
  selector: 'app-pay-now-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './pay-now-modal.html',
  styleUrls: ['./pay-now-modal.css']
})
export class PayNowModal {
  @Input({ required: true }) bookingType!: PaymentBookingType;
  @Input({ required: true }) bookingId!: string;
  @Input({ required: true }) bookingReference!: string;
  @Input() merchantName = '';
  @Input({ required: true }) amount!: number;
  @Input() currencyCode = 'BDT';
  @Output() closed = new EventEmitter<void>();

  submitting = false;
  error = '';
  selectedProvider: string | null = null;
  mobileNumber = '';
  mobileNumberTouched = false;

  // Text/color badges only, no official logos (per the "original UI, no copied
  // gateway assets" requirement). These only narrow which screen SSLCommerz's
  // hosted page opens on; entry of any PIN/OTP always happens there, never here.
  readonly providerOptions: Array<{ value: string; label: string }> = [
    { value: 'BKASH', label: 'bKash' },
    { value: 'NAGAD', label: 'Nagad' },
    { value: 'ROCKET', label: 'Rocket' },
    { value: 'UPAY', label: 'Upay' },
  ];

  get isMobileNumberValid(): boolean {
    return BD_MOBILE_PATTERN.test(this.mobileNumber.trim());
  }

  get canProceed(): boolean {
    return !this.submitting && this.isMobileNumberValid;
  }

  constructor(private paymentService: PaymentService, private cdr: ChangeDetectorRef) {}

  selectProvider(provider: string): void {
    if (this.submitting) return;
    this.selectedProvider = this.selectedProvider === provider ? null : provider;
  }

  onMobileNumberBlur(): void {
    this.mobileNumberTouched = true;
  }

  payNow(): void {
    if (this.submitting) return;
    if (!this.isMobileNumberValid) {
      this.mobileNumberTouched = true;
      return;
    }
    this.submitting = true;
    this.error = '';
    this.paymentService.initiate({
      bookingType: this.bookingType,
      bookingId: this.bookingId,
      preferredMethod: 'MOBILE_BANK',
      preferredProvider: this.selectedProvider ?? undefined,
      contactPhone: this.mobileNumber.trim()
    }).subscribe({
      next: (res) => {
        window.location.href = res.gatewayPageUrl;
      },
      error: (err) => {
        this.submitting = false;
        this.error = err?.error?.message || 'Could not start checkout. Please try again.';
        this.cdr.detectChanges();
      }
    });
  }

  requestClose(): void {
    if (this.submitting) return;
    this.closed.emit();
  }
}
