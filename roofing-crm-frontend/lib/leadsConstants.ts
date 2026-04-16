import type { LeadSource } from "./types";

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
