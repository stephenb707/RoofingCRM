import { formatPhone, formatFileSize } from "./format";

describe("formatFileSize", () => {
  it("formats bytes as B, KB, MB, GB", () => {
    expect(formatFileSize(0)).toBe("0 B");
    expect(formatFileSize(500)).toBe("500 B");
    expect(formatFileSize(1024)).toBe("1.0 KB");
    expect(formatFileSize(1536)).toBe("1.5 KB");
    expect(formatFileSize(1024 * 1024)).toBe("1.0 MB");
    expect(formatFileSize(2.5 * 1024 * 1024)).toBe("2.5 MB");
    expect(formatFileSize(1024 * 1024 * 1024)).toBe("1.0 GB");
    expect(formatFileSize(1.5 * 1024 * 1024 * 1024)).toBe("1.5 GB");
  });

  it("returns — for null/undefined/NaN", () => {
    expect(formatFileSize(null)).toBe("—");
    expect(formatFileSize(undefined)).toBe("—");
    expect(formatFileSize(Number.NaN)).toBe("—");
  });
});

describe("formatPhone", () => {
  it("formats 10-digit US phone number", () => {
    expect(formatPhone("3125551212")).toBe("(312) 555-1212");
    expect(formatPhone("555-123-4567")).toBe("(555) 123-4567");
    expect(formatPhone("(555) 123-4567")).toBe("(555) 123-4567");
  });

  it("formats 11-digit number starting with 1", () => {
    expect(formatPhone("13125551212")).toBe("+1 (312) 555-1212");
    expect(formatPhone("1-312-555-1212")).toBe("+1 (312) 555-1212");
    expect(formatPhone("+1 312 555 1212")).toBe("+1 (312) 555-1212");
  });

  it("returns original string for non-standard formats", () => {
    expect(formatPhone("+44 20 7946 0958")).toBe("+44 20 7946 0958");
    expect(formatPhone("12345")).toBe("12345");
    expect(formatPhone("555-123")).toBe("555-123");
  });

  it("returns — for null/undefined/empty", () => {
    expect(formatPhone(null)).toBe("—");
    expect(formatPhone(undefined)).toBe("—");
    expect(formatPhone("")).toBe("—");
  });
});
