export type PaymentBookingType = 'VENDOR_BOOKING' | 'PACKAGE_BOOKING';

export type PaymentStatusValue = 'PENDING' | 'PAID' | 'FAILED' | 'CANCELLED';

export type PreferredPaymentMethod = 'MOBILE_BANK' | 'CARD' | 'INTERNET_BANK';

export interface PaymentInitiateRequest {
  bookingType: PaymentBookingType;
  bookingId: string;
  // Optional — a filter hint so SSLCommerz's hosted page pre-narrows to this category.
  preferredMethod?: PreferredPaymentMethod;
  // Optional — a more specific provider within preferredMethod (e.g. "BKASH", "VISA").
  preferredProvider?: string;
  // Optional — Bangladeshi mobile number collected in the Mobile Banking sub-step
  // (contact info passed through to the gateway, never a PIN/OTP/credential).
  contactPhone?: string;
}

export interface PaymentInitiateResponse {
  gatewayPageUrl: string;
  txId: string;
  amount: number;
  currencyCode: string;
}

export interface PaymentStatusDTO {
  paymentId: string;
  txId: string;
  status: PaymentStatusValue;
  amount: number;
  currencyCode: string;
  gatewayCardType?: string;
  bookingType: PaymentBookingType;
  bookingId: string;
  bookingReference: string;
  initiatedAt: string;
  updatedAt: string;
}

export type SimulatedPaymentOutcome = 'PAID' | 'FAILED' | 'CANCELLED';

export interface PaymentSimulateRequest {
  txId: string;
  outcome: SimulatedPaymentOutcome;
}

export interface PaymentSimulateResponse {
  status: string;
}
