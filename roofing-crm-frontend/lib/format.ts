import type { AddressDto } from "./types";

function isDateOnlyString(iso: string): boolean {
  return /^\d{4}-\d{2}-\d{2}$/.test(iso);
}

/** Parse YYYY-MM-DD as local date (avoids UTC midnight shift). */
export function parseLocalDateOnly(iso: string): Date {
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(y, m - 1, d);
}

/** Alias for parseLocalDateOnly. */
export const parseDateOnly = parseLocalDateOnly;

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
 * Format YYYY-MM-DD as "Tue Jan 27" (weekday + short date).
 */
export function formatDateShortWeekday(iso: string): string {
  if (!iso || !isDateOnlyString(iso)) return "";
  const dt = parseLocalDateOnly(iso);
  return dt.toLocaleDateString("en-US", {
    weekday: "short",
    month: "short",
    day: "numeric",
  });
}

/**
 * Format an ISO date string for display (short: "Jan 1, 2024").
 * For date-only strings (YYYY-MM-DD), parses as local date to avoid timezone shifts.
 * Returns "—" if missing or invalid.
 */
export function formatDate(iso?: string | null): string {
  if (iso == null || iso === "") return "—";
  try {
    const dt = isDateOnlyString(iso) ? parseLocalDateOnly(iso) : new Date(iso);
    if (Number.isNaN(dt.getTime())) return "—";
    return dt.toLocaleDateString("en-US", {
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  } catch {
    return "—";
  }
}

/** Format a Date as YYYY-MM-DD using local getters (for date inputs). */
export function formatLocalDateInput(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

/** Alias for formatLocalDateInput. */
export const formatDateOnly = formatLocalDateInput;

/**
 * Format an ISO date string for display with time (e.g. "January 1, 2024 at 12:00 PM").
 * Uses toLocaleString for reliable time across browsers.
 * Returns "—" if missing or invalid.
 */
export function formatDateTime(iso?: string | null): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "—";
  return d.toLocaleString("en-US", {
    month: "long",
    day: "numeric",
    year: "numeric",
    hour: "numeric",
    minute: "2-digit",
  });
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

/**
 * Format byte count for display (B, KB, MB, GB).
 * Returns "—" if null/undefined/NaN.
 */
export function formatFileSize(bytes?: number | null): string {
  if (bytes == null || Number.isNaN(bytes)) return "—";
  if (bytes < 1024) return `${bytes} B`;
  const kb = bytes / 1024;
  if (kb < 1024) return `${kb.toFixed(1)} KB`;
  const mb = kb / 1024;
  if (mb < 1024) return `${mb.toFixed(1)} MB`;
  const gb = mb / 1024;
  return `${gb.toFixed(1)} GB`;
}
