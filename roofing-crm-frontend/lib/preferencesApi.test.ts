import { AxiosInstance } from "axios";
import { getAppPreferences, updateAppPreferences } from "./preferencesApi";
import { AppPreferencesDto, UpdateAppPreferencesRequest } from "./types";

const createMockApi = () => ({
  get: jest.fn(),
  post: jest.fn(),
  put: jest.fn(),
  delete: jest.fn(),
});

const mockPrefs: AppPreferencesDto = {
  dashboard: { widgets: ["metrics", "quickActions", "recentLeads"] },
  jobsList: { visibleFields: ["type", "status", "propertyAddress"] },
  leadsList: { visibleFields: ["customer", "status", "source"] },
  customersList: { visibleFields: ["name", "phone", "email"] },
  tasksList: { visibleFields: ["title", "status", "priority"] },
   estimatesList: { visibleFields: ["title", "status", "total"] },
  pipeline: {},
  updatedAt: null,
};

describe("preferencesApi", () => {
  describe("getAppPreferences", () => {
    it("calls GET /api/v1/settings/preferences", async () => {
      const mockApi = createMockApi();
      (mockApi.get as jest.Mock).mockResolvedValue({ data: mockPrefs });

      const result = await getAppPreferences(
        mockApi as unknown as AxiosInstance
      );

      expect(mockApi.get).toHaveBeenCalledWith(
        "/api/v1/settings/preferences"
      );
      expect(result).toEqual(mockPrefs);
    });
  });

  describe("updateAppPreferences", () => {
    it("calls PUT /api/v1/settings/preferences with payload", async () => {
      const mockApi = createMockApi();
      const updated = { ...mockPrefs, updatedAt: "2026-04-15T00:00:00Z" };
      (mockApi.put as jest.Mock).mockResolvedValue({ data: updated });

      const payload: UpdateAppPreferencesRequest = {
        dashboard: { widgets: ["metrics"] },
      };

      const result = await updateAppPreferences(
        mockApi as unknown as AxiosInstance,
        payload
      );

      expect(mockApi.put).toHaveBeenCalledWith(
        "/api/v1/settings/preferences",
        payload
      );
      expect(result.updatedAt).toBe("2026-04-15T00:00:00Z");
    });
  });
});
