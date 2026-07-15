import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PaymentService } from '../services/payment.service';
import { PaymentStatusDTO, SimulatedPaymentOutcome } from '../models/payment.model';

/**
 * The app's own clearly-labeled "test mode" checkout page — only ever reached
 * when SSLCommerz isn't configured (see PaymentServiceImpl#initiate). Original,
 * generic design; not a copy of any real gateway's layout, colors, or logos.
 * Lets the full paid/failed/cancelled flow be exercised without a live SSLCommerz
 * sandbox account. Once real credentials are added, this page is never reached —
 * initiate() routes straight to the real SSLCommerz hosted page instead.
 */
@Component({
  selector: 'app-payment-mock-checkout',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './payment-mock-checkout.html',
  styleUrls: ['./payment-mock-checkout.css']
})
export class PaymentMockCheckout implements OnInit {
  loading = true;
  error = '';
  status: PaymentStatusDTO | null = null;
  txId = '';
  resolving = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.route.queryParamMap.subscribe(params => {
      this.txId = params.get('txId') || '';
      if (!this.txId) {
        this.loading = false;
        this.error = 'Missing transaction reference.';
        this.cdr.detectChanges();
        return;
      }
      this.load();
    });
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.paymentService.getStatus(this.txId).subscribe({
      next: (status) => {
        this.status = status;
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Could not load this payment. It may have expired.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  resolve(outcome: SimulatedPaymentOutcome): void {
    if (this.resolving) return;
    this.resolving = true;
    this.cdr.detectChanges();
    this.paymentService.simulate({ txId: this.txId, outcome }).subscribe({
      next: (res) => {
        this.router.navigate(['/payment/result'], { queryParams: { txId: this.txId, status: res.status } });
      },
      error: (err) => {
        this.resolving = false;
        this.error = err?.error?.message || 'Could not resolve this simulated payment.';
        this.cdr.detectChanges();
      }
    });
  }
}
