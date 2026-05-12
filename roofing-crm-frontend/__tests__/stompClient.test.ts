import {
  formatWebSocketCloseDiagnostics,
  stompErrorLooksLikeAuthFailure,
  stompFrameSummary,
} from "@/lib/stompDiagnostics";

describe("stompDiagnostics", () => {
  it("stompFrameSummary reads command body headers", () => {
    expect(
      stompFrameSummary({
        command: "ERROR",
        body: "oops",
        headers: { message: "not allowed" },
      })
    ).toEqual({
      command: "ERROR",
      body: "oops",
      headers: { message: "not allowed" },
    });
  });

  it("stompFrameSummary handles nullish frame", () => {
    expect(stompFrameSummary(null).body).toBe("(empty)");
  });

  it("stompErrorLooksLikeAuthFailure detects typical broker messages", () => {
    expect(
      stompErrorLooksLikeAuthFailure({ body: "Access denied for this destination" })
    ).toBe(true);
    expect(stompErrorLooksLikeAuthFailure({ headers: { message: "401 Unauthorized" } })).toBe(
      true
    );
    expect(stompErrorLooksLikeAuthFailure({ body: "Some other broker issue" })).toBe(false);
  });

  it("formatWebSocketCloseDiagnostics includes code and reason", () => {
    const ev = new Event("close") as CloseEvent;
    Object.assign(ev, { code: 4000, reason: "gone", wasClean: false });
    expect(formatWebSocketCloseDiagnostics(ev)).toContain("code=4000");
    expect(formatWebSocketCloseDiagnostics(ev)).toContain("gone");
  });
});

describe("stompClient auth reconnect", () => {
  const { createStompClient } = jest.requireActual("@/lib/stompClient") as typeof import("@/lib/stompClient");

  function tokenWithExp(exp: number): string {
    return `x.${Buffer.from(JSON.stringify({ exp })).toString("base64url")}.y`;
  }

  it("refreshes an expired access token before reconnecting", async () => {
    const expired = tokenWithExp(Math.floor(Date.now() / 1000) - 60);
    const fresh = tokenWithExp(Math.floor(Date.now() / 1000) + 3600);
    const refreshAccessToken = jest.fn().mockResolvedValue(fresh);
    const client = createStompClient({
      getToken: () => expired,
      refreshAccessToken,
      onConnect: jest.fn(),
    });

    await client.beforeConnect(client);

    expect(refreshAccessToken).toHaveBeenCalledTimes(1);
    expect(client.reconnectDelay).toBeGreaterThan(0);
  });

  it("stops reconnecting when refresh fails", async () => {
    const expired = tokenWithExp(Math.floor(Date.now() / 1000) - 60);
    const refreshAccessToken = jest.fn().mockResolvedValue(null);
    const onConnectionIssue = jest.fn();
    const client = createStompClient({
      getToken: () => expired,
      refreshAccessToken,
      onConnect: jest.fn(),
      onConnectionIssue,
    });

    await client.beforeConnect(client);

    expect(refreshAccessToken).toHaveBeenCalledTimes(1);
    expect(client.reconnectDelay).toBe(0);
    expect(onConnectionIssue).toHaveBeenCalledWith(
      expect.objectContaining({ kind: "auth", terminal: true })
    );
  });
});
