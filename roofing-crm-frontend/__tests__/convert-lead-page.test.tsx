import React from "react";
import { render, screen, waitFor } from "./test-utils";
import userEvent from "@testing-library/user-event";
import ConvertLeadPage from "@/app/app/leads/[leadId]/convert/page";
import * as leadsApi from "@/lib/leadsApi";
import { LeadDto, JobDto } from "@/lib/types";

jest.mock("@/lib/leadsApi");
const mockedLeadsApi = leadsApi as jest.Mocked<typeof leadsApi>;

const mockPush = jest.fn();
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/leads/lead-1/convert",
  useParams: () => ({ leadId: "lead-1" }),
  useSearchParams: () => new URLSearchParams(),
}));

const mockLead: LeadDto = {
  id: "lead-1",
  customerId: "cust-1",
  status: "QUOTE_SENT",
  source: "WEBSITE",
  leadNotes: null,
  propertyAddress: {
    line1: "123 Main St",
    city: "Chicago",
    state: "IL",
    zip: "60601",
  },
  preferredContactMethod: null,
  customerFirstName: "John",
  customerLastName: "Doe",
  customerEmail: "john@example.com",
  customerPhone: "555-1234",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

const mockJob: JobDto = {
  id: "job-1",
  customerId: "cust-1",
  leadId: "lead-1",
  status: "SCHEDULED",
  type: "REPLACEMENT",
  propertyAddress: {
    line1: "123 Main St",
    city: "Chicago",
    state: "IL",
    zip: "60601",
  },
  scheduledStartDate: "2024-02-01",
  scheduledEndDate: null,
  internalNotes: "Test notes",
  crewName: "Team Alpha",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
};

describe("ConvertLeadPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockPush.mockClear();
    mockedLeadsApi.getLead.mockResolvedValue(mockLead);
    mockedLeadsApi.convertLeadToJob.mockResolvedValue(mockJob);
  });

  it("renders form and submits -> calls convertLeadToJob and navigates to job", async () => {
    const user = userEvent.setup();
    render(<ConvertLeadPage />);

    await waitFor(() => {
      expect(screen.getByText("Convert Lead to Job")).toBeInTheDocument();
    });

    // Fill form
    await user.selectOptions(screen.getByLabelText(/job type/i), "REPLACEMENT");
    await user.type(screen.getByLabelText(/scheduled start date/i), "2024-02-01");
    await user.type(screen.getByLabelText(/crew name/i), "Team Alpha");
    await user.type(screen.getByLabelText(/internal notes/i), "Test notes");

    // Submit
    await user.click(screen.getByRole("button", { name: /convert to job/i }));

    await waitFor(() => {
      expect(mockedLeadsApi.convertLeadToJob).toHaveBeenCalledWith(
        expect.any(Object),
        "lead-1",
        {
          type: "REPLACEMENT",
          scheduledStartDate: "2024-02-01",
          scheduledEndDate: null,
          crewName: "Team Alpha",
          internalNotes: "Test notes",
        }
      );
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/app/jobs/job-1");
    });
  });

  it("cancel navigates back to lead detail", async () => {
    const user = userEvent.setup();
    render(<ConvertLeadPage />);

    await waitFor(() => {
      expect(screen.getByText("Convert Lead to Job")).toBeInTheDocument();
    });

    const cancelLink = screen.getByRole("link", { name: /cancel/i });
    expect(cancelLink).toHaveAttribute("href", "/app/leads/lead-1");
  });

  it("shows error message on API failure", async () => {
    const user = userEvent.setup();
    mockedLeadsApi.convertLeadToJob.mockRejectedValue(new Error("API Error"));
    
    render(<ConvertLeadPage />);

    await waitFor(() => {
      expect(screen.getByText("Convert Lead to Job")).toBeInTheDocument();
    });

    await user.selectOptions(screen.getByLabelText(/job type/i), "REPLACEMENT");
    await user.click(screen.getByRole("button", { name: /convert to job/i }));

    await waitFor(() => {
      expect(screen.getByText("API Error")).toBeInTheDocument();
    });
  });

  it("shows lead summary", async () => {
    render(<ConvertLeadPage />);

    await waitFor(() => {
      expect(screen.getByText("Lead Summary")).toBeInTheDocument();
      expect(screen.getByText("John Doe")).toBeInTheDocument();
      expect(screen.getByText(/123 Main St/i)).toBeInTheDocument();
    });
  });

  it("validates required job type", async () => {
    const user = userEvent.setup();
    render(<ConvertLeadPage />);

    await waitFor(() => {
      expect(screen.getByText("Convert Lead to Job")).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: /convert to job/i }));

    await waitFor(() => {
      expect(screen.getByText(/please select a job type/i)).toBeInTheDocument();
    });

    expect(mockedLeadsApi.convertLeadToJob).not.toHaveBeenCalled();
  });

  it("when lead is already converted, shows links and hides form", async () => {
    mockedLeadsApi.getLead.mockResolvedValue({
      ...mockLead,
      convertedJobId: "job-99",
      status: "WON",
    });

    render(<ConvertLeadPage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /lead already converted/i })).toBeInTheDocument();
    });

    expect(screen.getByText(/this lead was already converted to a job/i)).toBeInTheDocument();

    const viewJobLink = screen.getByRole("link", { name: /view job/i });
    expect(viewJobLink).toHaveAttribute("href", "/app/jobs/job-99");

    const createEstimateLink = screen.getByRole("link", { name: /create estimate/i });
    expect(createEstimateLink).toHaveAttribute("href", "/app/jobs/job-99/estimates/new");

    const backToLeadLink = screen.getByRole("link", { name: /back to lead/i });
    expect(backToLeadLink).toHaveAttribute("href", "/app/leads/lead-1");

    expect(screen.queryByLabelText(/job type/i)).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /convert to job/i })).not.toBeInTheDocument();

    expect(mockedLeadsApi.convertLeadToJob).not.toHaveBeenCalled();
  });

  it("when lead is LOST, shows cannot convert message", async () => {
    mockedLeadsApi.getLead.mockResolvedValue({
      ...mockLead,
      status: "LOST",
      convertedJobId: undefined,
    });

    render(<ConvertLeadPage />);

    await waitFor(() => {
      expect(screen.getByRole("heading", { name: /cannot convert this lead/i })).toBeInTheDocument();
    });

    expect(screen.getByText(/leads with status lost cannot be converted/i)).toBeInTheDocument();

    const backToLeadLink = screen.getByRole("link", { name: /back to lead/i });
    expect(backToLeadLink).toHaveAttribute("href", "/app/leads/lead-1");

    expect(screen.queryByLabelText(/job type/i)).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /convert to job/i })).not.toBeInTheDocument();

    expect(mockedLeadsApi.convertLeadToJob).not.toHaveBeenCalled();
  });
});
