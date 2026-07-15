export interface Destination {
  id?: number;
  name: string;
  region: string;
  description?: string;
  isAlive?: boolean;
  latitude?: number;
  longitude?: number;
  createdAt?: Date;
  version?: number;
}

export interface NearbyQuery {
  lat?: number;
  lng?: number;
  radiusKm?: number;
  destinationId?: number;
}
