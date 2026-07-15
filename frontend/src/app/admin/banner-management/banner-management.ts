import { Component, OnInit, ChangeDetectorRef, Inject, PLATFORM_ID } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AdminBannerService } from '../../services/admin-banner.service';
import { AuthService } from '../../services/auth.service';
import { Banner } from '../../models/banner.model';
import { FooterComponent } from '../../shared/app-footer/app-footer';
import { environment } from '../../../environments/environment';

@Component({
  selector: 'app-banner-management',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, FooterComponent],
  templateUrl: './banner-management.html',
  styleUrls: ['./banner-management.css']
})
export class BannerManagement implements OnInit {
  banners: Banner[] = [];
  loading = false;
  error = '';
  actionLoading: string | null = null;

  showForm = false;
  editingId: string | null = null;
  submitting = false;
  formError = '';

  imageUploading = false;
  imageError = '';

  deleteTargetId: string | null = null;

  private static readonly MAX_IMAGE_BYTES = 5 * 1024 * 1024;
  private static readonly ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

  ctaTargets = [
    { value: 'offers', label: 'Offers & Packages section' },
    { value: 'services', label: 'Available Services section' },
    { value: 'destinations', label: 'Destinations section' },
  ];

  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    private adminBannerService: AdminBannerService,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef,
    @Inject(PLATFORM_ID) private platformId: Object
  ) {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(150)]],
      description: [''],
      imageUrl: [''],
      badgeText: ['Limited Time'],
      ctaLabel: ['Explore', [Validators.maxLength(40)]],
      ctaTarget: ['offers'],
      active: [true],
      startDate: [''],
      endDate: [''],
      displayOrder: [0]
    });
  }

  ngOnInit(): void {
    // Skip authenticated API calls during SSR — tokens aren't available server-side,
    // and Angular's non-destructive hydration never re-runs ngOnInit on the client,
    // so an SSR-time failure here would leave the page stuck until the component
    // is destroyed and recreated by a later client-side navigation.
    if (!isPlatformBrowser(this.platformId)) return;
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.adminBannerService.getAllBanners().subscribe({
      next: (banners) => {
        this.banners = banners || [];
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to load banners';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  get activeCount(): number {
    return this.banners.filter(b => b.active).length;
  }

  get formImageUrl(): string {
    return this.form.get('imageUrl')?.value || '';
  }

  openCreate(): void {
    this.editingId = null;
    this.form.reset({
      title: '', description: '', imageUrl: '', badgeText: 'Limited Time',
      ctaLabel: 'Explore', ctaTarget: 'offers', active: true, startDate: '', endDate: '', displayOrder: 0
    });
    this.formError = '';
    this.imageError = '';
    this.showForm = true;
  }

  openEdit(b: Banner): void {
    this.editingId = b.id || null;
    this.form.patchValue({
      title: b.title,
      description: b.description || '',
      imageUrl: b.imageUrl || '',
      badgeText: b.badgeText || '',
      ctaLabel: b.ctaLabel || 'Explore',
      ctaTarget: b.ctaTarget || 'offers',
      active: b.active ?? true,
      startDate: b.startDate ? b.startDate.substring(0, 10) : '',
      endDate: b.endDate ? b.endDate.substring(0, 10) : '',
      displayOrder: b.displayOrder ?? 0
    });
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

    if (!BannerManagement.ALLOWED_IMAGE_TYPES.includes(file.type)) {
      this.imageError = 'Unsupported image type. Use JPEG, PNG, or WEBP.';
      input.value = '';
      return;
    }
    if (file.size > BannerManagement.MAX_IMAGE_BYTES) {
      this.imageError = 'Image must be 5MB or smaller.';
      input.value = '';
      return;
    }

    this.imageUploading = true;
    this.adminBannerService.uploadBannerImage(file).subscribe({
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

    const raw = this.form.value;
    const payload: Banner = {
      ...raw,
      startDate: raw.startDate ? new Date(raw.startDate).toISOString() : undefined,
      endDate: raw.endDate ? new Date(raw.endDate).toISOString() : undefined,
      displayOrder: raw.displayOrder != null ? Number(raw.displayOrder) : 0
    };

    const req = this.editingId
      ? this.adminBannerService.updateBanner(this.editingId, payload)
      : this.adminBannerService.createBanner(payload);

    req.subscribe({
      next: () => {
        this.submitting = false;
        this.closeForm();
        this.load();
      },
      error: (err) => {
        this.formError = err?.error?.message || 'Failed to save banner';
        this.submitting = false;
        this.cdr.detectChanges();
      }
    });
  }

  toggleActive(b: Banner): void {
    if (!b.id) return;
    this.actionLoading = b.id;
    this.adminBannerService.setActive(b.id, !b.active).subscribe({
      next: () => {
        this.actionLoading = null;
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to update banner status';
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
    this.adminBannerService.deleteBanner(id).subscribe({
      next: () => {
        this.actionLoading = null;
        this.load();
      },
      error: (err) => {
        this.error = err?.error?.message || 'Failed to delete banner';
        this.actionLoading = null;
        this.cdr.detectChanges();
      }
    });
  }

  logout(): void {
    this.authService.logout().subscribe({ complete: () => this.router.navigate(['/login']) });
  }

  trackByBanner(index: number, banner: Banner): string {
    return banner.id ?? String(index);
  }
}
