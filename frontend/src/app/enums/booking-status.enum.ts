export enum BookingStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  CANCELLED = 'CANCELLED',
  COMPLETED = 'COMPLETED',
  CHECKED_IN = 'CHECKED_IN',
  CHECKED_OUT = 'CHECKED_OUT'
}

export const BookingStatusLabels: Record<BookingStatus, string> = {
  [BookingStatus.PENDING]: 'Pending',
  [BookingStatus.CONFIRMED]: 'Confirmed',
  [BookingStatus.CANCELLED]: 'Cancelled',
  [BookingStatus.COMPLETED]: 'Completed',
  [BookingStatus.CHECKED_IN]: 'Checked In',
  [BookingStatus.CHECKED_OUT]: 'Checked Out'
};

export const BookingStatusColors: Record<BookingStatus, string> = {
  [BookingStatus.PENDING]: 'bg-yellow-100 text-yellow-800',
  [BookingStatus.CONFIRMED]: 'bg-green-100 text-green-800',
  [BookingStatus.CANCELLED]: 'bg-red-100 text-red-800',
  [BookingStatus.COMPLETED]: 'bg-blue-100 text-blue-800',
  [BookingStatus.CHECKED_IN]: 'bg-indigo-100 text-indigo-800',
  [BookingStatus.CHECKED_OUT]: 'bg-gray-100 text-gray-800'
};
