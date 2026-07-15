import { BookingSource } from '../enums/booking-source.enum';
import { ServiceType } from '../enums/vendor.enums';

export interface BookingHistoryEntry {
  id: string;
  source: BookingSource;
  serviceType: ServiceType;
  title: string;
  bookingDate: string;
  travelDate: string;
  status: string;
  amount: number;
}
