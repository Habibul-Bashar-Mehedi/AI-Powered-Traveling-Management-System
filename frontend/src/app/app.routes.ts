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
    path: 'verify-otp',
    loadComponent: () => import('./verify-otp/verify-otp').then(m => m.VerifyOtp)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./dashboard/dashboard').then(m => m.Dashboard),
    canActivate: [AuthGuard],
    runGuardsAndResolvers: 'always'
  },
  {
    path: 'profile',
    loadComponent: () => import('./profile/profile').then(m => m.Profile),
    canActivate: [AuthGuard]
  },
  {
    path: 'explore-nearby',
    loadComponent: () => import('./explore-nearby/explore-nearby').then(m => m.ExploreNearby),
    canActivate: [AuthGuard]
  },
  {
    path: 'payment/result',
    loadComponent: () => import('./payment-result/payment-result').then(m => m.PaymentResult),
    canActivate: [AuthGuard]
  },
  {
    path: 'payment/mock-checkout',
    loadComponent: () => import('./payment-mock-checkout/payment-mock-checkout').then(m => m.PaymentMockCheckout),
    canActivate: [AuthGuard]
  },
  {
    path: 'home',
    loadComponent: () => import('./home/home').then(m => m.Home)
  },
  {
    path: 'help-center',
    loadComponent: () => import('./info-page/info-page').then(m => m.InfoPage)
  },
  {
    path: 'privacy-policy',
    loadComponent: () => import('./info-page/info-page').then(m => m.InfoPage)
  },
  {
    path: 'terms-of-service',
    loadComponent: () => import('./info-page/info-page').then(m => m.InfoPage)
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
      },
      {
        path: 'users',
        loadComponent: () => import('./admin/user-management/user-management').then(m => m.UserManagement)
      },
      {
        path: 'banners',
        loadComponent: () => import('./admin/banner-management/banner-management').then(m => m.BannerManagement)
      },
      {
        path: 'packages',
        loadComponent: () => import('./admin/package-management/package-management').then(m => m.PackageManagement)
      },
      {
        path: 'bookings',
        loadComponent: () => import('./admin/admin-bookings/admin-bookings').then(m => m.AdminBookings)
      }
    ]
  },

  {
    path: '',
    redirectTo: 'home',
    pathMatch: 'full'
  },
  // Catch-all: redirect unknown paths to the homepage
  {
    path: '**',
    redirectTo: 'home'
  }
];
