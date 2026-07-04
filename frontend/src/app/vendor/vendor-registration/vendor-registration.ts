import { Component, Inject, OnInit, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { VendorService } from '../../services/vendor.service';
import { AuthService } from '../../services/auth.service';
import { VendorType } from '../../enums/vendor.enums';

@Component({
  selector: 'app-vendor-registration',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './vendor-registration.html',
  styleUrls: ['./vendor-registration.css']
})
export class VendorRegistration implements OnInit {
  currentStep = 1;
  totalSteps = 3;
  loading = false;
  error = '';
  success = false;

  vendorTypes = Object.values(VendorType);

  step1Form!: FormGroup;
  step2Form!: FormGroup;
  step3Form!: FormGroup;

  private isBrowser: boolean;

  constructor(
    private fb: FormBuilder,
    private vendorService: VendorService,
    private authService: AuthService,
    private router: Router,
    @Inject(PLATFORM_ID) platformId: Object
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
  }

  ngOnInit(): void {
    // Guard bypass safety: in SSR the route guard is skipped (isBrowser=false),
    // so we re-validate authentication on the client after hydration.
    if (this.isBrowser) {
      this.authService.restoreSession().subscribe(authenticated => {
        if (!authenticated) {
          this.router.navigate(['/login'], { queryParams: { returnUrl: '/vendor/register' } });
        }
      });
    }

    this.step1Form = this.fb.group({
      businessName: ['', [Validators.required, Validators.maxLength(255)]],
      vendorType: ['', Validators.required],
      registrationNumber: [''],
      taxId: ['']
    });

    this.step2Form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required]],
      websiteUrl: [''],
      addressLine1: ['', Validators.required],
      addressLine2: [''],
      city: ['', Validators.required],
      stateProvince: [''],
      countryCode: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(2)]],
      postalCode: ['']
    });

    this.step3Form = this.fb.group({
      description: ['']
    });
  }

  nextStep(): void {
    if (this.currentStep === 1 && this.step1Form.valid) this.currentStep = 2;
    else if (this.currentStep === 2 && this.step2Form.valid) this.currentStep = 3;
  }

  prevStep(): void {
    if (this.currentStep > 1) this.currentStep--;
  }

  submit(): void {
    if (!this.step3Form.valid) return;
    this.loading = true;
    this.error = '';

    const payload = {
      ...this.step1Form.value,
      ...this.step2Form.value,
      ...this.step3Form.value
    };

    this.vendorService.register(payload).subscribe({
      next: () => {
        this.success = true;
        this.loading = false;
        // The backend promotes the user to VENDOR role. Refresh the JWT so the
        // new role is reflected in the access token before entering vendor routes.
        this.authService.refreshToken().subscribe({
          next: () => {
            setTimeout(() => this.router.navigate(['/vendor/dashboard']), 2000);
          },
          error: () => {
            // Refresh failed — session may have just expired; send user to login.
            setTimeout(() => this.router.navigate(['/login'], {
              queryParams: { returnUrl: '/vendor/dashboard' }
            }), 2000);
          }
        });
      },
      error: (err) => {
        this.loading = false;
        // 401 means the session expired (or was never established via SSR bypass).
        // Redirect to login so the user can authenticate and try again.
        if (err?.status === 401) {
          this.router.navigate(['/login'], { queryParams: { returnUrl: '/vendor/register' } });
          return;
        }
        this.error = err?.error?.message || 'Registration failed. Please try again.';
      }
    });
  }

  get stepProgress(): number {
    return (this.currentStep / this.totalSteps) * 100;
  }

  isStepValid(step: number): boolean {
    if (step === 1) return this.step1Form.valid;
    if (step === 2) return this.step2Form.valid;
    return true;
  }

  goToDashboard(): void {
    this.router.navigate(['/vendor/dashboard']);
  }

  viewApplication(): void {
    // Navigate to vendor application status page or show application details
    this.router.navigate(['/vendor/application-status']);
  }

  trackByValue(index: number, value: string): string {
    return value;
  }

  // ─── Step indicator styling (left brand panel) ──────────────────
  stepDotClass(step: number): string {
    const base = 'flex h-9 w-9 shrink-0 items-center justify-center rounded-full border-2 z-10 transition-colors duration-300 text-sm font-bold';
    if (this.currentStep > step) return `${base} bg-emerald-400 border-emerald-400 text-white`;
    if (this.currentStep === step) return `${base} bg-white border-white text-indigo-700`;
    return `${base} bg-white/10 border-white/30 text-white/60`;
  }

  stepLabelClass(step: number): string {
    return this.currentStep === step ? 'text-sm font-semibold text-white' : 'text-sm font-semibold text-white/70';
  }

  stepDescClass(step: number): string {
    return this.currentStep === step ? 'text-xs text-indigo-100' : 'text-xs text-white/50';
  }

  stepConnectorClass(step: number): string {
    return this.currentStep > step
      ? 'w-0.5 flex-1 min-h-[20px] ml-[18px] bg-emerald-400 transition-colors duration-300'
      : 'w-0.5 flex-1 min-h-[20px] ml-[18px] bg-white/20 transition-colors duration-300';
  }
}

