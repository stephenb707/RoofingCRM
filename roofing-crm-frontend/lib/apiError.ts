import axios from "axios";

/**
 * Extract a user-facing error message from an unknown error.
 * - If axios error with response.data.message (string) -> use it
 * - Else if response.data.error (string) -> use it
 * - Else if err is Error -> err.message
 * - Else fallback
 */
export function getApiErrorMessage(err: unknown, fallback: string): string {
  if (axios.isAxiosError(err)) {
    if (err.response?.status === 401) {
      return "Your session expired. Please sign in again.";
    }
    if (err.response?.status === 413) {
      return "File is too large. Maximum upload size is 20MB.";
    }
  }

  if (axios.isAxiosError(err) && err.response?.data) {
    const data = err.response.data as Record<string, unknown>;
    if (typeof data.message === "string") {
      if (data.message.toLowerCase().includes("maximum upload size")) {
        return "File is too large. Maximum upload size is 20MB.";
      }
      return data.message;
    }
    if (typeof data.error === "string") return data.error;
  }
  if (err instanceof Error) return err.message;
  return fallback;
}
