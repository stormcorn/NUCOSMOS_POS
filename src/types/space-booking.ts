export type PublicSpaceResource = {
  code: string;
  name: string;
  slug: string;
  description: string | null;
  locationLabel: string | null;
  capacity: number;
  timezone: string;
  hourlyRate: number;
  currencyCode: string;
  minimumHours: number;
  bookingIntervalMinutes: number;
  bufferBeforeMinutes: number;
  bufferAfterMinutes: number;
  maxAttendees: number;
  cancellationPolicyText: string | null;
  houseRulesText: string | null;
};

export type PublicSpaceAvailabilitySlot = {
  startAt: string;
  endAt: string;
  status: "AVAILABLE" | "UNAVAILABLE" | string;
  label: string;
};

export type PublicSpaceAvailabilityDay = {
  date: string;
  slots: PublicSpaceAvailabilitySlot[];
};

export type PublicSpaceAvailability = {
  spaceSlug: string;
  spaceName: string;
  timezone: string;
  bookingIntervalMinutes: number;
  days: PublicSpaceAvailabilityDay[];
};

export type AdminSpaceResource = {
  id: string;
  code: string;
  name: string;
  slug: string;
  locationLabel: string | null;
  capacity: number;
  active: boolean;
  timezone: string;
  hourlyRate: number;
  currencyCode: string;
  minimumHours: number;
  bookingIntervalMinutes: number;
  bufferBeforeMinutes: number;
  bufferAfterMinutes: number;
  maxAttendees: number;
};

export type AdminSpaceBookingSummary = {
  id: string;
  bookingNumber: string;
  status: string;
  source: string;
  spaceName: string;
  customerName: string;
  customerPhone: string;
  purpose: string | null;
  eventLink: string | null;
  attendeeCount: number;
  startAt: string;
  endAt: string;
  subtotalAmount: number;
  approvedAt: string | null;
};

export type AdminSpaceBooking = {
  id: string;
  bookingNumber: string;
  status: string;
  source: string;
  spaceId: string;
  spaceName: string;
  customerName: string;
  customerPhone: string;
  customerEmail: string | null;
  purpose: string | null;
  eventLink: string | null;
  attendeeCount: number;
  subtotalAmount: number;
  depositAmount: number;
  paidAmount: number;
  balanceAmount: number;
  note: string | null;
  internalNote: string | null;
  startAt: string;
  endAt: string;
  approvedAt: string | null;
  approvedBy: string | null;
  cancelledAt: string | null;
};

export type AdminSpaceBlockout = {
  id: string;
  spaceId: string;
  spaceName: string;
  title: string;
  reason: string | null;
  startAt: string;
  endAt: string;
  createdBy: string | null;
};
