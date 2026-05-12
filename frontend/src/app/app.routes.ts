import { Routes } from '@angular/router';
import { AuthGuard } from './guards/auth.guard';
import { VendorGuard } from './guards/vendor.guard';
import { AdminGuard } from './guards/admin.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./login/login').then(m => m.Login)
  },
  {
    path: 'registration',
    loadComponent: () => import('./registration/registration').then(m => m.Registration)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./dashboard/dashboard').then(m => m.Dashboard),
    canActivate: [AuthGuard]
  },
  {
    path: 'home',
    loadComponent: () => import('./home/home').then(m => m.Home),
    canActivate: [AuthGuard]
  },

  // ── Vendor Registration (authenticated user registers as vendor) ──────────
  {
    path: 'vendor/register',
    loadComponent: () => import('./vendor/vendor-registration/vendor-registration').then(m => m.VendorRegistration),
    canActivate: [AuthGuard]
  },

  // ── Vendor Dashboard shell + child routes ────────────────────────────────
  {
    path: 'vendor',
    loadComponent: () => import('./vendor/vendor-dashboard/vendor-dashboard').then(m => m.VendorDashboard),
    canActivate: [VendorGuard],
    children: [
      {
        path: '',
        redirectTo: 'dashboard',
        pathMatch: 'full'
      },
      {
        path: 'dashboard',
        loadComponent: () => import('./vendor/vendor-overview/vendor-overview').then(m => m.VendorOverview)
      },
      {
        path: 'services',
        loadComponent: () => import('./vendor/vendor-services/vendor-services').then(m => m.VendorServices)
      },
      {
        path: 'bookings',
        loadComponent: () => import('./vendor/vendor-bookings/vendor-bookings').then(m => m.VendorBookings)
      },
      {
        path: 'wallet',
        loadComponent: () => import('./vendor/vendor-wallet/vendor-wallet').then(m => m.VendorWallet)
      },
      {
        path: 'analytics',
        loadComponent: () => import('./vendor/vendor-analytics/vendor-analytics').then(m => m.VendorAnalytics)
      }
    ]
  },

  // ── Admin routes ──────────────────────────────────────────────────────────
  {
    path: 'admin',
    canActivate: [AdminGuard],
    children: [
      {
        path: '',
        redirectTo: 'vendors',
        pathMatch: 'full'
      },
      {
        path: 'vendors',
        loadComponent: () => import('./admin/vendor-management/vendor-management').then(m => m.VendorManagement)
      }
    ]
  },

  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full'
  }
];
