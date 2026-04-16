export type UserRole = "OWNER" | "ADMIN" | "SALES" | "FIELD_TECH";

export interface TenantSummary {
  tenantId: string;
  tenantName: string;
  tenantSlug: string;
  role: string; // UserRole string from backend
}

export interface TeamMember {
  userId: string;
  email: string;
  fullName?: string | null;
  role: UserRole;
}

export interface TenantInvite {
  inviteId: string;
  email: string;
  role: UserRole;
  expiresAt: string;
  acceptedAt?: string | null;
  createdAt: string;
  createdByName?: string | null;
}

export interface AcceptInviteResponse {
  tenantId: string;
  tenantName: string;
  tenantSlug?: string | null;
  role: UserRole;
}

export interface AuthResponse {
  token: string;
  userId: string;
  email: string;
  fullName: string | null;
  tenants: TenantSummary[];
}

export type PreferredContactMethod = "PHONE" | "TEXT" | "EMAIL";

export interface CustomerDto {
  id: string;
  firstName: string;
  lastName: string;
  primaryPhone?: string | null;
  email?: string | null;
  preferredContactMethod?: PreferredContactMethod | null;
  billingAddress?: AddressDto | null;
  notes?: string | null;
  createdAt?: string;
  updatedAt?: string;
}

export interface CreateCustomerRequest {
  firstName: string;
  lastName: string;
  primaryPhone: string;
  email?: string | null;
  preferredContactMethod?: PreferredContactMethod | null;
  billingAddress?: AddressDto | null;
  notes?: string | null;
}

export interface UpdateCustomerRequest {
  firstName: string;
  lastName: string;
  primaryPhone: string;
  email?: string | null;
  preferredContactMethod?: PreferredContactMethod | null;
  billingAddress?: AddressDto | null;
  notes?: string | null;
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
  /** Target pipeline column / persisted FK. */
  statusDefinitionId: string;
  /** Stable key: built-in enum name or custom id from backend. */
  statusKey: string;
  /** Tenant-configured display label. */
  statusLabel: string;
  source: LeadSource | null;
  leadNotes: string | null;
  propertyAddress: AddressDto | null;
  pipelinePosition: number;
  createdAt: string;
  updatedAt: string;
  // Enriched fields from backend (customer data)
  customerFirstName?: string;
  customerLastName?: string;
  customerPhone?: string;
  customerEmail?: string;
  /** Set when this lead has been converted to a job. */
  convertedJobId?: string | null;
}

export interface NewLeadCustomerRequest {
  firstName: string;
  lastName: string;
  primaryPhone: string;
  email?: string | null;
  preferredContactMethod?: PreferredContactMethod | null;
  billingAddress?: AddressDto | null;
}

export interface CreateLeadRequest {
  customerId?: string | null;
  newCustomer?: NewLeadCustomerRequest | null;
  source?: LeadSource | null;
  leadNotes?: string | null;
  propertyAddress: AddressDto;
}

export interface UpdateLeadStatusRequest {
  statusDefinitionId: string;
  position?: number;
}

export interface ConvertLeadToJobRequest {
  type: JobType;
  scheduledStartDate?: string | null;
  scheduledEndDate?: string | null;
  crewName?: string | null;
  internalNotes?: string | null;
}

