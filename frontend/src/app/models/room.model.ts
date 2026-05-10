import { RoomStatus } from '../enums/room-status.enum';
import { Hotel } from './hotel.model';

export interface Room {
  id?: number;
  hotel: Hotel | { id: number };
  roomTypeName: string;
  amenities?: string;
  pricePerNight: number;
  availableQuantities: number;
  status: RoomStatus;
  createdAt?: Date;
  updatedAt?: Date;
  version?: number;
}
