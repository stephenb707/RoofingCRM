import React from "react";
import { render, waitFor } from "./test-utils";
import PipelineHubPage from "@/app/app/pipeline/page";
import * as preferencesApi from "@/lib/preferencesApi";
import type { AppPreferencesDto } from "@/lib/types";

const replace = jest.fn();

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn(), replace, back: jest.fn(), refresh: jest.fn(), prefetch: jest.fn() }),
  usePathname: () => "/app/pipeline",
  useParams: () => ({}),
  useSearchParams: () => new URLSearchParams(),
}));

jest.mock("@/lib/preferencesApi");

const mockedPrefsApi = preferencesApi as jest.Mocked<typeof preferencesApi>;

const basePrefs = (): AppPreferencesDto => ({
  dashboard: {},
  jobsList: {},
  leadsList: {},
  customersList: {},
  tasksList: {},
  estimatesList: {},
  pipeline: {},
  updatedAt: null,
});

describe("PipelineHubPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    replace.mockClear();
  });

  it("redirects to lead pipeline when defaultView is leads", async () => {
    mockedPrefsApi.getAppPreferences.mockResolvedValue({
      ...basePrefs(),
      pipeline: { defaultView: "leads" },
    });

    render(<PipelineHubPage />);

    await waitFor(() => {
      expect(replace).toHaveBeenCalledWith("/app/leads/pipeline");
    });
  });

  it("redirects to combined when defaultView is combined", async () => {
    mockedPrefsApi.getAppPreferences.mockResolvedValue({
      ...basePrefs(),
      pipeline: { defaultView: "combined" },
    });

    render(<PipelineHubPage />);

    await waitFor(() => {
      expect(replace).toHaveBeenCalledWith("/app/pipeline/combined");
    });
  });

  it("redirects to jobs pipeline when defaultView is jobs", async () => {
    mockedPrefsApi.getAppPreferences.mockResolvedValue({
      ...basePrefs(),
      pipeline: { defaultView: "jobs" },
    });

    render(<PipelineHubPage />);

    await waitFor(() => {
      expect(replace).toHaveBeenCalledWith("/app/jobs/pipeline");
    });
  });

  it("redirects to lead pipeline when preference is unset (legacy)", async () => {
    mockedPrefsApi.getAppPreferences.mockResolvedValue({
      ...basePrefs(),
      pipeline: {},
    });

    render(<PipelineHubPage />);

    await waitFor(() => {
      expect(replace).toHaveBeenCalledWith("/app/leads/pipeline");
    });
  });
});
