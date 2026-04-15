import { Component } from '@angular/core';
import { FormControl, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

export interface LoginUser {
  email: string;
  password: string;
}

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterLink
],
  templateUrl: './login.html',
  styleUrls: ['./login.css'],
})

 

export class Login {
  constructor() {
    this.loginUser ={} as LoginUser;
   }
  ngOnInit() {
    console.log('Login component initialized');
  }

  loginUser: LoginUser = {
    email: '',
    password: ''
  };  

  loginGroup = new FormGroup({
    email: new FormControl(this.loginUser.email, [Validators.required, Validators.email]),
    password: new FormControl(this.loginUser.password, [
      Validators.required, Validators.minLength(6), Validators.maxLength(12)
    ])
  });

  getEmail() {
    return this.loginGroup.get('email')?.value;
  }

  getPassword() {
    return this.loginGroup.get('password')?.value;
  } 

  onSubmit() {
    console.warn(this.loginGroup.value);//TODO: Implement actual login logic here
  }
}
