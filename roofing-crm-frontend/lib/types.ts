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
