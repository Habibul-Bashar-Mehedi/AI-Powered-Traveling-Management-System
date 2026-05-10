import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth';
import { LoginRequest } from '../models/user.model';
import { VALIDATION_MESSAGES } from '../constants/validation-messages';
import { APP_CONSTANTS } from '../constants/app-constants';

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
  
  validationMessages = VALIDATION_MESSAGES;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {
    this.loginGroup = this.createForm();
  }

  ngOnInit() {
    console.log('Login initialized');
    
    // Redirect if already logged in
    if (this.authService.isLoggedIn()) {
      this.router.navigate(['/dashboard']);
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

    const loginData: LoginRequest = {
      email: this.loginGroup.value.email!,
      password: this.loginGroup.value.password!
    };

    this.authService.login(loginData).subscribe({
      next: (response: string) => {
        console.log("Backend Response:", response);
        if (response.includes("Successful")) {
          alert("Login Successful!");
          this.router.navigate(['/dashboard']);
        } else {
          this.errorMessage = response;
          this.isSubmitting = false;
        }
      },
      error: (error) => {
        console.error("Login Failed:", error);
        this.errorMessage = error.error || "Login failed. Please check your credentials.";
        this.isSubmitting = false;
      },
      complete: () => {
        this.isSubmitting = false;
      }
    });
  }
}
