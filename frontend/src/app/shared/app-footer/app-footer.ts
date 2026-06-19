import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './app-footer.html'
})
export class FooterComponent {
  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  navigateToDashboard(event: Event): void {
    event.preventDefault();
    
    // Check if user is logged in and navigate to appropriate dashboard
    const currentUser = this.authService.getCurrentUserValue();
    if (currentUser) {
      const role = currentUser.role;
      if (role === 'VENDOR') {
        this.router.navigate(['/vendor/dashboard']);
      } else if (role === 'ADMIN') {
        this.router.navigate(['/admin/vendor-management']);
      } else {
        this.router.navigate(['/dashboard']);
      }
    } else {
      this.router.navigate(['/login']);
    }
  }
}
