import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AdminPackageService } from '../../services/admin-package.service';
import { AuthService } from '../../services/auth.service';
import { PackageComponent as PkgComponent, PackageExtra, PackageStatus, TravelPackage } from '../../models/package.model';
import { PublicServiceListing } from '../../models/vendor.model';
import { ServiceCatalogService } from '../../services/service-catalog.service';
import { DestinationService } from '../../services/destination.service';
import { Destination } from '../../models/destination.model';
import { FooterComponent } from '../../shared/app-footer/app-footer';
import { Card } from '../../shared/card/card';
import { Modal } from '../../shared/modal/modal';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-package-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, FooterComponent, Card, Modal],
  templateUrl: './package-management.html',
  styleUrls: ['./package-management.css']
})
export class PackageManagement implements OnInit {
  packages: TravelPackage[] = [];
  loading = false;
  error = '';
  actionLoading: string | null = null;

  activeServices: PublicServiceListing[] = [];
  destinations: Destination[] = [];

  showForm = false;
  editingId: string | null = null;
  submitting = false;
  formError = '';

  imageUploading = false;
  imageError = '';

  deleteTargetId: string | null = null;

  private static readonly MAX_IMAGE_BYTES = 5 * 1024 * 1024;
  private static readonly ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private adminPackageService: AdminPackageService,
    private serviceCatalogService: ServiceCatalogService,
    private destinationService: DestinationService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.buildForm();
  }

  ngOnInit(): void {
    if (!isPlatformBrowser(this.platformId)) return;
    this.load();
    this.loadActiveServices();
    this.destinationService.getAll().subscribe({
      next: (destinations) => { this.destinations = destinations; this.cdr.detectChanges(); },
      error: () => {}
    });
  }

  private buildForm(): void {
    this.form = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(150)]],
      description: ['', Validators.required],
      imageUrl: [''],
      destinationId: [null],
      totalPrice: ['', [Validators.required, Validators.min(0.01)]],
      currencyCode: ['BDT'],
      components: this.fb.array([]),
      extras: this.fb.array([]),
    });
  }

  get componentsArray(): FormArray {
    return this.form.get('components') as FormArray;
  }

  get extrasArray(): FormArray {
    return this.form.get('extras') as FormArray;
  }

  private newComponentGroup(c?: PkgComponent): FormGroup {
    return this.fb.group({
      componentId: [c?.componentId ?? null],
      serviceId: [c?.serviceId ?? '', Validators.required],
      quantity: [c?.quantity ?? 1, [Validators.required, Validators.min(1)]],
      dayNumber: [c?.dayNumber ?? null],
      sequence: [c?.sequence ?? 0],
      notes: [c?.notes ?? ''],
    });
  }

  private newExtraGroup(e?: PackageExtra): FormGroup {
    return this.fb.group({
      extraId: [e?.extraId ?? null],
      title: [e?.title ?? '', [Validators.required, Validators.maxLength(150)]],
      description: [e?.description ?? ''],
      price: [e?.price ?? null],
      included: [e?.included ?? true],
    });
  }

  addComponent(): void {
    this.componentsArray.push(this.newComponentGroup({ serviceId: '', quantity: 1, sequence: this.componentsArray.length } as PkgComponent));
  }

  removeComponent(index: number): void {
    this.componentsArray.removeAt(index);
  }

  addExtra(): void {
    this.extrasArray.push(this.newExtraGroup());
  }

  removeExtra(index: number): void {
    this.extrasArray.removeAt(index);
  }

  serviceLabel(serviceId: string): string {
    const service = this.activeServices.find(s => s.serviceId === serviceId);
    return service ? `${service.serviceName} (${service.serviceType}) — ${service.currencyCode} ${service.basePrice}` : 'Select a service…';
  }

  trackByIndex(index: number): number {
    return index;
  }

  trackByPackage(index: number, pkg: TravelPackage): string {
    return pkg.packageId ?? String(index);
  }

  trackByDestination(index: number, destination: Destination): number {
    return destination.id ?? index;
  }

  private loadActiveServices(): void {
    this.serviceCatalogService.getActiveServices(0, 100).subscribe({
      next: (page) => { this.activeServices = page.content; this.cdr.detectChanges(); },
      error: () => {}
    });
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.adminPackageService.getAllPackages().subscribe({
      next: (packages) => {
        this.packages = packages || [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load packages';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  get formImageUrl(): string {
    return this.form.get('imageUrl')?.value || '';
  }

  packageSubtitle(pkg: TravelPackage): string {
    return `${pkg.currencyCode} ${pkg.totalPrice} · ${pkg.components.length} component${pkg.components.length === 1 ? '' : 's'}`;
  }

  openCreate(): void {
    this.editingId = null;
    this.buildForm();
    this.formError = '';
    this.imageError = '';
    this.showForm = true;
  }

  openEdit(pkg: TravelPackage): void {
    this.editingId = pkg.packageId || null;
    this.buildForm();
    this.form.patchValue({
      name: pkg.name,
      description: pkg.description,
      imageUrl: pkg.imageUrl || '',
      destinationId: pkg.destinationId ?? null,
      totalPrice: pkg.totalPrice,
      currencyCode: pkg.currencyCode || 'BDT',
    });
    (pkg.components || []).forEach(c => this.componentsArray.push(this.newComponentGroup(c)));
    (pkg.extras || []).forEach(e => this.extrasArray.push(this.newExtraGroup(e)));
    this.formError = '';
    this.imageError = '';
    this.showForm = true;
  }

  closeForm(): void {
    this.showForm = false;
    this.editingId = null;
    this.formError = '';
    this.imageError = '';
  }

  onImageSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    this.imageError = '';

    if (!PackageManagement.ALLOWED_IMAGE_TYPES.includes(file.type)) {
      this.imageError = 'Unsupported image type. Use JPEG, PNG, or WEBP.';
      input.value = '';
      return;
    }
    if (file.size > PackageManagement.MAX_IMAGE_BYTES) {
      this.imageError = 'Image must be 5MB or smaller.';
      input.value = '';
      return;
    }

    this.imageUploading = true;
    this.adminPackageService.uploadImage(file).subscribe({
      next: (res) => {
        this.form.patchValue({ imageUrl: res.url });
        this.imageUploading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.imageError = err?.error?.message || 'Failed to upload image';
        this.imageUploading = false;
        this.cdr.detectChanges();
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
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.formError = '';

    const payload: TravelPackage = this.form.value;

    const req = this.editingId
      ? this.adminPackageService.updatePackage(this.editingId, payload)
      : this.adminPackageService.createPackage(payload);

    req.subscribe({
      next: () => {
        this.submitting = false;
        this.closeForm();
        this.load();
      },
      error: (err) => {
        this.formError = err?.error?.message || 'Failed to save package';
        this.submitting = false;
        this.cdr.detectChanges();
      }
    });
  }

  setStatus(pkg: TravelPackage, status: PackageStatus): void {
    if (!pkg.packageId) return;
    this.actionLoading = pkg.packageId;
    this.error = '';
    this.adminPackageService.setStatus(pkg.packageId, status).subscribe({
      next: () => {
        this.actionLoading = null;
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to update package status';
        this.actionLoading = null;
        this.cdr.detectChanges();
      }
    });
  }

  openDeleteConfirm(id: string): void {
    this.deleteTargetId = id;
  }

  closeDeleteConfirm(): void {
    this.deleteTargetId = null;
  }

  confirmDelete(): void {
    if (!this.deleteTargetId) return;
    const id = this.deleteTargetId;
    this.actionLoading = id;
    this.closeDeleteConfirm();
    this.adminPackageService.deletePackage(id).subscribe({
      next: () => {
        this.actionLoading = null;
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete package';
        this.actionLoading = null;
        this.cdr.detectChanges();
      }
    });
  }

  logout(): void {
    this.authService.logout().subscribe({ complete: () => this.router.navigate(['/login']) });
  }
}
