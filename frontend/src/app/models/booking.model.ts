import { BookingStatus } from '../enums/booking-status.enum';
import { User } from './user.model';
import { Room } from './room.model';
import { Hotel } from './hotel.model';

export interface Booking {
  id?: number;
  user: User | { id: number };
  room: Room | { id: number };
  hotel: Hotel | { id: number };
  checkInDate: Date | string;
  checkOutDate: Date | string;
  guestCount: number;
  totalPrice: number;
  status: BookingStatus;
  specialRequest?: string;
  createdAt?: Date;
  updatedAt?: Date;
  version?: number;
}

export interface BookingRequest {
  userId: number;
  roomId: number;
  hotelId: number;
  checkInDate: string;
  checkOutDate: string;
  guestCount: number;
  totalPrice: number;
  status?: BookingStatus;
  specialRequest?: string;
}
