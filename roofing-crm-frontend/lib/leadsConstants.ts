import type { LeadStatus, LeadSource } from "./types";

export const LEAD_STATUSES: LeadStatus[] = [
  "NEW",
  "CONTACTED",
  "INSPECTION_SCHEDULED",
  "QUOTE_SENT",
  "WON",
  "LOST",
];

export const STATUS_LABELS: Record<LeadStatus, string> = {
  NEW: "New",
  CONTACTED: "Contacted",
  INSPECTION_SCHEDULED: "Inspection Scheduled",
  QUOTE_SENT: "Quote Sent",
  WON: "Won",
  LOST: "Lost",
};

export const STATUS_COLORS: Record<LeadStatus, string> = {
  NEW: "bg-blue-100 text-blue-700 border-blue-200",
  CONTACTED: "bg-amber-100 text-amber-700 border-amber-200",
  INSPECTION_SCHEDULED: "bg-purple-100 text-purple-700 border-purple-200",
  QUOTE_SENT: "bg-sky-100 text-sky-700 border-sky-200",
  WON: "bg-green-100 text-green-700 border-green-200",
  LOST: "bg-red-100 text-red-700 border-red-200",
};

export const LEAD_SOURCES: LeadSource[] = [
  "REFERRAL",
  "WEBSITE",
  "DOOR_TO_DOOR",
  "INSURANCE_PARTNER",
  "OTHER",
];

export const SOURCE_LABELS: Record<LeadSource, string> = {
  REFERRAL: "Referral",
  WEBSITE: "Website",
  DOOR_TO_DOOR: "Door to Door",
  INSURANCE_PARTNER: "Insurance Partner",
  OTHER: "Other",
};
