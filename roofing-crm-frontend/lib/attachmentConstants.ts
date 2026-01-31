import type { AttachmentTag } from "./types";

export const ATTACHMENT_TAGS: AttachmentTag[] = [
  "BEFORE",
  "DAMAGE",
  "AFTER",
  "INVOICE",
  "DOCUMENT",
  "OTHER",
];

export const TAG_LABELS: Record<AttachmentTag, string> = {
  BEFORE: "Before",
  DAMAGE: "Damage",
  AFTER: "After",
  INVOICE: "Invoice",
  DOCUMENT: "Document",
  OTHER: "Other",
};
