export interface TenantSummary {
  tenantId: string;
  tenantName: string;
  tenantSlug: string;
  role: string; // UserRole string from backend
}

export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
  fullName: string | null;
  tenants: TenantSummary[];
}

export interface CustomerDto {
  id: string;
  firstName: string;
  lastName: string;
  primaryPhone?: string | null;
  email?: string | null;
  notes?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

// Lead-related types
export type LeadStatus =
  | "NEW"
  | "CONTACTED"
  | "INSPECTION_SCHEDULED"
  | "QUOTE_SENT"
  | "WON"
  | "LOST";

export type LeadSource =
  | "REFERRAL"
  | "WEBSITE"
  | "DOOR_TO_DOOR"
  | "INSURANCE_PARTNER"
  | "OTHER";

export interface AddressDto {
  line1?: string | null;
  line2?: string | null;
  city?: string | null;
  state?: string | null;
  zip?: string | null;
  countryCode?: string | null;
}

export interface LeadDto {
  id: string;
  customerId: string | null;
  status: LeadStatus;
  source: LeadSource | null;
  leadNotes: string | null;
  propertyAddress: AddressDto | null;
  preferredContactMethod: string | null;
  createdAt: string;
  updatedAt: string;
  // Enriched fields from backend (customer data)
  customerFirstName?: string;
  customerLastName?: string;
  customerPhone?: string;
  customerEmail?: string;
}

export interface NewLeadCustomerRequest {
  firstName: string;
  lastName: string;
  primaryPhone: string;
  email?: string | null;
  billingAddress?: AddressDto | null;
}

export interface CreateLeadRequest {
  customerId?: string | null;
  newCustomer?: NewLeadCustomerRequest | null;
  source?: LeadSource | null;
  leadNotes?: string | null;
  propertyAddress: AddressDto;
  preferredContactMethod?: string | null;
}

export interface UpdateLeadStatusRequest {
  status: LeadStatus;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}
