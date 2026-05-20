import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { VendorService } from '../../services/vendor.service';
import { VendorType } from '../../enums/vendor.enums';

@Component({
  selector: 'app-vendor-registration',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
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

  constructor(
    private fb: FormBuilder,
    private vendorService: VendorService,
    private router: Router
  ) {}

  ngOnInit(): void {
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
        setTimeout(() => this.router.navigate(['/vendor/dashboard']), 2000);
      },
      error: (err) => {
        this.error = err?.error?.message || 'Registration failed. Please try again.';
        this.loading = false;
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
}

