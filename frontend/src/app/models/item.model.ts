import { Destination } from './destination.model';
import { Market } from './market.model';

export interface TraditionalItem {
  id?: number;
  market?: Market;
  destination?: Destination;
  categoryName: string;
  description?: string;
  priceRange?: string;
  linkedServiceId?: string;
}
