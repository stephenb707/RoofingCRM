/**
 * Drop values that break backend UUID binding (null/undefined/empty from API quirks).
 */
export function sanitizeAttachmentIds(ids: string[]): string[] {
  return ids
    .filter((id): id is string => typeof id === "string")
    .map((id) => id.trim())
    .filter((id) => id.length > 0);
}
