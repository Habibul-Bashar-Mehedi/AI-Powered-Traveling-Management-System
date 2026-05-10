import { HotelStatus } from '../enums/hotel-status.enum';
import { User } from './user.model';
import { Destination } from './destination.model';

export interface Hotel {
  id?: number;
  vendor?: User | { id: number };
  destination?: Destination | { id: number };
  hotelName: string;
  address: string;
  status: HotelStatus;
  descriptions?: string;
  createdAt?: Date;
  updatedAt?: Date;
  isDeleted?: boolean;
  version?: number;
}
