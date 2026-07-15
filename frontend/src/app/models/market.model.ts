import { Destination } from './destination.model';

export interface Market {
  id?: number;
  destination?: Destination;
  name: string;
  location?: string;
  operatingDays?: string;
  operatingHours?: string;
  description?: string;
  alive?: boolean;
}
