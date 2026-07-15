import { PaymentMethod } from '../enums/vendor.enums';
import { VendorBooking } from './vendor.model';

export type PackageStatus = 'DRAFT' | 'PUBLISHED' | 'ARCHIVED';

export interface PackageComponent {
  componentId?: string;
  serviceId: string;
  serviceName?: string;
  serviceType?: string;
  basePrice?: number;
  currencyCode?: string;
  maxCapacity?: number;
  quantity: number;
  dayNumber?: number | null;
  sequence?: number;
  notes?: string;
}

export interface PackageExtra {
  extraId?: string;
  title: string;
  description?: string;
  price?: number;
  included?: boolean;
}

export interface TravelPackage {
  packageId?: string;
  name: string;
  description: string;
  imageUrl?: string;
  destinationId?: number | null;
  destinationName?: string;
  totalPrice: number;
  currencyCode?: string;
  status?: PackageStatus;
  createdAt?: string;
  updatedAt?: string;
  components: PackageComponent[];
  extras: PackageExtra[];
}

export interface PackageBookRequest {
  startDate: string;
  // Payment is no longer collected here — checkout happens via a real SSLCommerz
  // redirect after the package booking is reserved (see PayNowModal/PaymentService).
}

export interface PackageBooking {
  packageBookingId: string;
  packageId: string;
  packageName: string;
  startDate: string;
  totalGrossAmount: number;
  totalCommissionAmount: number;
  totalNetAmount: number;
  paymentStatus: string;
  paymentMethod?: PaymentMethod;
  paymentReference?: string;
  createdAt?: string;
  componentBookings: VendorBooking[];
}
