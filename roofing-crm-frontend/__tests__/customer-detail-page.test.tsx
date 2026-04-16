import React from "react";
import { render, screen, waitFor, fireEvent, within } from "./test-utils";
import CustomerDetailPage from "@/app/app/customers/[customerId]/page";
import * as customersApi from "@/lib/customersApi";
import * as jobsApi from "@/lib/jobsApi";
import * as leadsApi from "@/lib/leadsApi";
import { CustomerDto } from "@/lib/types";
import { PageResponse } from "@/lib/types";
import { JobDto, LeadDto } from "@/lib/types";

jest.mock("@/lib/customersApi");
jest.mock("@/lib/jobsApi");
jest.mock("@/lib/leadsApi");
const mockedCustomersApi = customersApi as jest.Mocked<typeof customersApi>;
const mockedJobsApi = jobsApi as jest.Mocked<typeof jobsApi>;
const mockedLeadsApi = leadsApi as jest.Mocked<typeof leadsApi>;

const mockPush = jest.fn();
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/customers/cust-1",
  useParams: () => ({ customerId: "cust-1" }),
}));

const mockCustomer: CustomerDto = {
  id: "cust-1",
  firstName: "Jane",
  lastName: "Doe",
  primaryPhone: "555-111-2222",
  email: "jane@example.com",
  notes: null,
  billingAddress: null,
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

const emptyJobs: PageResponse<JobDto> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 10,
  first: true,
  last: true,
};

const emptyLeads: PageResponse<LeadDto> = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 10,
  first: true,
  last: true,
};

describe("CustomerDetailPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockedCustomersApi.getCustomer.mockResolvedValue(mockCustomer);
    mockedJobsApi.listJobs.mockResolvedValue(emptyJobs);
    mockedLeadsApi.listLeads.mockResolvedValue(emptyLeads);
  });

  it("renders sidebar with Details card above Actions card", async () => {
    render(<CustomerDetailPage />);

    await waitFor(() => {
      expect(screen.getByText("Details")).toBeInTheDocument();
      expect(screen.getByText("Actions")).toBeInTheDocument();
    });

    const allH2 = screen.getAllByRole("heading", { level: 2 });
    const texts = allH2.map((h) => h.textContent);
    const detailsIdx = texts.indexOf("Details");
    const actionsIdx = texts.indexOf("Actions");
    expect(detailsIdx).toBeGreaterThanOrEqual(0);
    expect(actionsIdx).toBeGreaterThanOrEqual(0);
    expect(detailsIdx).toBeLessThan(actionsIdx);
  });

  it("renders a fixed left section rail with customer anchors", async () => {
    render(<CustomerDetailPage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { level: 1 })).toHaveTextContent("Jane Doe");
    });

    const railContainer = screen.getByTestId("customer-section-nav-rail-container");
    expect(railContainer).toHaveClass("hidden", "lg:block", "lg:fixed", "lg:left-6");

    const railNav = screen.getByTestId("detail-section-nav-rail");
    const expectedLinks = [
      ["Customer", "#customer-information"],
      ["Related Jobs", "#related-jobs"],
      ["Related Leads", "#related-leads"],
    ] as const;

    for (const [label, href] of expectedLinks) {
      expect(within(railNav).getByRole("link", { name: label })).toHaveAttribute("href", href);
      expect(document.getElementById(href.slice(1))).toBeInTheDocument();
    }
  });

  it("clicking a related job row navigates to the job detail page", async () => {
    mockedJobsApi.listJobs.mockResolvedValue({
      ...emptyJobs,
      content: [
        {
          id: "job-1",
          customerId: "cust-1",
          leadId: null,
          statusDefinitionId: "status-1",
          statusKey: "SCHEDULED",
          statusLabel: "Scheduled",
          type: "REPLACEMENT",
          propertyAddress: { line1: "123 Main St" },
          scheduledStartDate: null,
          scheduledEndDate: null,
          internalNotes: null,
          crewName: null,
          createdAt: "2024-01-01T00:00:00Z",
          updatedAt: "2024-01-01T00:00:00Z",
        },
      ],
    });

    render(<CustomerDetailPage />);

    const row = await screen.findByTestId("related-job-row-job-1");
    fireEvent.click(row);

    expect(mockPush).toHaveBeenCalledWith("/app/jobs/job-1");
  });

  it("clicking a related lead row navigates to the lead detail page", async () => {
    mockedLeadsApi.listLeads.mockResolvedValue({
      ...emptyLeads,
      content: [
        {
          id: "lead-1",
          customerId: "cust-1",
          statusDefinitionId: "status-1",
          statusKey: "NEW",
          statusLabel: "New",
          propertyAddress: { line1: "456 Elm St" },
          createdAt: "2024-01-01T00:00:00Z",
          updatedAt: "2024-01-01T00:00:00Z",
        },
      ],
    });

    render(<CustomerDetailPage />);

    const row = await screen.findByTestId("related-lead-row-lead-1");
    fireEvent.click(row);

    expect(mockPush).toHaveBeenCalledWith("/app/leads/lead-1");
  });
});
