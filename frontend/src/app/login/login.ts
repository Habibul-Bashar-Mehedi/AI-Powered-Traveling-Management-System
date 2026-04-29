import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { Auth } from '../services/auth'; 

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
})
export class Login implements OnInit {

  loginGroup = new FormGroup({
    email: new FormControl('', [Validators.required, Validators.email]),
    password: new FormControl('', [Validators.required, Validators.minLength(6)]),
    role: new FormControl('USER', [Validators.required])
  });

  constructor(private authService: Auth, private router: Router) {}

  ngOnInit() {
    console.log('Login initialized');
  }

  get email() { return this.loginGroup.get('email'); }
  get password() { return this.loginGroup.get('password'); }
  get role() { return this.loginGroup.get('role'); }

  onSubmit() {
    if (this.loginGroup.valid) {
      const loginData = {
        email: this.loginGroup.value.email!,
        password: this.loginGroup.value.password!,
        role: this.loginGroup.value.role!
      };

      this.authService.login(loginData).subscribe({
        next: (res: string) => {
          console.log("Backend Response:", res);
          if (res.includes("Successful")) {
            alert("Login Successful!");
            this.router.navigate(['/dashboard']);
          }
        },
        error: (err) => {
          console.error("Login Failed:", err);
          alert("Login failed! Error: " + (err.error || "Server Connection Issue"));
        }
      });
    } else {
      this.loginGroup.markAllAsTouched();
    }
  }
}
