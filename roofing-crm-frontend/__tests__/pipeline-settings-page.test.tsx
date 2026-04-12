import React from "react";
import { render, screen, waitFor, mockAuthValue, within } from "./test-utils";
import userEvent from "@testing-library/user-event";
import PipelineStatusSettingsPage from "@/app/app/settings/pipeline-statuses/page";
import * as pipelineStatusesApi from "@/lib/pipelineStatusesApi";

jest.mock("@/lib/pipelineStatusesApi");

const mockedApi = pipelineStatusesApi as jest.Mocked<typeof pipelineStatusesApi>;

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn(), back: jest.fn() }),
  usePathname: () => "/app/settings/pipeline-statuses",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
}));

describe("PipelineStatusSettingsPage", () => {
  const defaultTenant = {
    tenantId: "tenant-123",
    tenantName: "Test Company",
    tenantSlug: "test-company",
    role: "ADMIN" as const,
  };

  const leadA = {
    id: "lead-def-a",
    pipelineType: "LEAD" as const,
    systemKey: "NEW",
    label: "New",
    sortOrder: 0,
    builtIn: true,
    active: true,
  };

  const leadB = {
    id: "lead-def-b",
    pipelineType: "LEAD" as const,
    systemKey: "CONTACTED",
    label: "Contacted",
    sortOrder: 1,
    builtIn: true,
    active: true,
  };

  const leadCustom = {
    id: "lead-def-custom",
    pipelineType: "LEAD" as const,
    systemKey: "CUSTOM_X",
    label: "Custom X",
    sortOrder: 2,
    builtIn: false,
    active: true,
  };

  const jobA = {
    id: "job-def-a",
    pipelineType: "JOB" as const,
    systemKey: "SCHEDULED",
    label: "Scheduled",
    sortOrder: 0,
    builtIn: true,
    active: true,
  };

  beforeEach(() => {
    jest.clearAllMocks();
    mockAuthValue.auth.tenants = [{ ...defaultTenant }];
    mockAuthValue.auth.selectedTenantId = "tenant-123";
    mockAuthValue.auth.token = "t";

    mockedApi.listSettingsPipelineStatuses.mockImplementation((_api, type) => {
      if (type === "LEAD") return Promise.resolve([leadA, leadB, leadCustom]);
      return Promise.resolve([jobA]);
    });
    mockedApi.updateSettingsPipelineStatus.mockImplementation((_api, id, body) => {
      const all = [leadA, leadB, leadCustom, jobA];
      const found = all.find((d) => d.id === id)!;
      return Promise.resolve({ ...found, label: body.label });
    });
    mockedApi.createSettingsPipelineStatus.mockResolvedValue({
      id: "new-id",
      pipelineType: "LEAD",
      systemKey: "CUSTOM_NEW",
      label: "Fresh",
      sortOrder: 99,
      builtIn: false,
      active: true,
    });
    mockedApi.reorderSettingsPipelineStatuses.mockResolvedValue(undefined);
    mockedApi.restoreDefaultPipelineStatuses.mockResolvedValue(undefined);
    mockedApi.deactivateSettingsPipelineStatus.mockResolvedValue(undefined);
  });

  it("renders lead and job status sections", async () => {
    render(<PipelineStatusSettingsPage />);

    await waitFor(() => {
      expect(screen.getByTestId("pipeline-settings-page")).toBeInTheDocument();
    });

    expect(screen.getByTestId("pipeline-settings-lead-section")).toBeInTheDocument();
    expect(screen.getByTestId("pipeline-settings-job-section")).toBeInTheDocument();
    expect(mockedApi.listSettingsPipelineStatuses).toHaveBeenCalledWith(
      expect.anything(),
      "LEAD"
    );
    expect(mockedApi.listSettingsPipelineStatuses).toHaveBeenCalledWith(
      expect.anything(),
      "JOB"
    );
  });

  it("shows built-in vs custom labels", async () => {
    render(<PipelineStatusSettingsPage />);

    await waitFor(() => {
      expect(screen.getByTestId(`status-kind-${leadA.id}`)).toHaveTextContent("Built-in");
    });
    expect(screen.getByTestId(`status-kind-${leadCustom.id}`)).toHaveTextContent("Custom");
  });

  it("rename saves via update API", async () => {
    const user = userEvent.setup();
    render(<PipelineStatusSettingsPage />);

    await waitFor(() => {
      expect(screen.getByLabelText(/Label for status NEW/i)).toBeInTheDocument();
    });

    const input = screen.getByLabelText(/Label for status NEW/i);
    await user.clear(input);
    await user.type(input, "Intake");
    await user.click(screen.getAllByRole("button", { name: "Save" })[0]!);

    await waitFor(() => {
      expect(mockedApi.updateSettingsPipelineStatus).toHaveBeenCalledWith(
        expect.anything(),
        leadA.id,
        { label: "Intake" }
      );
    });
  });

  it("add custom status calls create API", async () => {
    const user = userEvent.setup();
    render(<PipelineStatusSettingsPage />);

    await waitFor(() => {
      expect(screen.getByTestId("add-status-input-LEAD")).toBeInTheDocument();
    });

    await user.type(screen.getByTestId("add-status-input-LEAD"), "Follow-up");
    await user.click(screen.getByTestId("add-status-submit-LEAD"));

    await waitFor(() => {
      expect(mockedApi.createSettingsPipelineStatus).toHaveBeenCalledWith(expect.anything(), {
        pipelineType: "LEAD",
        label: "Follow-up",
      });
    });
  });

  it("move down calls reorder API with swapped ids for lead section", async () => {
    const user = userEvent.setup();
    render(<PipelineStatusSettingsPage />);

    await waitFor(() => {
      expect(screen.getByTestId("pipeline-settings-lead-section")).toBeInTheDocument();
    });

    const leadSection = screen.getByTestId("pipeline-settings-lead-section");
    const moveDownButtons = within(leadSection).getAllByRole("button", { name: "Move down" });
    await user.click(moveDownButtons[0]!);

    await waitFor(() => {
      expect(mockedApi.reorderSettingsPipelineStatuses).toHaveBeenCalledWith(
        expect.anything(),
        {
          pipelineType: "LEAD",
          orderedDefinitionIds: [leadB.id, leadA.id, leadCustom.id],
        }
      );
    });
  });

  it("restore defaults opens confirm and calls restore API", async () => {
    const user = userEvent.setup();
    render(<PipelineStatusSettingsPage />);

    await waitFor(() => {
      expect(screen.getByTestId("restore-defaults-open-LEAD")).toBeInTheDocument();
    });

    await user.click(screen.getByTestId("restore-defaults-open-LEAD"));
    await user.click(screen.getByTestId("restore-deactivate-unused-LEAD"));
    await user.click(screen.getByTestId("restore-defaults-confirm-LEAD"));

    await waitFor(() => {
      expect(mockedApi.restoreDefaultPipelineStatuses).toHaveBeenCalledWith(
        expect.anything(),
        "LEAD",
        true
      );
    });
  });

  it("non-admin users see access message", async () => {
    mockAuthValue.auth.tenants[0]!.role = "SALES";
    render(<PipelineStatusSettingsPage />);

    await waitFor(() => {
      expect(
        screen.getByText(/Only owners and admins can manage pipeline statuses/i)
      ).toBeInTheDocument();
    });
    expect(mockedApi.listSettingsPipelineStatuses).not.toHaveBeenCalled();
  });
});
