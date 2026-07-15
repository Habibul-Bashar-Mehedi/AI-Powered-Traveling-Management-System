import { ChangeDetectionStrategy, Component, OnDestroy, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../services/auth.service';

const RESEND_COOLDOWN_SECONDS = 45;

@Component({
  selector: 'app-verify-otp',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './verify-otp.html',
  styleUrls: ['./verify-otp.css'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class VerifyOtp implements OnDestroy {
  otpGroup: FormGroup;
  email: string;

  isSubmitting = signal(false);
  isResending = signal(false);
  errorMessage = signal('');
  infoMessage = signal('');
  cooldownSeconds = signal(0);

  private cooldownTimer: ReturnType<typeof setInterval> | undefined;

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.email = this.route.snapshot.queryParams['email'] ?? '';
    this.otpGroup = new FormGroup({
      otp: new FormControl('', [Validators.required, Validators.pattern(/^\d{6}$/)])
    });
  }

  ngOnDestroy(): void {
    if (this.cooldownTimer) {
      clearInterval(this.cooldownTimer);
    }
  }

  onSubmit(): void {
    if (this.otpGroup.invalid) {
      this.otpGroup.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.errorMessage.set('');
    this.infoMessage.set('');

    const otp = this.otpGroup.value.otp as string;

    this.authService.verifyOtp(this.email, otp).subscribe({
      next: (response) => {
        this.router.navigate([this.authService.getPostAuthRedirectUrl(response.user?.roles?.[0])]);
      },
      error: (error: HttpErrorResponse) => {
        this.isSubmitting.set(false);
        this.errorMessage.set(this.mapErrorMessage(error));
      }
    });
  }

  onResend(): void {
    if (this.cooldownSeconds() > 0 || this.isResending()) {
      return;
    }

    this.isResending.set(true);
    this.errorMessage.set('');
    this.infoMessage.set('');

    this.authService.resendOtp(this.email).subscribe({
      next: (response) => {
        this.isResending.set(false);
        this.infoMessage.set(response.message);
        this.startCooldown(RESEND_COOLDOWN_SECONDS);
      },
      error: (error: HttpErrorResponse) => {
        this.isResending.set(false);
        if (error.status === 429) {
          this.startCooldown(RESEND_COOLDOWN_SECONDS);
        }
        this.errorMessage.set(this.mapErrorMessage(error));
      }
    });
  }

  private startCooldown(seconds: number): void {
    this.cooldownSeconds.set(seconds);
    if (this.cooldownTimer) {
      clearInterval(this.cooldownTimer);
    }
    this.cooldownTimer = setInterval(() => {
      const remaining = this.cooldownSeconds() - 1;
      if (remaining <= 0) {
        this.cooldownSeconds.set(0);
        clearInterval(this.cooldownTimer);
      } else {
        this.cooldownSeconds.set(remaining);
      }
    }, 1000);
  }

  private mapErrorMessage(error: HttpErrorResponse): string {
    const code = error.error?.error;
    switch (code) {
      case 'OTP_EXPIRED':
        return 'This code has expired. Request a new one below.';
      case 'OTP_INVALID':
        return 'That code is incorrect. Please try again.';
      case 'OTP_MAX_ATTEMPTS':
        return 'Too many incorrect attempts. Please request a new code.';
      case 'OTP_RESEND_COOLDOWN':
        return 'Please wait before requesting another code.';
      default:
        return error.error?.message ?? 'Something went wrong. Please try again.';
    }
  }
}
