import { Component } from '@angular/core';
import { FormGroup, FormControl, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { min } from 'rxjs';

export interface RegistrationUser {
  fullname: string;
  email: string;
  role: string;
  password: string;
  confirmPassword: string;
  country?: string;
}

@Component({
  selector: 'app-registration',
  standalone: true,
  imports: [
    FormsModule,
    ReactiveFormsModule,
    CommonModule, 
    RouterLink
  ],
  templateUrl: './registration.html',
  styleUrls: ['./registration.css'],
})
export class Registration {

  constructor() {}

  ngOnInit() {
    console.log('Registration component initialized');
  }

  registrationUser: RegistrationUser = {
    fullname: '',
    email: '',
    role: '',
    password: '',
    confirmPassword: '',
    country: ''
  };

  regisstrationGroup = new FormGroup({  
    fullname: new FormControl(this.registrationUser.fullname, [
      Validators.required,
      Validators.minLength(6),
      Validators.maxLength(20)
    ]),
    email: new FormControl(this.registrationUser.email, [
      Validators.required,
      Validators.email
    ]),
    role: new FormControl(this.registrationUser.role, [
      Validators.required
    ]),
    password: new FormControl(this.registrationUser.password, [
      Validators.required,
      Validators.minLength(6),
      Validators.maxLength(100)
    ]),
    confirmPassword: new FormControl(this.registrationUser.confirmPassword, [
      Validators.required,
      Validators.minLength(6),
      Validators.maxLength(100)
    ]),
    country: new FormControl(this.registrationUser.country, [
      Validators.required,
      Validators.maxLength(40),
      Validators.minLength(2)
    ])

  }); 
  
  getFullName() {
    return this.regisstrationGroup.get('fullname')?.value;
  }

  getEmail() {
    return this.regisstrationGroup.get('email')?.value;
  }

  getRole() {
    return this.regisstrationGroup.get('role')?.value;
  }

  getPassword() {
    return this.regisstrationGroup.get('password')?.value;
  }

  getConfirmPassword() {
    return this.regisstrationGroup.get('confirmPassword')?.value;
  }   

  getCountry() {
    return this.regisstrationGroup.get('country')?.value;
  }

  onSubmit() {
    console.warn(this.regisstrationGroup.value); //TODO: Implement actual registration logic here 
  }
}