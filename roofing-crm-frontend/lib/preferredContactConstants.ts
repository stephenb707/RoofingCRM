import type { PreferredContactMethod } from "./types";

export const PREFERRED_CONTACT_METHODS: PreferredContactMethod[] = [
  "PHONE",
  "TEXT",
  "EMAIL",
];

export const PREFERRED_CONTACT_LABELS: Record<PreferredContactMethod, string> = {
  PHONE: "Phone call",
  TEXT: "Text message",
  EMAIL: "Email",
};
