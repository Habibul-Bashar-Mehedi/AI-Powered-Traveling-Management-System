export enum RoomStatus {
  AVAILABLE = 'AVAILABLE',
  BOOKED = 'BOOKED',
  MAINTENANCE = 'MAINTENANCE',
  UNAVAILABLE = 'UNAVAILABLE'
}

export const RoomStatusLabels: Record<RoomStatus, string> = {
  [RoomStatus.AVAILABLE]: 'Available',
  [RoomStatus.BOOKED]: 'Booked',
  [RoomStatus.MAINTENANCE]: 'Under Maintenance',
  [RoomStatus.UNAVAILABLE]: 'Unavailable'
};
