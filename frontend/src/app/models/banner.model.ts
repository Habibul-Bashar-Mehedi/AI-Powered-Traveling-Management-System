export interface Banner {
  id?: string;
  title: string;
  description?: string;
  imageUrl?: string;
  badgeText?: string;
  ctaLabel?: string;
  ctaTarget?: string;
  active?: boolean;
  startDate?: string;
  endDate?: string;
  displayOrder?: number;
  createdAt?: string;
  updatedAt?: string;
}