export interface UpdateLeadRequest {
  source?: LeadSource | null;
  leadNotes?: string | null;
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
  | "UNSCHEDULED"
  | "SCHEDULED"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "INVOICED";

export type JobType = "REPLACEMENT" | "REPAIR" | "INSPECTION_ONLY";

export interface JobDto {
  id: string;
  customerId: string | null;
  leadId: string | null;
  statusDefinitionId: string;
  statusKey: string;
  statusLabel: string;
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
  clearSchedule?: boolean | null;
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
  statusDefinitionId: string;
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
  customerName?: string | null;
  customerEmail?: string | null;
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

export interface EstimateSummaryDto {
  id: string;
  jobId: string;
  customerId?: string | null;
  status: EstimateStatus;
  title?: string | null;
  notes?: string | null;
  issueDate?: string | null; // YYYY-MM-DD
  validUntil?: string | null; // YYYY-MM-DD
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

export interface ShareEstimateResponse {
  token: string;
  expiresAt: string;
}

export interface SendEstimateEmailRequest {
  recipientEmail: string;
  recipientName?: string;
  subject?: string;
  message?: string;
  expiresInDays?: number;
}

export interface SendEstimateEmailResponse {
  success: boolean;
  sentAt: string;
  publicUrl: string;
  reusedExistingToken: boolean;
}

export interface ShareInvoiceResponse {
  token: string;
  expiresAt: string;
}

export interface SendInvoiceEmailRequest {
  recipientEmail: string;
  recipientName?: string;
  subject?: string;
  message?: string;
  expiresInDays?: number;
}

export interface SendInvoiceEmailResponse {
  success: boolean;
  sentAt: string;
  publicUrl: string;
  reusedExistingToken: boolean;
}

export type JobCostCategory =
  | "MATERIAL"
  | "TRANSPORTATION"
  | "LABOR"
  | "OTHER";

export interface JobCostEntryDto {
  id: string;
  jobId: string;
  category: JobCostCategory;
  vendorName?: string | null;
  description: string;
  amount: number;
  incurredAt: string;
  notes?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CreateJobCostEntryRequest {
  category: JobCostCategory;
  vendorName?: string | null;
  description: string;
  amount: number;
  incurredAt: string;
  notes?: string | null;
}

export interface UpdateJobCostEntryRequest {
  category?: JobCostCategory | null;
  vendorName?: string | null;
  description?: string | null;
  amount?: number | null;
  incurredAt?: string | null;
  notes?: string | null;
}

export interface JobAccountingSummaryDto {
  agreedAmount?: number | null;
  invoicedAmount: number;
  paidAmount: number;
  totalCosts: number;
  grossProfit: number;
  marginPercent?: number | null;
  projectedProfit?: number | null;
  actualProfit: number;
  projectedMarginPercent?: number | null;
  actualMarginPercent?: number | null;
  categoryTotals: Record<JobCostCategory, number>;
  hasAcceptedEstimate: boolean;
}

export interface PublicEstimateItemDto {
  name: string;
  description?: string | null;
  quantity: number;
  unitPrice: number;
  unit?: string | null;
  lineTotal: number;
}

export interface PublicEstimateDto {
  estimateNumber: string;
  status: EstimateStatus;
  title?: string | null;
  notes?: string | null;
  issueDate?: string | null;
  validUntil?: string | null;
  subtotal?: number | null;
  total?: number | null;
  publicExpiresAt?: string | null;
  customerName?: string | null;
  customerAddress?: string | null;
  items: PublicEstimateItemDto[];
}

export interface PublicEstimateDecisionRequest {
  decision: EstimateStatus;
  signerName: string;
  signerEmail?: string | null;
}

// Invoice types
export type InvoiceStatus = "DRAFT" | "SENT" | "PAID" | "VOID";

export interface InvoiceItemDto {
  id: string;
  name: string;
  description?: string | null;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  sortOrder?: number;
}

export interface InvoiceDto {
  id: string;
  invoiceNumber: string;
  status: InvoiceStatus;
  issuedAt?: string | null;
  sentAt?: string | null;
  dueAt?: string | null;
  paidAt?: string | null;
  total: number;
  notes?: string | null;
  jobId: string;
  estimateId?: string | null;
  customerName?: string | null;
  customerEmail?: string | null;
  items?: InvoiceItemDto[];
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface InvoiceSummaryDto {
  id: string;
  invoiceNumber: string;
  status: InvoiceStatus;
  issuedAt?: string | null;
  sentAt?: string | null;
  dueAt?: string | null;
  paidAt?: string | null;
  total: number;
  notes?: string | null;
  jobId: string;
  estimateId?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface PublicInvoiceItemDto {
  name: string;
  description?: string | null;
  quantity: number;
  unitPrice: number;
  unit?: string | null;
  lineTotal: number;
}

export interface PublicInvoiceDto {
  invoiceNumber: string;
  status: InvoiceStatus;
  issuedAt?: string | null;
  dueAt?: string | null;
  sentAt?: string | null;
  total: number;
  notes?: string | null;
  publicExpiresAt?: string | null;
  customerName?: string | null;
  customerAddress?: string | null;
  items: PublicInvoiceItemDto[];
}

// Attachment types
export type AttachmentTag =
  | "BEFORE"
  | "DAMAGE"
  | "AFTER"
  | "INVOICE"
  | "RECEIPT"
  | "DOCUMENT"
  | "OTHER";

export interface AttachmentDto {
  id: string;
  fileName: string | null;
  contentType: string | null;
  fileSize: number | null;
  storageProvider?: string | null;
  storageKey?: string | null;
  leadId?: string | null;
  jobId?: string | null;
  description?: string | null;
  tag?: AttachmentTag | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface JobReceiptDto {
  id: string;
  fileName: string;
  contentType?: string | null;
  fileSize?: number | null;
  description?: string | null;
  uploadedAt?: string | null;
  linkedCostEntryId?: string | null;
  linkedCostEntryDescription?: string | null;
  linkedCostEntryAmount?: number | null;
  extractionStatus?: ReceiptExtractionStatus | null;
  extractedAt?: string | null;
  extractionError?: string | null;
  extractionConfidence?: number | null;
  extractionResult?: ReceiptExtractionResultDto | null;
}

export interface CreateCostFromReceiptRequest {
  category: JobCostCategory;
  vendorName?: string | null;
  description: string;
  amount: number;
  incurredAt: string;
  notes?: string | null;
}

export type ReceiptExtractionStatus =
  | "NOT_STARTED"
  | "PROCESSING"
  | "COMPLETED"
  | "FAILED";

export type ReceiptAmountConfidence = "HIGH" | "MEDIUM" | "LOW";

export type ReceiptFieldConfidence = "UNKNOWN" | "LOW" | "MEDIUM" | "HIGH";

/** Final reconciled extraction values (match backend ReceiptExtractionResultDto). */
export interface ReceiptExtractionResultDto {
  vendorName?: string | null;
  incurredAt?: string | null;
  amount?: number | null;
  extractedSubtotal?: number | null;
  extractedTax?: number | null;
  extractedTotal?: number | null;
  extractedAmountPaid?: number | null;
  computedTotal?: number | null;
  subtotalConfidence?: ReceiptFieldConfidence | null;
  taxConfidence?: ReceiptFieldConfidence | null;
  totalConfidence?: ReceiptFieldConfidence | null;
  amountPaidConfidence?: ReceiptFieldConfidence | null;
  summaryRegionSubtotal?: number | null;
  summaryRegionTax?: number | null;
  summaryRegionTotal?: number | null;
  summaryRegionAmountPaid?: number | null;
  extractedTaxRatePercent?: number | null;
  amountCandidates?: number[] | null;
  amountConfidence?: ReceiptAmountConfidence | null;
  suggestedCategory?: JobCostCategory | null;
  notes?: string | null;
  confidence?: number | null;
  rawExtractedText?: string | null;
  summaryRegionRawText?: string | null;
  extractionWarnings?: string[] | null;
}

export interface ExtractReceiptResponseDto {
  receiptId: string;
  status: ReceiptExtractionStatus;
  extractedAt?: string | null;
  error?: string | null;
  confidence?: number | null;
  result?: ReceiptExtractionResultDto | null;
}

export interface ConfirmReceiptCostRequest {
  category: JobCostCategory;
  vendorName?: string | null;
  description: string;
  amount: number;
  incurredAt: string;
  notes?: string | null;
}

// Task-related types
export type TaskStatus = "TODO" | "IN_PROGRESS" | "COMPLETED" | "CANCELED";
export type TaskPriority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

export interface TaskDto {
  taskId: string;
  title: string;
  description?: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueAt?: string | null;
  completedAt?: string | null;
  assignedToUserId?: string | null;
  assignedToName?: string | null;
  leadId?: string | null;
  jobId?: string | null;
  customerId?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTaskRequest {
  title: string;
  description?: string | null;
  status?: TaskStatus;
  priority?: TaskPriority;
  dueAt?: string | null;
  assignedToUserId?: string | null;
  leadId?: string | null;
  jobId?: string | null;
  customerId?: string | null;
}

export interface UpdateTaskRequest {
  title?: string;
  description?: string | null;
  status?: TaskStatus;
  priority?: TaskPriority;
  dueAt?: string | null;
  assignedToUserId?: string | null;
  leadId?: string | null;
  jobId?: string | null;
  customerId?: string | null;
}

// Communication log types (match backend channel/direction strings)
export type CommunicationChannel = "CALL" | "SMS" | "EMAIL" | "NOTE";
export type CommunicationDirection = "INBOUND" | "OUTBOUND";

export interface CommunicationLogDto {
  id: string;
  channel: string;
  direction: string | null;
  subject: string | null;
  body: string | null;
  occurredAt: string;
  leadId?: string | null;
  jobId?: string | null;
  createdAt?: string | null;
  updatedAt?: string | null;
}

export interface CreateCommunicationLogRequest {
  channel: string;
  direction?: string | null;
  subject: string;
  body?: string | null;
  occurredAt?: string | null;
}

// Activity timeline types
export type ActivityEntityType = "LEAD" | "JOB";
export type ActivityEventType =
  | "NOTE"
  | "LEAD_STATUS_CHANGED"
  | "JOB_STATUS_CHANGED"
  | "JOB_SCHEDULE_CHANGED"
  | "TASK_CREATED"
  | "TASK_STATUS_CHANGED"
  | "LEAD_CONVERTED_TO_JOB"
  | "ATTACHMENT_ADDED"
  | "ESTIMATE_SHARED"
  | "ESTIMATE_EMAIL_SENT"
  | "ESTIMATE_ACCEPTED"
  | "ESTIMATE_REJECTED"
  | "INVOICE_CREATED"
  | "INVOICE_SHARED"
  | "INVOICE_EMAIL_SENT"
  | "INVOICE_STATUS_CHANGED"
  | "COST_ENTRY_CREATED"
  | "COST_ENTRY_UPDATED"
  | "COST_ENTRY_DELETED"
  | "RECEIPT_UPLOADED"
  | "RECEIPT_LINKED_TO_COST"
  | "RECEIPT_UNLINKED_FROM_COST"
  | "RECEIPT_DELETED";

export interface ActivityEventDto {
  activityId: string;
  entityType: ActivityEntityType;
  entityId: string;
  eventType: ActivityEventType;
  message: string;
  createdAt: string;
  createdByUserId?: string | null;
  createdByName?: string | null;
  metadata?: Record<string, unknown> | null;
}

export interface CreateNoteRequest {
  entityType: ActivityEntityType;
  entityId: string;
  body: string;
}

// Dashboard (GET /api/v1/dashboard/summary)
export interface DashboardLeadSnippetDto {
  id: string;
  statusKey: string;
  statusLabel: string;
  customerLabel: string;
  propertyLine1?: string | null;
  updatedAt: string;
}

export interface DashboardJobSnippetDto {
  id: string;
  statusKey: string;
  statusLabel: string;
  scheduledStartDate?: string | null;
  propertyLine1?: string | null;
  customerLabel: string;
}

export interface DashboardTaskSnippetDto {
  taskId: string;
  title: string;
  status: TaskStatus;
  dueAt?: string | null;
  leadId?: string | null;
  jobId?: string | null;
  customerId?: string | null;
}

export interface DashboardSummaryDto {
  customerCount: number;
  leadCount: number;
  jobCount: number;
  estimateCount: number;
  invoiceCount: number;
  openTaskCount: number;
  leadCountByStatus: Record<string, number>;
  jobCountByStatus: Record<string, number>;
  jobsScheduledThisWeek: number;
  unscheduledJobsCount: number;
  estimatesSentCount: number;
  unpaidInvoiceCount: number;
  activePipelineLeadCount: number;
  recentLeads: DashboardLeadSnippetDto[];
  upcomingJobs: DashboardJobSnippetDto[];
  openTasks: DashboardTaskSnippetDto[];
}

/** Customer-facing photo + text report (inspection, before/after, scope). */
export interface CustomerPhotoReportSectionPhotoDto {
  attachmentId: string;
  sortOrder: number;
}

export interface CustomerPhotoReportSectionDto {
  id?: string;
  sortOrder: number;
  title: string;
  body?: string | null;
  photos: CustomerPhotoReportSectionPhotoDto[];
}

export interface CustomerPhotoReportDto {
  id: string;
  customerId: string;
  customerName: string;
  customerEmail?: string | null;
  jobId?: string | null;
  jobDisplayName?: string | null;
  title: string;
  reportType?: string | null;
  summary?: string | null;
  sections: CustomerPhotoReportSectionDto[];
  createdAt?: string;
  updatedAt?: string;
}

export interface CustomerPhotoReportSummaryDto {
  id: string;
  title: string;
  reportType?: string | null;
  customerId: string;
  customerName: string;
  jobId?: string | null;
  jobDisplayName?: string | null;
  updatedAt?: string;
}

export interface CustomerPhotoReportSectionRequest {
  title: string;
  body?: string | null;
  attachmentIds: string[];
}

export interface UpsertCustomerPhotoReportRequest {
  customerId: string;
  jobId?: string | null;
  title: string;
  reportType?: string | null;
  summary?: string | null;
  sections: CustomerPhotoReportSectionRequest[];
}

export interface SendCustomerPhotoReportEmailRequest {
  recipientEmail: string;
  recipientName?: string;
  subject?: string;
  message?: string;
}

export interface SendCustomerPhotoReportEmailResponse {
  success: boolean;
  sentAt: string;
}

// App Preferences (tenant-level customization)
export interface AppPreferencesDto {
  dashboard: Record<string, unknown>;
  jobsList: Record<string, unknown>;
  leadsList: Record<string, unknown>;
  customersList: Record<string, unknown>;
  tasksList: Record<string, unknown>;
  estimatesList: Record<string, unknown>;
  updatedAt: string | null;
}

export interface UpdateAppPreferencesRequest {
  dashboard?: Record<string, unknown>;
  jobsList?: Record<string, unknown>;
  leadsList?: Record<string, unknown>;
  customersList?: Record<string, unknown>;
  tasksList?: Record<string, unknown>;
  estimatesList?: Record<string, unknown>;
}
