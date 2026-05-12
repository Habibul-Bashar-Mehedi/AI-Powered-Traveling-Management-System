import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { VendorWalletService } from '../../services/vendor-wallet.service';
import { WalletSummary, PayoutRequest } from '../../models/vendor.model';
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
  loading = true;
  showPayoutForm = false;
  payoutSubmitting = false;
  payoutError = '';
  payoutSuccess = false;

  payoutMethods = Object.values(PayoutMethod);
  payoutForm!: FormGroup;

  constructor(private fb: FormBuilder, private walletService: VendorWalletService) {}

  ngOnInit(): void {
    this.payoutForm = this.fb.group({
      amount: ['', [Validators.required, Validators.min(50)]],
      payoutMethod: ['BANK_TRANSFER', Validators.required],
      payoutDetails: ['']
    });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.walletService.getWalletSummary().subscribe({
      next: (w) => { this.wallet = w; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  requestPayout(): void {
    if (this.payoutForm.invalid) return;
    this.payoutSubmitting = true;
    this.payoutError = '';

    const req: PayoutRequest = this.payoutForm.value;
    this.walletService.requestPayout(req).subscribe({
      next: () => {
        this.payoutSubmitting = false;
        this.payoutSuccess = true;
        this.showPayoutForm = false;
        this.load();
      },
      error: (err) => {
        this.payoutError = err?.error?.message || 'Payout request failed';
        this.payoutSubmitting = false;
      }
    });
  }

  txTypeColor(type: string): string {
    return type === 'CREDIT' ? '#10b981' : '#f43f5e';
  }
}

