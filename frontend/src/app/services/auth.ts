import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// LoginUser ইন্টারফেসটি এখানে ডিফাইন করা ভালো অথবা আলাদা ইন্টারফেস ফাইল থেকে ইমপোর্ট করুন
export interface LoginUser {
  email: string;
  password: string;
  role?: string;
}

@Injectable({
  providedIn: 'root'
})
export class Auth {
  private baseUrl = 'http://localhost:8080/api/auth/user';

  constructor(private http: HttpClient) {}


  login(credentials: LoginUser): Observable<string> {
    return this.http.post(`${this.baseUrl}/login`, credentials, {
      responseType: 'text'
    });
  }


  register(userData: any): Observable<any> {
    return this.http.post(`${this.baseUrl}/registar`, userData);
  }
}
