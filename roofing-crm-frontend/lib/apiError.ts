import axios from "axios";

/**
 * Extract a user-facing error message from an unknown error.
 * - If axios error with response.data.message (string) -> use it
 * - Else if response.data.error (string) -> use it
 * - Else if err is Error -> err.message
 * - Else fallback
 */
export function getApiErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err) && err.response?.data) {
    const data = err.response.data as Record<string, unknown>;
    if (typeof data.message === "string") return data.message;
    if (typeof data.error === "string") return data.error;
  }
  if (err instanceof Error) return err.message;
  return fallback;
}
