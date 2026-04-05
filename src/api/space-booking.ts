import { apiRequest } from "@/api/http";
import type {
  AdminSpaceBlockout,
  AdminSpaceBooking,
  AdminSpaceBookingSummary,
  AdminSpaceResource,
  PublicSpaceAvailability,
} from "@/types/space-booking";

export function fetchAdminSpaces() {
  return apiRequest<AdminSpaceResource[]>("/api/v1/admin/space-bookings/spaces");
}

export function fetchAdminBookings(query: { from?: string; to?: string; status?: string }) {
  return apiRequest<AdminSpaceBookingSummary[]>("/api/v1/admin/space-bookings", { query });
}

export function fetchAdminBooking(bookingId: string) {
  return apiRequest<AdminSpaceBooking>(`/api/v1/admin/space-bookings/${bookingId}`);
}

export function updateAdminBooking(bookingId: string, payload: Record<string, unknown>) {
  return apiRequest<AdminSpaceBooking>(`/api/v1/admin/space-bookings/${bookingId}`, {
    method: "PATCH",
    body: payload,
  });
}

export function createAdminBooking(payload: Record<string, unknown>) {
  return apiRequest<AdminSpaceBooking>("/api/v1/admin/space-bookings", {
    method: "POST",
    body: payload,
  });
}

export function approveAdminBooking(bookingId: string, internalNote: string) {
  return apiRequest<AdminSpaceBooking>(`/api/v1/admin/space-bookings/${bookingId}/approve`, {
    method: "POST",
    body: { internalNote },
  });
}

export function cancelAdminBooking(bookingId: string, internalNote: string) {
  return apiRequest<AdminSpaceBooking>(`/api/v1/admin/space-bookings/${bookingId}/cancel`, {
    method: "POST",
    body: { internalNote },
  });
}

export function fetchAdminBlockouts(query: { from?: string; to?: string }) {
  return apiRequest<AdminSpaceBlockout[]>("/api/v1/admin/space-bookings/blockouts", { query });
}

export function createAdminBlockout(payload: Record<string, unknown>) {
  return apiRequest<AdminSpaceBlockout>("/api/v1/admin/space-bookings/blockouts", {
    method: "POST",
    body: payload,
  });
}

export function deleteAdminBlockout(blockoutId: string) {
  return apiRequest<boolean>(`/api/v1/admin/space-bookings/blockouts/${blockoutId}`, {
    method: "DELETE",
  });
}

export function fetchPublicAvailability(slug: string, query: { from?: string; to?: string }) {
  return apiRequest<PublicSpaceAvailability>(`/api/v1/public/spaces/${slug}/availability`, {
    query,
    auth: false,
  });
}
