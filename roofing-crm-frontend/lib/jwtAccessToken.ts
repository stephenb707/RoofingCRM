/**
 * Minimal JWT payload helpers for the browser (no signature verification).
 * Used to avoid infinite STOMP reconnect attempts with an already-expired access token.
 */

const B64URL_TO_B64 = /-/g;

export function getJwtExpiryMs(token: string): number | null {
  try {
    const parts = token.split(".");
    if (parts.length < 2) return null;
    const json = atob(parts[1].replace(B64URL_TO_B64, "+").replace(/_/g, "/"));
    const payload = JSON.parse(json) as { exp?: number };
    if (typeof payload.exp !== "number") return null;
    return payload.exp * 1000;
  } catch {
    return null;
  }
}

/**
 * @param skewMs — refresh/stop reconnects this long before actual expiry
 */
export function isJwtExpired(token: string, skewMs = 60_000): boolean {
  const expMs = getJwtExpiryMs(token);
  if (expMs == null) return false;
  return Date.now() >= expMs - skewMs;
}
