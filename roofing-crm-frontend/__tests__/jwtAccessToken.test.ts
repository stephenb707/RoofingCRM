import { getJwtExpiryMs, isJwtExpired } from "@/lib/jwtAccessToken";

function encodePayload(payload: object): string {
  const json = JSON.stringify(payload);
  const b64 = Buffer.from(json, "utf8").toString("base64url");
  return `x.${b64}.y`;
}

describe("jwtAccessToken", () => {
  it("isJwtExpired is false when exp is in the future", () => {
    const future = Math.floor(Date.now() / 1000) + 3600;
    const token = encodePayload({ exp: future });
    expect(isJwtExpired(token, 60_000)).toBe(false);
  });

  it("isJwtExpired is true when exp is in the past", () => {
    const past = Math.floor(Date.now() / 1000) - 120;
    const token = encodePayload({ exp: past });
    expect(isJwtExpired(token, 0)).toBe(true);
  });

  it("getJwtExpiryMs returns null for malformed token", () => {
    expect(getJwtExpiryMs("not-jwt")).toBeNull();
  });
});
