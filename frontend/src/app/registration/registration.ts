import { Component, OnInit } from '@angular/core';
import { FormGroup, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import {Auth} from '../services/auth';


@Component({
  selector: 'app-registration',
  standalone: true,
  imports: [FormsModule, ReactiveFormsModule, CommonModule, RouterLink],
  templateUrl: './registration.html',
  styleUrls: ['./registration.css'],
})
export class Registration implements OnInit {

  constructor(private authService: Auth, private router: Router) {}

  ngOnInit() {
    console.log('Registration component initialized');
  }

  regisstrationGroup = new FormGroup({
    fullname: new FormControl('', [Validators.required, Validators.minLength(6), Validators.maxLength(20)]),
    email: new FormControl('', [Validators.required, Validators.email]),
    role: new FormControl('user', [Validators.required]),
    password: new FormControl('', [Validators.required, Validators.minLength(8)]),
    confirmPassword: new FormControl('', [Validators.required]),
    country: new FormControl('', [Validators.required])
  });

  onSubmit() {
    if (this.regisstrationGroup.valid) {
      const formVal = this.regisstrationGroup.value;

      // পাসওয়ার্ড ম্যাচিং চেক
      if (formVal.password !== formVal.confirmPassword) {
        alert("Passwords do not match!");
        return;
      }

      // আপনার Spring Boot User Entity অনুযায়ী ডাটা ম্যাপিং
      const payload = {
        username: formVal.fullname, // এনটিটিতে username আছে
        email: formVal.email,
        password: formVal.password,
        role: formVal.role,
        countryId: formVal.country // এনটিটিতে countryId আছে
      };

      this.authService.register(payload).subscribe({
        next: (res) => {
          console.log('Success:', res);
          alert('Registration Successful!');
          this.router.navigate(['/login']);
        },
        error: (err) => {
          console.error('Error:', err);
          alert('Registration failed! Please try again.');
        }
      });
    } else {
      this.regisstrationGroup.markAllAsTouched();
    }
  }
}
