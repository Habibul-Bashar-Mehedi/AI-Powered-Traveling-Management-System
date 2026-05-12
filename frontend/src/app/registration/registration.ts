import { Component, OnInit } from '@angular/core';
import { FormGroup, FormControl, FormsModule, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { UserRole } from '../enums/user-role.enum';
import { RegisterRequest } from '../models/user.model';
import { VALIDATION_MESSAGES } from '../constants/validation-messages';
import { APP_CONSTANTS } from '../constants/app-constants';

@Component({
  selector: 'app-registration',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule, RouterLink],
  templateUrl: './registration.html',
  styleUrls: ['./registration.css'],
})
export class Registration implements OnInit {
  registrationGroup: FormGroup;
  isSubmitting = false;
  errorMessage = '';
  validationErrors: { [key: string]: string } = {};

  // Expose enums to template
  UserRole = UserRole;
  validationMessages = VALIDATION_MESSAGES;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    this.registrationGroup = this.createForm();
  }

  ngOnInit() {
    console.log('Registration component initialized');
  }

  /**
   * Create registration form
   */
  private createForm(): FormGroup {
    return new FormGroup({
      fullname: new FormControl('', [
        Validators.required,
        Validators.minLength(APP_CONSTANTS.USERNAME_MIN_LENGTH),
        Validators.maxLength(APP_CONSTANTS.USERNAME_MAX_LENGTH)
      ]),
      email: new FormControl('', [
        Validators.required,
        Validators.email
      ]),
      role: new FormControl(UserRole.USER, [Validators.required]),
      password: new FormControl('', [
        Validators.required,
        Validators.minLength(APP_CONSTANTS.PASSWORD_MIN_LENGTH),
        Validators.maxLength(APP_CONSTANTS.PASSWORD_MAX_LENGTH)
      ]),
      confirmPassword: new FormControl('', [Validators.required]),
      country: new FormControl('', [Validators.required])
    }, { validators: this.passwordMatchValidator });
  }

  /**
   * Custom validator for password matching
   */
  private passwordMatchValidator(control: AbstractControl): ValidationErrors | null {
    const group = control as FormGroup;
    const password = group.get('password')?.value;
    const confirmPassword = group.get('confirmPassword')?.value;
    return password === confirmPassword ? null : { passwordMismatch: true };
  }

  /**
   * Get form control
   */
  getControl(name: string): FormControl {
    return this.registrationGroup.get(name) as FormControl;
  }

  /**
   * Check if field has error
   */
  hasError(fieldName: string, errorType: string): boolean {
    const control = this.getControl(fieldName);
    return control.hasError(errorType) && (control.dirty || control.touched);
  }

  /**
   * Submit registration form
   */
  onSubmit() {
    if (this.registrationGroup.invalid) {
      this.registrationGroup.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.validationErrors = {};

    const formValue = this.registrationGroup.value;

    const registerRequest: RegisterRequest = {
      username: formValue.fullname,
      email: formValue.email,
      password: formValue.password,
      role: formValue.role as UserRole,
      countryId: formValue.country
    };

    this.authService.register(registerRequest).subscribe({
      next: (response) => {
        console.log('Registration successful:', response);

        // Role-based redirect after successful registration
        const role = response.user?.roles?.[0];
        if (role === UserRole.VENDOR) {
          this.router.navigate(['/vendor/dashboard']);
        } else if (role === UserRole.ADMIN) {
          this.router.navigate(['/admin/vendors']);
        } else {
          // USER role or default
          this.router.navigate(['/dashboard']);
        }
      },
      error: (error) => {
        console.error('Registration failed:', error);
        this.isSubmitting = false;

        // Handle validation errors
        if (error.status === 400 && error.error?.errors) {
          // Field-specific validation errors
          this.validationErrors = error.error.errors;
          this.errorMessage = 'Please correct the errors below.';
        } else if (error.status === 409) {
          // Email already exists
          this.errorMessage = 'An account with this email already exists.';
        } else if (error.error?.message) {
          // Server error message
          this.errorMessage = error.error.message;
        } else {
          // Generic error
          this.errorMessage = 'Registration failed. Please try again.';
        }
      }
    });
  }
}
