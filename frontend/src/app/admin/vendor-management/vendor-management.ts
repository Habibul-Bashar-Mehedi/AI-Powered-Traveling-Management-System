import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
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

  // Pre-fetched caches — populated in parallel on init
  private pendingVendors: VendorProfile[] = [];
  private allVendors: VendorProfile[] = [];
  private payoutsCache: PayoutRequest[] = [];

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
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Preload pending + all vendors in parallel — tab switches will be instant
    this.preloadVendors();
  }

  // ── Tab helpers ──────────────────────────────────────────────

  getTabTitle(): string {
    switch (this.activeTab) {
      case 'pending': return 'Pending Review';
      case 'all':     return 'All Vendors';
      case 'payouts': return 'Payout Requests';
      default:        return 'Vendor Management';
    }
  }

  getTabSubtitle(): string {
    switch (this.activeTab) {
      case 'pending': return 'Review and approve new vendor applications';
      case 'all':     return 'Manage all registered vendors and their status';
      case 'payouts': return 'Process vendor payout requests and transactions';
      default:        return 'Manage vendors and payouts';
    }
  }

  // ── Data loading ─────────────────────────────────────────────

  /**
   * Preload pending + all vendors simultaneously.
   * After this resolves, tab switching is instant (no HTTP calls).
   */
  private preloadVendors(): void {
    this.loading = true;
    this.error = '';

    forkJoin({
      pending: this.adminVendorService.getPendingVendors(),
      all:     this.adminVendorService.getAllVendors()
    }).subscribe({
      next: ({ pending, all }) => {
        this.pendingVendors = pending || [];
        this.allVendors     = all     || [];
        this.loading        = false;
        // Render the currently active tab
        this.applyTabCache();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error   = err?.error?.message || 'Failed to load vendor data';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  /** Switch tab — shows pre-loaded data instantly, no HTTP call for vendors. */
  loadTab(tab: TabId): void {
    this.activeTab = tab;
    this.error     = '';

    if (tab === 'pending' || tab === 'all') {
      // Data already in memory — instant render
      this.applyTabCache();
      this.cdr.detectChanges();
      return;
    }

    // Payouts are loaded lazily (only when the tab is first opened)
    if (tab === 'payouts') {
      if (this.payoutsCache.length > 0) {
        this.payouts = this.payoutsCache;
        this.cdr.detectChanges();
        return;
      }
      this.loading = true;
      this.adminVendorService.getPendingPayouts().subscribe({
        next: (p) => {
          this.payoutsCache = p || [];
          this.payouts      = this.payoutsCache;
          this.loading      = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.error   = err?.error?.message || 'Failed to load payouts';
          this.loading = false;
          this.cdr.detectChanges();
        }
      });
    }
  }

  /** Sync the `vendors` array from the appropriate in-memory cache. */
  private applyTabCache(): void {
    if (this.activeTab === 'pending') {
      this.vendors = this.pendingVendors;
    } else if (this.activeTab === 'all') {
      this.vendors = this.allVendors;
    }
  }

  /**
   * After any mutation (approve / reject / suspend / reinstate),
   * refresh both caches silently in the background.
   */
  private refreshVendorCaches(): void {
    forkJoin({
      pending: this.adminVendorService.getPendingVendors(),
      all:     this.adminVendorService.getAllVendors()
    }).subscribe({
      next: ({ pending, all }) => {
        this.pendingVendors = pending || [];
        this.allVendors     = all     || [];
        this.applyTabCache();
        this.cdr.detectChanges();
      },
      error: () => { /* silent — stale cache is acceptable */ }
    });
  }

  // ── Vendor actions ────────────────────────────────────────────

  approve(id: string): void {
    this.actionLoading = id;
    this.adminVendorService.approveVendor(id).subscribe({
      next: () => {
        this.actionLoading = null;
        this.refreshVendorCaches();
      },
      error: (err) => {
        this.error         = err?.error?.message || 'Approval failed';
        this.actionLoading = null;
        this.cdr.detectChanges();
      }
    });
  }

  openReject(id: string): void {
    this.modalType     = 'reject';
    this.modalVendorId = id;
    this.modalReason   = '';
  }

  openSuspend(id: string): void {
    this.modalType     = 'suspend';
    this.modalVendorId = id;
    this.modalReason   = '';
  }

  closeModal(): void {
    this.modalType     = null;
    this.modalVendorId = null;
    this.modalReason   = '';
  }

  confirmModal(): void {
    if (!this.modalVendorId || !this.modalReason.trim()) return;
    const id         = this.modalVendorId;
    const reason     = this.modalReason;
    const actionType = this.modalType;
    this.actionLoading = id;
    this.closeModal();

    const obs = actionType === 'reject'
      ? this.adminVendorService.rejectVendor(id, reason)
      : this.adminVendorService.suspendVendor(id, reason);

    obs.subscribe({
      next: () => {
        this.actionLoading = null;
        this.refreshVendorCaches();
      },
      error: (err) => {
        this.error         = err?.error?.message || 'Action failed';
        this.actionLoading = null;
        this.cdr.detectChanges();
      }
    });
  }

  reinstate(id: string): void {
    this.actionLoading = id;
    this.adminVendorService.reinstateVendor(id).subscribe({
      next: () => {
        this.actionLoading = null;
        this.refreshVendorCaches();
      },
      error: (err) => {
        this.error         = err?.error?.message || 'Reinstate failed';
        this.actionLoading = null;
        this.cdr.detectChanges();
      }
    });
  }

  // ── Payout actions ────────────────────────────────────────────

  openPayoutModal(id: string, approve: boolean): void {
    this.payoutModalId  = id;
    this.payoutApprove  = approve;
    this.payoutNote     = '';
  }

  closePayoutModal(): void {
    this.payoutModalId = null;
    this.payoutApprove = null;
    this.payoutNote    = '';
  }

  confirmPayout(): void {
    if (!this.payoutModalId || this.payoutApprove === null) return;
    const id      = this.payoutModalId;
    const approve = this.payoutApprove;
    const note    = this.payoutNote;
    this.actionLoading = id;
    this.closePayoutModal();

    this.adminVendorService.processPayout(id, approve, note).subscribe({
      next: () => {
        this.actionLoading = null;
        this.payoutsCache  = [];   // invalidate payout cache
        this.loadTab('payouts');   // reload payouts
      },
      error: (err) => {
        this.error         = err?.error?.message || 'Payout processing failed';
        this.actionLoading = null;
        this.cdr.detectChanges();
      }
    });
  }

  // ── Utilities ─────────────────────────────────────────────────

  statusColor(status?: string): string {
    switch (status) {
      case 'APPROVED':      return '#10b981';
      case 'PENDING_REVIEW': return '#f59e0b';
      case 'PENDING':        return '#f59e0b';
      case 'REJECTED':       return '#f43f5e';
      case 'SUSPENDED':      return '#8b5cf6';
      default:               return '#8a9bbf';
    }
  }

  logout(): void {
    this.authService.logout().subscribe({ complete: () => this.router.navigate(['/login']) });
  }
}
