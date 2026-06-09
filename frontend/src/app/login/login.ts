import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { LoginRequest } from '../models/user.model';
import { APP_CONSTANTS } from '../constants/app-constants';
import { UserRole } from '../enums/user-role.enum';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
})
export class Login implements OnInit {
  loginGroup: FormGroup;
  isSubmitting = false;
  errorMessage = '';
  isAccountLocked = false;
  retryAfter: string | null = null;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.loginGroup = this.createForm();
  }

  ngOnInit() {
    console.log('Login initialized');

    // Redirect if already logged in — route by role
    if (this.authService.isAuthenticated()) {
      const user = this.authService.getCurrentUserValue();
      if (user?.role === UserRole.VENDOR) {
        this.router.navigate(['/vendor/dashboard']);
      } else if (user?.role === UserRole.ADMIN || user?.role === UserRole.SUPER_ADMIN) {
        this.router.navigate(['/admin/vendors']);
      } else {
        this.router.navigate(['/dashboard']);
      }
    }
  }

  /**
   * Create login form
   */
  private createForm(): FormGroup {
    return new FormGroup({
      email: new FormControl('', [Validators.required, Validators.email]),
      password: new FormControl('', [
        Validators.required,
        Validators.minLength(APP_CONSTANTS.PASSWORD_MIN_LENGTH)
      ])
    });
  }

  /**
   * Get form control
   */
  get email() {
    return this.loginGroup.get('email');
  }

  get password() {
    return this.loginGroup.get('password');
  }

  /**
   * Submit login form
   */
  onSubmit() {
    if (this.loginGroup.invalid) {
      this.loginGroup.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.errorMessage = '';
    this.isAccountLocked = false;
    this.retryAfter = null;

    const loginData: LoginRequest = {
      email: this.loginGroup.value.email!,
      password: this.loginGroup.value.password!
    };

    this.authService.login(loginData).subscribe({
      next: (response) => {
        console.log("Login successful:", response);

        // Role-based redirect
        const role = response.user?.roles?.[0];
        if (role === UserRole.VENDOR) {
          this.router.navigate(['/vendor/dashboard']);
        } else if (role === UserRole.ADMIN || role === UserRole.SUPER_ADMIN) {
          this.router.navigate(['/admin/vendors']);
        } else {
          // Get return URL from query params or default to dashboard
          const returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
          this.router.navigate([returnUrl]);
        }
      },
      error: (error) => {
        console.error("Login failed:", error);
        this.isSubmitting = false;

        // Handle different error types
        if (error.status === 423) {
          // Account locked
          this.isAccountLocked = true;
          this.errorMessage = "Account locked due to too many failed login attempts. Please try again later.";

          // Extract retry_after if available
          if (error.error?.retry_after) {
            this.retryAfter = new Date(error.error.retry_after).toLocaleString();
          }
        } else if (error.status === 401) {
          // Invalid credentials
          this.errorMessage = "Invalid email or password. Please try again.";
        } else if (error.status === 0) {
          // Browser-level network/CORS failure
          this.errorMessage = "Cannot reach the server. Check backend is running and CORS origin matches your frontend URL.";
        } else if (error.error?.message) {
          // Server error message
          this.errorMessage = error.error.message;
        } else {
          // Generic error
          this.errorMessage = "Login failed. Please try again.";
        }
      }
    });
  }
}
