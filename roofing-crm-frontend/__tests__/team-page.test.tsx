import React from "react";
import { render, screen, waitFor, fireEvent, mockAuthValue } from "./test-utils";
import TeamPage from "@/app/app/team/page";
import * as teamApi from "@/lib/teamApi";

jest.mock("@/lib/teamApi");
const mockedTeamApi = teamApi as jest.Mocked<typeof teamApi>;

describe("TeamPage", () => {
  const mockMembers = [
    {
      userId: "user-123",
      email: "owner@test.com",
      fullName: "Owner User",
      role: "OWNER" as const,
    },
    {
      userId: "user-456",
      email: "sales@test.com",
      fullName: "Sales User",
      role: "SALES" as const,
    },
  ];

  const mockInvites = [
    {
      inviteId: "inv-1",
      email: "invited@test.com",
      role: "ADMIN" as const,
      expiresAt: "2025-02-08T00:00:00Z",
      createdAt: "2025-02-01T00:00:00Z",
      createdByName: "Owner User",
    },
  ];

  const defaultTenant = {
    tenantId: "tenant-123",
    tenantName: "Test Company",
    tenantSlug: "test-company",
    role: "OWNER",
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockedTeamApi.listMembers.mockResolvedValue(mockMembers);
    mockedTeamApi.listInvites.mockResolvedValue(mockInvites);
    mockAuthValue.auth.tenants = [{ ...defaultTenant }];
    mockAuthValue.auth.userId = "user-123";
    mockAuthValue.auth.selectedTenantId = "tenant-123";
  });

  it("renders members list from teamApi.listMembers", async () => {
    render(<TeamPage />);

    await waitFor(() => {
      expect(screen.getByText("Team")).toBeInTheDocument();
    });

    await waitFor(() => {
      expect(mockedTeamApi.listMembers).toHaveBeenCalled();
    });

    expect(screen.getByText("Owner User")).toBeInTheDocument();
    expect(screen.getByText("Sales User")).toBeInTheDocument();
  });

  it("if role is ADMIN, invite form is visible but role-change/remove are disabled", async () => {
    mockAuthValue.auth.tenants[0].role = "ADMIN";

    render(<TeamPage />);

    await waitFor(() => {
      expect(screen.getByText("Team")).toBeInTheDocument();
    });

    expect(screen.getByPlaceholderText("colleague@company.com")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Invite/i })).toBeInTheDocument();

    await waitFor(() => {
      expect(screen.getByText("Sales User")).toBeInTheDocument();
    });

    // ADMIN cannot manage roles - no role dropdown or Remove button for other users
    expect(screen.getByText("SALES")).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: /Remove/i })).not.toBeInTheDocument();
  });

  it("if role is OWNER, role-change triggers teamApi.updateMemberRole", async () => {
    mockedTeamApi.updateMemberRole.mockResolvedValue({
      ...mockMembers[1],
      role: "ADMIN",
    });

    render(<TeamPage />);

    await waitFor(() => {
      expect(screen.getByText("Sales User")).toBeInTheDocument();
    });

    const roleSelects = screen.getAllByRole("combobox");
    expect(roleSelects.length).toBeGreaterThan(0);
    const salesRow = screen.getByText("Sales User").closest("tr");
    const roleSelectInRow = salesRow?.querySelector("select");
    expect(roleSelectInRow).toBeInTheDocument();
    fireEvent.change(roleSelectInRow!, { target: { value: "ADMIN" } });

    await waitFor(() => {
      expect(mockedTeamApi.updateMemberRole).toHaveBeenCalledWith(
        expect.anything(),
        "user-456",
        "ADMIN"
      );
    });
  });

  it("invite create triggers teamApi.createInvite and renders email success state", async () => {
    const createdInvite = {
      inviteId: "inv-new",
      email: "new@test.com",
      role: "SALES" as const,
      expiresAt: "2025-02-08T00:00:00Z",
      createdAt: "2025-02-01T00:00:00Z",
    };
    mockedTeamApi.createInvite.mockResolvedValue(createdInvite);

    render(<TeamPage />);

    await waitFor(() => {
      expect(screen.getByPlaceholderText("colleague@company.com")).toBeInTheDocument();
    });

    const emailInput = screen.getByPlaceholderText("colleague@company.com");
    fireEvent.change(emailInput, { target: { value: "new@test.com" } });
    fireEvent.click(screen.getByRole("button", { name: /Invite/i }));

    await waitFor(() => {
      expect(mockedTeamApi.createInvite).toHaveBeenCalledWith(
        expect.anything(),
        expect.objectContaining({
          email: "new@test.com",
          role: "SALES",
        })
      );
    });

    await waitFor(() => {
      expect(
        screen.getByText("Invitation email sent to new@test.com.")
      ).toBeInTheDocument();
    });
    expect(screen.queryByRole("button", { name: /Copy/i })).not.toBeInTheDocument();
  });

  it("pending invites render resend and revoke actions", async () => {
    mockedTeamApi.resendInvite.mockResolvedValue(mockInvites[0]);

    render(<TeamPage />);

    await waitFor(() => {
      expect(screen.getByText(/invited@test.com/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole("button", { name: /Resend/i }));

    await waitFor(() => {
      expect(mockedTeamApi.resendInvite).toHaveBeenCalledWith(
        expect.anything(),
        "inv-1"
      );
    });

    expect(screen.getByRole("button", { name: /Revoke/i })).toBeInTheDocument();
  });
});
