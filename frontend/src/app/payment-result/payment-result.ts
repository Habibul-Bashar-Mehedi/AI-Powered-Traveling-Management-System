import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PaymentService } from '../services/payment.service';
import { PaymentStatusDTO } from '../models/payment.model';

@Component({
  selector: 'app-payment-result',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './payment-result.html',
  styleUrls: ['./payment-result.css']
})
export class PaymentResult implements OnInit {
  loading = true;
  error = '';
  status: PaymentStatusDTO | null = null;
  txId = '';

  constructor(
    private route: ActivatedRoute,
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

  get isPaid(): boolean {
    return this.status?.status === 'PAID';
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
}
