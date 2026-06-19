import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminVendorService } from '../../services/admin-vendor.service';
import { VendorProfile, PayoutRequest } from '../../models/vendor.model';
import { VendorStatus } from '../../enums/vendor.enums';
import { AuthService } from '../../services/auth.service';
import { Router, RouterLink } from '@angular/router';
import { FooterComponent } from '../../shared/app-footer/app-footer';

type TabId = 'pending' | 'all' | 'payouts';

@Component({
  selector: 'app-vendor-management',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, FooterComponent],
  templateUrl: './vendor-management.html',
  styleUrls: ['./vendor-management.css']
})
export class VendorManagement implements OnInit {
  activeTab: TabId = 'pending';
  vendors: VendorProfile[] = [];
  payouts: PayoutRequest[] = [];
  loading = false;
  actionLoading: string | null = null;
  error = '';

  // Modal state
  modalType: 'reject' | 'suspend' | null = null;
  modalVendorId: string | null = null;
  modalReason = '';

  // Payout note modal
  payoutModalId: string | null = null;
  payoutApprove: boolean | null = null;
  payoutNote = '';

  VendorStatus = VendorStatus;

  constructor(
    private adminVendorService: AdminVendorService,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadTab('pending');
  }

  getTabTitle(): string {
    switch (this.activeTab) {
      case 'pending':
        return 'Pending Review';
      case 'all':
        return 'All Vendors';
      case 'payouts':
        return 'Payout Requests';
      default:
        return 'Vendor Management';
    }
  }

  getTabSubtitle(): string {
    switch (this.activeTab) {
      case 'pending':
        return 'Review and approve new vendor applications';
      case 'all':
        return 'Manage all registered vendors and their status';
      case 'payouts':
        return 'Process vendor payout requests and transactions';
      default:
        return 'Manage vendors and payouts';
    }
  }

  loadTab(tab: TabId): void {
    this.activeTab = tab;
    this.vendors = [];
    this.payouts = [];
    this.error = '';
    this.loading = true;

    if (tab === 'pending') {
      this.adminVendorService.getPendingVendors().subscribe({
        next: (v) => { this.vendors = v; this.loading = false; },
        error: () => { this.error = 'Failed to load pending vendors'; this.loading = false; }
      });
    } else if (tab === 'all') {
      this.adminVendorService.getAllVendors().subscribe({
        next: (v) => { this.vendors = v; this.loading = false; },
        error: () => { this.error = 'Failed to load vendors'; this.loading = false; }
      });
    } else if (tab === 'payouts') {
      this.adminVendorService.getPendingPayouts().subscribe({
        next: (p) => { this.payouts = p; this.loading = false; },
        error: () => { this.error = 'Failed to load payouts'; this.loading = false; }
      });
    }
  }

  approve(id: string): void {
    this.actionLoading = id;
    this.adminVendorService.approveVendor(id).subscribe({
      next: () => { this.actionLoading = null; this.loadTab(this.activeTab); },
      error: (err) => { this.error = err?.error?.message || 'Approval failed'; this.actionLoading = null; }
    });
  }

  openReject(id: string): void {
    this.modalType = 'reject';
    this.modalVendorId = id;
    this.modalReason = '';
  }

  openSuspend(id: string): void {
    this.modalType = 'suspend';
    this.modalVendorId = id;
    this.modalReason = '';
  }

  closeModal(): void {
    this.modalType = null;
    this.modalVendorId = null;
    this.modalReason = '';
  }

  confirmModal(): void {
    if (!this.modalVendorId || !this.modalReason.trim()) return;
    const id = this.modalVendorId;
    const reason = this.modalReason;
    const actionType = this.modalType;
    this.actionLoading = id;
    this.closeModal();

    const obs = actionType === 'reject'
      ? this.adminVendorService.rejectVendor(id, reason)
      : this.adminVendorService.suspendVendor(id, reason);

    obs.subscribe({
      next: () => { this.actionLoading = null; this.loadTab(this.activeTab); },
      error: (err) => { this.error = err?.error?.message || 'Action failed'; this.actionLoading = null; }
    });
  }

  reinstate(id: string): void {
    this.actionLoading = id;
    this.adminVendorService.reinstateVendor(id).subscribe({
      next: () => { this.actionLoading = null; this.loadTab(this.activeTab); },
      error: (err) => { this.error = err?.error?.message || 'Reinstate failed'; this.actionLoading = null; }
    });
  }

  openPayoutModal(id: string, approve: boolean): void {
    this.payoutModalId = id;
    this.payoutApprove = approve;
    this.payoutNote = '';
  }

  closePayoutModal(): void {
    this.payoutModalId = null;
    this.payoutApprove = null;
    this.payoutNote = '';
  }

  confirmPayout(): void {
    if (!this.payoutModalId || this.payoutApprove === null) return;
    const id = this.payoutModalId;
    const approve = this.payoutApprove;
    const note = this.payoutNote;
    this.actionLoading = id;
    this.closePayoutModal();

    this.adminVendorService.processPayout(id, approve, note).subscribe({
      next: () => { this.actionLoading = null; this.loadTab('payouts'); },
      error: (err) => { this.error = err?.error?.message || 'Payout processing failed'; this.actionLoading = null; }
    });
  }

  statusColor(status?: string): string {
    switch (status) {
      case 'APPROVED': return '#10b981';
      case 'PENDING_REVIEW': return '#f59e0b';
      case 'REJECTED': return '#f43f5e';
      case 'SUSPENDED': return '#8b5cf6';
      default: return '#8a9bbf';
    }
  }

  logout(): void {
    this.authService.logout().subscribe({ complete: () => this.router.navigate(['/login']) });
  }
}
