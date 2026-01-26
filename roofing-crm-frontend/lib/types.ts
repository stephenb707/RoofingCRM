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

export interface UpdateLeadRequest {
  source?: LeadSource | null;
  leadNotes?: string | null;
  preferredContactMethod?: string | null;
  propertyAddress?: AddressDto | null;
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

// Job-related types
export type JobStatus =
  | "SCHEDULED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "INVOICED";

export type JobType = "REPLACEMENT" | "REPAIR" | "INSPECTION_ONLY";

export interface JobDto {
  id: string;
  customerId: string | null;
  leadId: string | null;
  status: JobStatus;
  type: JobType;
  propertyAddress: AddressDto | null;
  scheduledStartDate: string | null;
  scheduledEndDate: string | null;
  internalNotes: string | null;
  crewName: string | null;
  createdAt: string;
  updatedAt: string;
  customerFirstName?: string;
  customerLastName?: string;
  customerEmail?: string;
  customerPhone?: string;
}

export interface UpdateJobRequest {
  type?: JobType | null;
  propertyAddress?: AddressDto | null;
  scheduledStartDate?: string | null;
  scheduledEndDate?: string | null;
  internalNotes?: string | null;
  crewName?: string | null;
}

export interface CreateJobRequest {
  leadId?: string | null;
  customerId?: string | null;
  type: JobType;
  propertyAddress: AddressDto;
  scheduledStartDate?: string | null;
  scheduledEndDate?: string | null;
  internalNotes?: string | null;
  crewName?: string | null;
}

export interface UpdateJobStatusRequest {
  status: JobStatus;
}

// Estimate-related types
export type EstimateStatus = "DRAFT" | "SENT" | "ACCEPTED" | "REJECTED";

export interface EstimateItemDto {
  id: string;
  name: string;
  description?: string | null;
  quantity: number;
  unitPrice: number;
  unit?: string | null;
}

export interface EstimateDto {
  id: string;
  jobId: string;
  customerId?: string | null;
  status: EstimateStatus;
  title?: string | null;
  notes?: string | null;
  issueDate?: string | null; // YYYY-MM-DD
  validUntil?: string | null; // YYYY-MM-DD
  items: EstimateItemDto[];
  subtotal?: number | null;
  total?: number | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface EstimateItemRequest {
  name: string;
  description?: string | null;
  quantity: number;
  unitPrice: number;
  unit?: string | null;
}

export interface CreateEstimateRequest {
  title?: string | null;
  notes?: string | null;
  issueDate?: string | null;
  validUntil?: string | null;
  items: EstimateItemRequest[];
  status?: EstimateStatus | null;
}

export interface UpdateEstimateRequest {
  title?: string | null;
  notes?: string | null;
  issueDate?: string | null;
  validUntil?: string | null;
  items?: EstimateItemRequest[] | null;
  status?: EstimateStatus | null;
}

export interface UpdateEstimateStatusRequest {
  status: EstimateStatus;
}
