import { AxiosError } from "axios";
import { getApiErrorMessage } from "./apiError";

function axiosResponseError(status: number, data?: Record<string, unknown>): AxiosError {
  return new AxiosError("fail", "ERR_BAD_RESPONSE", undefined, undefined, {
    status,
    statusText: status === 401 ? "Unauthorized" : "Error",
    data: data ?? {},
    headers: {},
    config: {} as never,
  });
}

describe("getApiErrorMessage", () => {
  it("uses session-expired wording only for HTTP 401", () => {
    expect(getApiErrorMessage(axiosResponseError(401), "fallback")).toBe(
      "Your session expired. Please sign in again."
    );
  });

  it("prefers API message body for non-401 errors", () => {
    expect(
      getApiErrorMessage(
        axiosResponseError(500, { message: "Dashboard query failed" }),
        "fallback"
      )
    ).toBe("Dashboard query failed");
  });

  it("does not treat403 as session expiry", () => {
    expect(
      getApiErrorMessage(axiosResponseError(403, { message: "Forbidden" }), "fallback")
    ).toBe("Forbidden");
  });
});
