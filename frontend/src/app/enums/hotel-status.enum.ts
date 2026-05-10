export enum HotelStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  MAINTENANCE = 'MAINTENANCE'
}

export const HotelStatusLabels: Record<HotelStatus, string> = {
  [HotelStatus.ACTIVE]: 'Active',
  [HotelStatus.INACTIVE]: 'Inactive',
  [HotelStatus.MAINTENANCE]: 'Under Maintenance'
};
