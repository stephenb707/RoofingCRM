/**
 * Centralized field configuration for customizable list pages.
 *
 * Each list defines its available fields, default visibility, and display labels.
 * The `key` values must match the keys stored in tenant app preferences
 * (e.g. prefs.jobsList.visibleFields = ["type", "status", ...]).
 */

export interface ListFieldDef {
  key: string;
  label: string;
  defaultVisible: boolean;
}

export type ListConfigKey =
  | "jobsList"
  | "leadsList"
  | "customersList"
  | "tasksList";

export const LIST_FIELD_CONFIGS: Record<ListConfigKey, ListFieldDef[]> = {
  jobsList: [
    { key: "type", label: "Type", defaultVisible: true },
    { key: "status", label: "Status", defaultVisible: true },
    { key: "customer", label: "Customer", defaultVisible: false },
    { key: "propertyAddress", label: "Property Address", defaultVisible: true },
    { key: "scheduledStartDate", label: "Scheduled", defaultVisible: true },
    { key: "crew", label: "Crew", defaultVisible: false },
    { key: "updatedAt", label: "Updated", defaultVisible: true },
  ],
  leadsList: [
    { key: "customer", label: "Customer", defaultVisible: true },
    { key: "propertyAddress", label: "Property Address", defaultVisible: true },
    { key: "status", label: "Status", defaultVisible: true },
    { key: "source", label: "Source", defaultVisible: true },
    { key: "createdAt", label: "Created", defaultVisible: true },
  ],
  customersList: [
    { key: "name", label: "Name", defaultVisible: true },
    { key: "phone", label: "Phone", defaultVisible: true },
    { key: "email", label: "Email", defaultVisible: true },
    { key: "preferredContact", label: "Preferred Contact", defaultVisible: false },
    { key: "createdAt", label: "Created", defaultVisible: false },
  ],
  tasksList: [
    { key: "title", label: "Title", defaultVisible: true },
    { key: "status", label: "Status", defaultVisible: true },
    { key: "priority", label: "Priority", defaultVisible: true },
    { key: "dueAt", label: "Due", defaultVisible: true },
    { key: "assignedTo", label: "Assignee", defaultVisible: true },
    { key: "related", label: "Related", defaultVisible: true },
  ],
};

export function getDefaultVisibleFields(listKey: ListConfigKey): string[] {
  return LIST_FIELD_CONFIGS[listKey]
    .filter((f) => f.defaultVisible)
    .map((f) => f.key);
}

/**
 * Resolve the ordered list of visible field keys from saved preferences.
 * Falls back to defaults if preferences are missing or empty.
 * Filters out any keys that are no longer in the available field config
 * (protects against stale prefs after config changes).
 */
export function resolveVisibleFields(
  listKey: ListConfigKey,
  savedFields: unknown
): string[] {
  const available = new Set(LIST_FIELD_CONFIGS[listKey].map((f) => f.key));
  if (Array.isArray(savedFields) && savedFields.length > 0) {
    const valid = savedFields.filter(
      (k): k is string => typeof k === "string" && available.has(k)
    );
    if (valid.length > 0) return valid;
  }
  return getDefaultVisibleFields(listKey);
}

export const LIST_DISPLAY_NAMES: Record<ListConfigKey, string> = {
  jobsList: "Jobs",
  leadsList: "Leads",
  customersList: "Customers",
  tasksList: "Tasks",
};
