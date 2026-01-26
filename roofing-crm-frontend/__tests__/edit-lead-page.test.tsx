import React from "react";
import { render, screen, waitFor, fireEvent } from "./test-utils";
import EditLeadPage from "@/app/app/leads/[leadId]/edit/page";
import * as leadsApi from "@/lib/leadsApi";
import { LeadDto } from "@/lib/types";

const mockPush = jest.fn();

jest.mock("@/lib/leadsApi");
jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: mockPush, replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/leads/lead-1/edit",
  useParams: () => ({ leadId: "lead-1" }),
  useSearchParams: () => new URLSearchParams(),
}));

const mockedLeadsApi = leadsApi as jest.Mocked<typeof leadsApi>;

const mockLead: LeadDto = {
  id: "lead-1",
  customerId: "cust-1",
  status: "NEW",
  source: "WEBSITE",
  leadNotes: "Original notes",
  propertyAddress: { line1: "123 Main St", line2: "", city: "Denver", state: "CO", zip: "80202" },
  preferredContactMethod: "Phone",
  createdAt: "2024-01-01T00:00:00Z",
  updatedAt: "2024-01-01T00:00:00Z",
  customerFirstName: "John",
  customerLastName: "Doe",
};

describe("EditLeadPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockPush.mockClear();
    mockedLeadsApi.getLead.mockResolvedValue(mockLead);
    mockedLeadsApi.updateLead.mockResolvedValue({ ...mockLead, leadNotes: "Updated notes" });
  });

  it("prefills form from lead and submit calls updateLead then navigates to detail", async () => {
    render(<EditLeadPage />);

    await waitFor(() => {
      expect(screen.getByDisplayValue("Original notes")).toBeInTheDocument();
    });
    expect(screen.getByDisplayValue("123 Main St")).toBeInTheDocument();
    expect(screen.getByDisplayValue("Denver")).toBeInTheDocument();

    fireEvent.change(screen.getByLabelText(/Notes/i), { target: { value: "Updated notes" } });

    fireEvent.click(screen.getByRole("button", { name: /Save changes/i }));

    await waitFor(() => {
      expect(mockedLeadsApi.updateLead).toHaveBeenCalledWith(
        expect.anything(),
        "lead-1",
        expect.objectContaining({
          leadNotes: "Updated notes",
          propertyAddress: expect.any(Object),
        })
      );
    });

    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/app/leads/lead-1");
    });
  });

  it("shows loading then form", async () => {
    let resolve: (v: LeadDto) => void;
    mockedLeadsApi.getLead.mockImplementation(() => new Promise((r) => { resolve = r; }));

    render(<EditLeadPage />);
    expect(screen.getByText("Loading lead…")).toBeInTheDocument();

    resolve!(mockLead);

    await waitFor(() => {
      expect(screen.getByRole("button", { name: /Save changes/i })).toBeInTheDocument();
    });
  });
});
