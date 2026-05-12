import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { VendorService } from '../../services/vendor.service';
import { VendorServiceListing } from '../../models/vendor.model';
import { ServiceStatus, ServiceType, PricingUnit, BookingMode } from '../../enums/vendor.enums';

@Component({
  selector: 'app-vendor-services',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './vendor-services.html',
  styleUrls: ['../shared-vendor.css', './vendor-services.css']
})
export class VendorServices implements OnInit {
  services: VendorServiceListing[] = [];
  loading = false;   // false on SSR; set true in browser ngOnInit to avoid hydration mismatch
  showForm = false;
  editingId: string | null = null;
  submitting = false;
  error = '';

  serviceTypes = Object.values(ServiceType);
  pricingUnits = Object.values(PricingUnit);
  bookingModes = Object.values(BookingMode);
  serviceStatuses = Object.values(ServiceStatus);

  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private vendorService: VendorService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  ngOnInit(): void {
    this.buildForm();
    // Skip authenticated API calls during SSR to avoid NG0100 during hydration
    if (!isPlatformBrowser(this.platformId)) return;
    this.loadServices();
  }

  buildForm(): void {
    this.form = this.fb.group({
      serviceName: ['', [Validators.required, Validators.maxLength(255)]],
      serviceType: ['', Validators.required],
      description: ['', Validators.required],
      basePrice: ['', [Validators.required, Validators.min(0.01)]],
      currencyCode: ['USD'],
      pricingUnit: ['', Validators.required],
      maxCapacity: ['', [Validators.required, Validators.min(1)]],
      bookingMode: ['MANUAL'],
      confirmationWindow: [24],
      status: ['DRAFT'],
      locationAddress: [''],
      cancellationPolicy: [''],
      tags: [''],
    });
  }

  loadServices(): void {
    this.loading = true;
    this.vendorService.getServices().subscribe({
      next: (s) => {
        this.services = s;
        this.loading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.loading = false;
        // 404 means vendor profile not yet created — redirect to registration
        if (err?.status === 404) {
          this.router.navigate(['/vendor/register']);
          return;
        }
        this.cdr.markForCheck();
      }
    });
  }

  openCreate(): void {
    this.editingId = null;
    this.form.reset({ currencyCode: 'USD', bookingMode: 'MANUAL', confirmationWindow: 24, status: 'DRAFT' });
    this.showForm = true;
  }

  openEdit(s: VendorServiceListing): void {
    this.editingId = s.serviceId || null;
    this.form.patchValue(s);
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.editingId = null;
    this.error = '';
  }

  submit(): void {
    if (this.form.invalid) return;
    this.submitting = true;
    this.error = '';

    const payload: VendorServiceListing = this.form.value;

    const req = this.editingId
      ? this.vendorService.updateService(this.editingId, payload)
      : this.vendorService.createService(payload);

    req.subscribe({
      next: () => {
        this.submitting = false;
        this.closeForm();
        this.loadServices();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to save service';
        this.submitting = false;
        this.cdr.markForCheck();
      }
    });
  }

  delete(id: string): void {
    if (!confirm('Delete this service? This cannot be undone.')) return;
    this.vendorService.deleteService(id).subscribe({ complete: () => this.loadServices() });
  }

  toggleStatus(s: VendorServiceListing, newStatus: string): void {
    this.vendorService.toggleServiceStatus(s.serviceId!, newStatus).subscribe({ complete: () => this.loadServices() });
  }

  statusBadgeColor(status?: ServiceStatus): string {
    switch (status) {
      case ServiceStatus.ACTIVE: return '#10b981';
      case ServiceStatus.INACTIVE: return '#f59e0b';
      default: return '#8a9bbf';
    }
  }
}

