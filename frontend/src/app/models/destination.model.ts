export interface Destination {
  id?: number;
  name: string;
  region: string;
  description?: string;
  isAlive?: boolean;
  createdAt?: Date;
  version?: number;
}
