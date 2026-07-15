import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AdminVendorService, AdminVendorUpdate } from '../../services/admin-vendor.service';
import { VendorProfile, PayoutRequest, ReinstatementRequest } from '../../models/vendor.model';
import { VendorStatus, ReinstatementStatus, VendorType } from '../../enums/vendor.enums';
import { AuthService } from '../../services/auth.service';
import { Router, RouterLink } from '@angular/router';
import { FooterComponent } from '../../shared/app-footer/app-footer';

type TabId = 'pending' | 'all' | 'payouts' | 'reinstatements';

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
  reinstatementRequests: ReinstatementRequest[] = [];
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

  // Reinstatement review modal
  reinstateModalId: string | null = null;
  reinstateApprove: boolean | null = null;
  reinstateReason = '';

  // Edit vendor modal
  editVendorId: string | null = null;
  editBusinessName = '';
  editVendorType: VendorType | null = null;
  vendorTypes = Object.values(VendorType);

  VendorStatus = VendorStatus;
  ReinstatementStatus = ReinstatementStatus;

  constructor(
    private adminVendorService: AdminVendorService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    // Skip authenticated API calls during SSR — tokens aren't available server-side,
    // and Angular's non-destructive hydration never re-runs ngOnInit on the client,
    // so an SSR-time failure here would leave the dashboard stuck until the component
    // is destroyed and recreated by a later client-side navigation.
    if (!isPlatformBrowser(this.platformId)) return;
    this.loadTab('pending');
  }

  // ── Tab helpers ──────────────────────────────────────────────

  getTabTitle(): string {
    switch (this.activeTab) {
      case 'pending':        return 'Pending Review';
      case 'all':            return 'All Vendors';
      case 'payouts':        return 'Payout Requests';
      case 'reinstatements': return 'Reinstatement Requests';
      default:               return 'Vendor Management';
    }
  }

  getTabSubtitle(): string {
    switch (this.activeTab) {
      case 'pending':        return 'Review and approve new vendor applications';
      case 'all':            return 'Manage all registered vendors and their status';
      case 'payouts':        return 'Process vendor payout requests and transactions';
      case 'reinstatements': return 'Review suspended vendors\' requests to be reinstated';
      default:               return 'Manage vendors and payouts';
    }
  }

  // ── Data loading ─────────────────────────────────────────────

  /** Switch tab and always fetch fresh data for it from the backend. */
  loadTab(tab: TabId): void {
    this.activeTab = tab;
    this.error     = '';
    this.loading   = true;

    const onError = (err: any) => {
      this.error   = err?.error?.message || `Failed to load ${this.getTabTitle().toLowerCase()}`;
      this.loading = false;
      this.cdr.detectChanges();
    };

    if (tab === 'payouts') {
      this.adminVendorService.getPendingPayouts().subscribe({
        next: (payouts) => {
          this.payouts = payouts || [];
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: onError
      });
      return;
    }

    if (tab === 'reinstatements') {
      this.adminVendorService.getReinstatementRequests().subscribe({
        next: (requests) => {
          this.reinstatementRequests = requests || [];
          this.loading = false;
          this.cdr.detectChanges();
        },
        error: onError
      });
      return;
    }

    const vendors$ = tab === 'all'
      ? this.adminVendorService.getAllVendors()
      : this.adminVendorService.getPendingVendors();

    vendors$.subscribe({
      next: (vendors) => {
        this.vendors = vendors || [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: onError
    });
  }

  // ── Vendor actions ────────────────────────────────────────────

  approve(id: string): void {
    this.actionLoading = id;
    this.adminVendorService.approveVendor(id).subscribe({
      next: () => {
        this.actionLoading = null;
        this.loadTab(this.activeTab);
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
        this.loadTab(this.activeTab);
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
        this.loadTab(this.activeTab);
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
        this.loadTab('payouts');
      },
      error: (err) => {
        this.error         = err?.error?.message || 'Payout processing failed';
        this.actionLoading = null;
        this.cdr.detectChanges();
      }
    });
  }

  // ── Edit vendor actions ────────────────────────────────────────

  openEdit(v: VendorProfile): void {
    this.editVendorId = v.vendorId ?? null;
    this.editBusinessName = v.businessName;
    this.editVendorType = v.vendorType;
  }

  closeEdit(): void {
    this.editVendorId = null;
    this.editBusinessName = '';
    this.editVendorType = null;
  }

  confirmEdit(): void {
    if (!this.editVendorId) return;
    const id = this.editVendorId;
    const dto: AdminVendorUpdate = { businessName: this.editBusinessName, vendorType: this.editVendorType ?? undefined };
    this.actionLoading = id;
    this.closeEdit();

    this.adminVendorService.updateVendor(id, dto).subscribe({
      next: () => {
        this.actionLoading = null;
        this.loadTab(this.activeTab);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Update failed';
        this.actionLoading = null;
        this.cdr.detectChanges();
      }
    });
  }

  // ── Reinstatement request actions ──────────────────────────────

  openReinstateModal(id: string, approve: boolean): void {
    this.reinstateModalId  = id;
    this.reinstateApprove  = approve;
    this.reinstateReason   = '';
  }

  closeReinstateModal(): void {
    this.reinstateModalId = null;
    this.reinstateApprove = null;
    this.reinstateReason  = '';
  }

  confirmReinstateReview(): void {
    if (!this.reinstateModalId || this.reinstateApprove === null) return;
    const id      = this.reinstateModalId;
    const approve = this.reinstateApprove;
    const reason  = this.reinstateReason;
    this.actionLoading = id;
    this.closeReinstateModal();

    this.adminVendorService.reviewReinstatementRequest(id, approve, reason || undefined).subscribe({
      next: () => {
        this.actionLoading = null;
        this.loadTab('reinstatements');
      },
      error: (err) => {
        this.error         = err?.error?.message || 'Failed to review reinstatement request';
        this.actionLoading = null;
        this.cdr.detectChanges();
      }
    });
  }

  trackByReinstatement(index: number, request: ReinstatementRequest): string {
    return request.requestId ?? String(index);
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

  trackByVendor(index: number, vendor: VendorProfile): string {
    return vendor.vendorId ?? String(index);
  }

  trackByPayout(index: number, payout: PayoutRequest): string {
    return payout.payoutId ?? String(index);
  }
}
