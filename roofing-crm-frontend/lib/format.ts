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
 * Format an ISO date string for display.
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
