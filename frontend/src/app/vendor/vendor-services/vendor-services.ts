import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { VendorService } from '../../services/vendor.service';
import { PackageItem, VendorServiceListing } from '../../models/vendor.model';
import { ServiceStatus, ServiceType, PricingUnit, BookingMode, PackageItemType } from '../../enums/vendor.enums';
import { environment } from '../../../environments/environment';

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

  imageUploading = false;
  imageError = '';
  listError = '';

  private static readonly MAX_IMAGE_BYTES = 5 * 1024 * 1024;
  private static readonly ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp', 'image/gif'];

  serviceTypes = Object.values(ServiceType);
  pricingUnits = Object.values(PricingUnit);
  bookingModes = Object.values(BookingMode);
  serviceStatuses = Object.values(ServiceStatus);
  packageItemTypes = Object.values(PackageItemType);

  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private vendorService: VendorService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {}

  get formImageUrl(): string {
    return this.form?.get('imageUrl')?.value || '';
  }

  get isPackage(): boolean {
    return this.form?.get('serviceType')?.value === ServiceType.TOUR_PACKAGE;
  }

  get packageItemsArray(): FormArray {
    return this.form.get('packageItems') as FormArray;
  }

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
      imageUrl: [''],
      cancellationPolicy: [''],
      tags: [''],
      packageItems: this.fb.array([]),
    });
  }

  private newPackageItemGroup(item?: PackageItem): FormGroup {
    return this.fb.group({
      itemId: [item?.itemId ?? null],
      itemType: [item?.itemType ?? PackageItemType.TRANSPORT, Validators.required],
      title: [item?.title ?? '', [Validators.required, Validators.maxLength(255)]],
      description: [item?.description ?? ''],
      dayNumber: [item?.dayNumber ?? null],
    });
  }

  addPackageItem(): void {
    this.packageItemsArray.push(this.newPackageItemGroup({
      itemType: PackageItemType.TRANSPORT,
      title: '',
      sequence: this.packageItemsArray.length
    }));
  }

  removePackageItem(index: number): void {
    this.packageItemsArray.removeAt(index);
  }

  trackByIndex(index: number): number {
    return index;
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
    this.packageItemsArray.clear();
    this.imageError = '';
    this.showForm = true;
  }

  openEdit(s: VendorServiceListing): void {
    this.editingId = s.serviceId || null;
    this.form.patchValue(s);
    this.packageItemsArray.clear();
    (s.packageItems ?? []).forEach(item => this.packageItemsArray.push(this.newPackageItemGroup(item)));
    this.imageError = '';
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.editingId = null;
    this.error = '';
    this.imageError = '';
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.imageError = '';

    if (!VendorServices.ALLOWED_IMAGE_TYPES.includes(file.type)) {
      this.imageError = 'Unsupported image type. Use JPEG, PNG, WEBP or GIF.';
      input.value = '';
      return;
    }
    if (file.size > VendorServices.MAX_IMAGE_BYTES) {
      this.imageError = 'Image must be 5MB or smaller.';
      input.value = '';
      return;
    }

    this.imageUploading = true;
    this.vendorService.uploadServiceImage(file).subscribe({
      next: (res) => {
        this.form.patchValue({ imageUrl: res.url });
        this.imageUploading = false;
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.imageError = err?.error?.message || 'Failed to upload image';
        this.imageUploading = false;
        this.cdr.markForCheck();
      }
    });
    input.value = '';
  }

  removeImage(): void {
    this.form.patchValue({ imageUrl: '' });
  }

  resolveImageUrl(url: string | null | undefined): string {
    if (!url) return '';
    if (/^https?:\/\//i.test(url)) return url;
    return `${environment.assetOrigin}${url}`;
  }

  submit(): void {
    if (this.form.invalid) return;
    this.submitting = true;
    this.error = '';

    const payload: VendorServiceListing = this.form.value;
    payload.packageItems = this.isPackage
      ? (payload.packageItems ?? []).map((item, index) => ({ ...item, sequence: index }))
      : [];

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
    this.listError = '';
    this.vendorService.deleteService(id).subscribe({
      next: () => this.loadServices(),
      error: (err) => {
        this.listError = err?.error?.message || 'Failed to delete this service.';
        this.cdr.markForCheck();
      }
    });
  }

  toggleStatus(s: VendorServiceListing, newStatus: string): void {
    this.listError = '';
    this.vendorService.toggleServiceStatus(s.serviceId!, newStatus).subscribe({
      next: () => this.loadServices(),
      error: (err) => {
        this.listError = err?.error?.message || 'Failed to update service status.';
        this.cdr.markForCheck();
      }
    });
  }

  statusBadgeClass(status?: ServiceStatus): string {
    switch (status) {
      case ServiceStatus.ACTIVE: return 'service-status-active';
      case ServiceStatus.INACTIVE: return 'service-status-inactive';
      default: return 'service-status-default';
    }
  }

  trackByService(index: number, service: VendorServiceListing): string {
    return service.serviceId ?? String(index);
  }

  trackByValue(index: number, value: string): string {
    return value;
  }
}

