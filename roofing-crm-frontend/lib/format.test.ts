import { formatPhone } from "./format";

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
