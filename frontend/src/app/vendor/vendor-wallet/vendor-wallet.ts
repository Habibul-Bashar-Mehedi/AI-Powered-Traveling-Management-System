import { ChangeDetectorRef, Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { VendorWalletService } from '../../services/vendor-wallet.service';
import { WalletSummary, PayoutRequest, WalletTransaction } from '../../models/vendor.model';
import { PayoutMethod } from '../../enums/vendor.enums';

@Component({
  selector: 'app-vendor-wallet',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './vendor-wallet.html',
  styleUrls: ['../shared-vendor.css', './vendor-wallet.css']
})
export class VendorWallet implements OnInit {
  wallet: WalletSummary | null = null;
  loading = false;
  showPayoutForm = false;
  payoutSubmitting = false;
  payoutError = '';
  payoutSuccess = false;

  payoutMethods = Object.values(PayoutMethod);
  payoutForm!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private walletService: VendorWalletService,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    this.payoutForm = this.fb.group({
      amount: ['', [Validators.required, Validators.min(50)]],
      payoutMethod: ['BANK_TRANSFER', Validators.required],
      payoutDetails: ['']
    });
    if (!isPlatformBrowser(this.platformId)) return;
    this.load();
  }

  private applyViewState(update: () => void): void {
    setTimeout(() => {
      update();
      this.cdr.markForCheck();
    }, 0);
  }

  load(): void {
    this.loading = true;
    this.walletService.getWalletSummary().subscribe({
      next: (w) => this.applyViewState(() => {
        this.wallet = w;
        this.loading = false;
      }),
      error: () => this.applyViewState(() => {
        this.loading = false;
      })
    });
  }

  requestPayout(): void {
    if (this.payoutForm.invalid) return;
    this.payoutSubmitting = true;
    this.payoutError = '';

    const req: PayoutRequest = this.payoutForm.value;
    this.walletService.requestPayout(req).subscribe({
      next: () => this.applyViewState(() => {
        this.payoutSubmitting = false;
        this.payoutSuccess = true;
        this.showPayoutForm = false;
        this.load();
      }),
      error: (err) => this.applyViewState(() => {
        this.payoutError = err?.error?.message || 'Payout request failed';
        this.payoutSubmitting = false;
      })
    });
  }

  trackByTransaction(index: number, tx: WalletTransaction): string {
    return tx.transactionId;
  }

  trackByValue(index: number, value: string): string {
    return value;
  }

}

