export interface DestinationExpenseSummary {
  destinationId: number | null;
  destinationName: string;
  totalSpent: number;
  bookingCount: number;
  firstTravelDate: string;
  lastTravelDate: string;
}
