import type { AddressDto } from "./types";

/**
 * Format an address for display.
 * e.g. "123 Main St, City, ST 12345"
 */
export function formatAddress(address?: AddressDto | null): string {
  if (!address) return "—";
  const parts = [
    address.line1,
    address.line2,
    [address.city, address.state].filter(Boolean).join(", "),
    address.zip,
  ].filter(Boolean);
  return parts.length > 0 ? parts.join(", ") : "—";
}

/**
 * Format an ISO date string for display (short: "Jan 1, 2024").
 * Returns "—" if missing or invalid.
 */
export function formatDate(iso?: string | null): string {
  if (iso == null || iso === "") return "—";
  try {
    return new Date(iso).toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  } catch {
    return "—";
  }
}

/**
 * Format an ISO date string for display with time (e.g. "January 1, 2024 at 12:00 PM").
 * Returns "—" if missing or invalid.
 */
export function formatDateTime(iso?: string | null): string {
  if (iso == null || iso === "") return "—";
  try {
    return new Date(iso).toLocaleDateString("en-US", {
      month: "long",
      day: "numeric",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
    });
  } catch {
    return "—";
  }
}

/**
 * Format a decimal amount as money.
 * Assumes amount is in whole currency units (e.g. dollars), not cents.
 * Returns "—" if amount is null/undefined/NaN.
 */
export function formatMoney(
  amount?: number | null,
  currency: string = "USD"
): string {
  if (amount == null || Number.isNaN(amount)) return "—";
  try {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency,
    }).format(amount);
  } catch {
    return "—";
  }
}

/**
 * Format a phone number for display.
 * - 10 digits: (XXX) XXX-XXXX
 * - 11 digits starting with 1: +1 (XXX) XXX-XXXX
 * - Otherwise: returns original string
 * Returns "—" if input is null/undefined/empty.
 */
export function formatPhone(raw?: string | null): string {
  if (!raw) return "—";
  const digits = raw.replace(/\D/g, "");
  if (digits.length === 10) {
    return `(${digits.slice(0, 3)}) ${digits.slice(3, 6)}-${digits.slice(6)}`;
  }
  if (digits.length === 11 && digits.startsWith("1")) {
    const d = digits.slice(1);
    return `+1 (${d.slice(0, 3)}) ${d.slice(3, 6)}-${d.slice(6)}`;
  }
  return raw;
}
