import React from "react";
import { render, screen, fireEvent } from "./test-utils";
import { DatePicker } from "@/components/DatePicker";
import { DateRangePicker } from "@/components/DateRangePicker";

function localDateString(day: number): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(day).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

describe("DayPicker style overrides", () => {
  it("single-date picker renders a dark selected day", () => {
    const selectedDate = localDateString(15);
    render(<DatePicker value={selectedDate} onChange={jest.fn()} />);

    fireEvent.click(screen.getByRole("button"));

    const selectedCell = document.querySelector(`[data-day="${selectedDate}"]`);
    const selected = (selectedCell?.className || "")
      .toString()
      .includes("bg-sky-700");
    expect(selected).toBe(true);
  });

  it("range picker renders dark endpoints and darker middle range", () => {
    const startDate = localDateString(10);
    const middleDate = localDateString(11);
    const endDate = localDateString(12);

    render(
      <DateRangePicker
        startDate={startDate}
        endDate={endDate}
        onChange={jest.fn()}
      />
    );

    fireEvent.click(screen.getByRole("button"));

    const startCell = document.querySelector(`[data-day="${startDate}"]`);
    const endCell = document.querySelector(`[data-day="${endDate}"]`);
    const middleCell = document.querySelector(`[data-day="${middleDate}"]`);

    const middleHighlighted = (middleCell?.className || "")
      .toString()
      .includes("bg-sky-600/80");

    expect((startCell?.className || "").toString()).toContain("bg-sky-700");
    expect((endCell?.className || "").toString()).toContain("bg-sky-700");
    expect(middleHighlighted).toBe(true);
  });
});
