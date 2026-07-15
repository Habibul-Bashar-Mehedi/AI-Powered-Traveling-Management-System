import { Component } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-footer',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './app-footer.html',
  styleUrls: ['./app-footer.css'],
})
export class FooterComponent {
  readonly currentYear = new Date().getFullYear();
  shortDescription = 'SMTS — helping travelers plan smarter trips and helping local businesses reach them.';

  constructor(
    private router: Router,
    private authService: AuthService
  ) {}

  navigateToDashboard(event: Event): void {
    event.preventDefault();

    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login']);
      return;
    }

    const user = this.authService.getCurrentUserValue();
    this.router.navigate([this.authService.getPostAuthRedirectUrl(user?.role)]);
  }
}
