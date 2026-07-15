import { Destination } from './destination.model';

export interface Food {
  id?: number;
  destination?: Destination;
  dishName: string;
  description?: string;
  culturalContext?: string;
  priceRange?: string;
  recommendedLocation?: string;
  linkedServiceId?: string;
}
