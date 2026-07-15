import {
  VendorType, VendorStatus, ServiceType, ServiceStatus, BookingMode,
  PricingUnit, VendorBookingStatus, PayoutMethod, PayoutStatus, TransactionType, PaymentMethod, PackageItemType,
  ReinstatementStatus
} from '../enums/vendor.enums';

export interface PackageItem {
  itemId?: string;
  itemType: PackageItemType;
  title: string;
  description?: string;
  dayNumber?: number | null;
  sequence?: number;
}

export interface VendorProfile {
  vendorId?: string;
  userId?: string;
  businessName: string;
  vendorType: VendorType;
  registrationNumber?: string;
  taxId?: string;
  description?: string;
  logoUrl?: string;
  email: string;
  phone: string;
  websiteUrl?: string;
  addressLine1: string;
  addressLine2?: string;
  city: string;
  stateProvince?: string;
  countryCode: string;
  postalCode?: string;
  status?: VendorStatus;
  rejectionReason?: string;
  commissionRate?: number;
  walletBalance?: number;
  pendingBalance?: number;
  payoutMethod?: PayoutMethod;
  averageRating?: number;
  totalReviews?: number;
  isEmailVerified?: boolean;
  createdAt?: string;
  approvedAt?: string;
  suspensionReason?: string;
  suspendedAt?: string;
}

export interface ReinstatementRequest {
  requestId?: string;
  vendorId?: string;
  vendorBusinessName?: string;
  message: string;
  status?: ReinstatementStatus;
  rejectionReason?: string;
  submittedAt?: string;
  reviewedAt?: string;
  reviewedByName?: string;
}

export interface PublicServiceListing {
  serviceId: string;
  serviceName: string;
  serviceType: ServiceType;
  description: string;
  basePrice: number;
  currencyCode: string;
  pricingUnit: PricingUnit;
  locationAddress?: string;
  imageUrl?: string;
  averageRating?: number;
  totalBookings?: number;
  maxCapacity?: number;
  packageItems?: PackageItem[];
  vendorId: string;
  vendorBusinessName: string;
}

export interface BookServiceRequest {
  startDate: string;
  endDate?: string;
  quantity?: number;
  specialRequests?: string;
  deliveryAddress?: string;
  contactPhone?: string;
  // Payment is no longer collected here — checkout happens via a real SSLCommerz
  // redirect after the booking is reserved (see PayNowModal/PaymentService).
}

export interface ServiceAvailability {
  maxCapacity: number;
  alreadyBooked: number;
  available: number;
}

export interface VendorServiceListing {
  serviceId?: string;
  serviceName: string;
  serviceType: ServiceType;
  description: string;
  basePrice: number;
  currencyCode?: string;
  pricingUnit: PricingUnit;
  maxCapacity: number;
  minBookingNotice?: number;
  maxBookingAdvance?: number;
  bookingMode?: BookingMode;
  confirmationWindow?: number;
  status?: ServiceStatus;
  cancellationPolicy?: string;
  locationLat?: number;
  locationLng?: number;
  locationAddress?: string;
  destinationId?: number;
  destinationName?: string;
  imageUrl?: string;
  tags?: string;
  metadata?: string;
  averageRating?: number;
  totalBookings?: number;
  isFeatured?: boolean;
  createdAt?: string;
  updatedAt?: string;
  packageItems?: PackageItem[];
}

export interface VendorBooking {
  bookingId: string;
  serviceId: string;
  serviceName: string;
  vendorId: string;
  vendorBusinessName?: string;
  userId: string;
  userName: string;
  userEmail: string;
  bookingStatus: VendorBookingStatus;
  startDate: string;
  endDate?: string;
  quantity: number;
  grossAmount: number;
  commissionAmount: number;
  netAmount: number;
  paymentStatus: string;
  paymentMethod?: PaymentMethod;
  paymentReference?: string;
  specialRequests?: string;
  deliveryAddress?: string;
  contactPhone?: string;
  cancellationReason?: string;
  cancelledBy?: string;
  createdAt: string;
  confirmedAt?: string;
  completedAt?: string;
}

export interface WalletSummary {
  availableBalance: number;
  pendingBalance: number;
  lifetimeEarnings: number;
  payoutMethod?: PayoutMethod;
  recentTransactions: WalletTransaction[];
}

export interface WalletTransaction {
  transactionId: string;
  transactionType: TransactionType;
  amount: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
  bookingId?: string;
}

export interface PayoutRequest {
  payoutId?: string;
  amount: number;
  payoutMethod: PayoutMethod;
  payoutDetails?: string;
  status?: PayoutStatus;
  adminNote?: string;
  requestedAt?: string;
  processedAt?: string;
}

export interface AnalyticsSummary {
  totalRevenue: number;
  revenueThisMonth: number;
  revenueThisWeek: number;
  revenueToday: number;
  totalBookings: number;
  confirmedBookings: number;
  cancelledBookings: number;
  cancellationRate: number;
  activeServices: number;
  averageRating: number;
  totalReviews: number;
  revenueTimeSeries: { [date: string]: number };
  topServices: ServicePerformance[];
}

export interface ServicePerformance {
  serviceId: string;
  serviceName: string;
  revenue: number;
  bookingCount: number;
  averageRating: number;
}

