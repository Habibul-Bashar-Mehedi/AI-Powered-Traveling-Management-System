import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../services/auth.service';
import { MyBookings } from '../my-bookings/my-bookings';
import { BookingHistory } from '../booking-history/booking-history';
import { MySpending } from '../my-spending/my-spending';
import { SideDrawer } from '../shared/side-drawer/side-drawer';

type ProfileDrawer = 'bookings' | 'history' | 'spending' | null;

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, MyBookings, BookingHistory, MySpending, SideDrawer],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile {
  constructor(private authService: AuthService) {}

  activeDrawer: ProfileDrawer = null;

  get username(): string {
    return this.authService.getCurrentUserValue()?.username || 'Traveler';
  }

  get userInitials(): string {
    const parts = this.username.trim().split(/\s+/).filter(Boolean);
    if (parts.length === 0) return 'T';
    return parts.slice(0, 2).map(p => p[0]!.toUpperCase()).join('');
  }

  openDrawer(drawer: Exclude<ProfileDrawer, null>): void {
    this.activeDrawer = drawer;
  }

  closeDrawer(): void {
    this.activeDrawer = null;
  }
}
