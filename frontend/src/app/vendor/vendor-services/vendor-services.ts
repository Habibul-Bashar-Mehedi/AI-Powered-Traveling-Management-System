import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { VendorService } from '../../services/vendor.service';
import { PackageItem, VendorServiceListing } from '../../models/vendor.model';
import { ServiceStatus, ServiceType, VendorType, PricingUnit, BookingMode, PackageItemType } from '../../enums/vendor.enums';
import { environment } from '../../../environments/environment';
import { DestinationService } from '../../services/destination.service';
import { Destination } from '../../models/destination.model';
import { Card } from '../../shared/card/card';
import { Modal } from '../../shared/modal/modal';
import { ConfirmDialogService } from '../../shared/confirm-dialog/confirm-dialog.service';

const VENDOR_TYPE_TO_SERVICE_TYPE: Record<VendorType, ServiceType> = {
  [VendorType.HOTEL]: ServiceType.HOTEL_ROOM,
  [VendorType.TOUR_GUIDE]: ServiceType.TOUR_PACKAGE,
  [VendorType.TRANSPORT]: ServiceType.TRANSPORT_ROUTE,
  [VendorType.TOURIST_SPOT]: ServiceType.TOURIST_SPOT,
  [VendorType.TRADITIONAL_FOOD]: ServiceType.TRADITIONAL_FOOD,
  [VendorType.TRADITIONAL_ITEM]: ServiceType.TRADITIONAL_ITEM,
  [VendorType.MARKET]: ServiceType.MARKET,
  [VendorType.DESTINATION]: ServiceType.DESTINATION,
  [VendorType.TRAVEL_PACKAGE]: ServiceType.TRAVEL_PACKAGE,
};

@Component({
  selector: 'app-vendor-services',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, Card, Modal],
  templateUrl: './vendor-services.html',
  styleUrls: ['../shared-vendor.css', './vendor-services.css']
})
export class VendorServices implements OnInit {
  services: VendorServiceListing[] = [];
  loading = false;   // false on SSR; set true in browser ngOnInit to avoid hydration mismatch
  vendorSuspended = false;
  showForm = false;
  editingId: string | null = null;
  submitting = false;
  error = '';

  imageUploading = false;
  imageError = '';
  listError = '';

  private static readonly MAX_IMAGE_BYTES = 5 * 1024 * 1024;
  private static readonly ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

  vendorType: VendorType | null = null;
  pricingUnits = Object.values(PricingUnit);
  bookingModes = Object.values(BookingMode);
  serviceStatuses = Object.values(ServiceStatus);
  packageItemTypes = Object.values(PackageItemType);
  destinations: Destination[] = [];
  detailServiceItem: VendorServiceListing | null = null;

  get allowedServiceType(): ServiceType | null {
    return this.vendorType ? VENDOR_TYPE_TO_SERVICE_TYPE[this.vendorType] : null;
  }

  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private vendorService: VendorService,
    private destinationService: DestinationService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private confirmDialog: ConfirmDialogService,
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
    this.vendorService.getProfile().subscribe({
      next: (v) => {
        this.vendorType = v.vendorType;
        this.vendorSuspended = v.status === 'SUSPENDED';
        this.cdr.markForCheck();
      },
      error: () => {
        // Non-critical — backend still enforces the suspension block server-side
      }
    });
    this.destinationService.getAll().subscribe({
      next: (destinations) => {
        this.destinations = destinations;
        this.cdr.markForCheck();
      },
      error: () => {
        // Non-critical — destination stays optional if this fails to load
      }
    });
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
      destinationId: [null],
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
    if (this.allowedServiceType) {
      this.form.patchValue({ serviceType: this.allowedServiceType });
    }
    this.packageItemsArray.clear();
    this.imageError = '';
    this.showForm = true;
  }

  openEdit(s: VendorServiceListing): void {
    this.editingId = s.serviceId || null;
    this.form.patchValue(s);
    if (this.allowedServiceType) {
      this.form.patchValue({ serviceType: this.allowedServiceType });
    }
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
      this.imageError = 'Unsupported image type. Use JPEG, PNG, or WEBP.';
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

  async delete(id: string): Promise<void> {
    const ok = await this.confirmDialog.confirm({
      title: 'Delete this service?',
      message: 'This cannot be undone. Any published listing will be removed from the catalog immediately.',
      confirmLabel: 'Delete Service',
      danger: true
    });
    if (!ok) return;

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

  trackByDestination(index: number, destination: Destination): number {
    return destination.id ?? index;
  }

  serviceShortDesc(s: VendorServiceListing): string {
    if (!s.description) return '';
    return s.description.length > 100 ? `${s.description.slice(0, 100)}…` : s.description;
  }

  serviceMetaPills(s: VendorServiceListing): string[] {
    const pills = [
      `${s.currencyCode} ${s.basePrice.toFixed(2)}`,
      s.pricingUnit,
      `Max ${s.maxCapacity}`,
    ];
    if (s.packageItems && s.packageItems.length > 0) {
      pills.push(`${s.packageItems.length} itinerary item${s.packageItems.length > 1 ? 's' : ''}`);
    }
    return pills;
  }

  openServiceDetail(s: VendorServiceListing): void {
    this.detailServiceItem = s;
  }

  closeServiceDetail(): void {
    this.detailServiceItem = null;
  }

  editFromDetail(s: VendorServiceListing): void {
    this.detailServiceItem = null;
    this.openEdit(s);
  }
}

