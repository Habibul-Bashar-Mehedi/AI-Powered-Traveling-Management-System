import { Destination } from './destination.model';

export interface TouristSpot {
  id?: number;
  destination?: Destination;
  name: string;
  description?: string;
  visitingHours?: string;
  adultEntryFees: number;
  childEntryFees: number;
  locationDescription?: string;
  alive?: boolean;
  requiresTicket?: boolean;
  linkedServiceId?: string;
}
