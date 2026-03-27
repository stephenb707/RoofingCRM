import React, { useState } from "react";
import { render, screen, fireEvent } from "./test-utils";
import { DatePicker } from "@/components/DatePicker";
import { DateTimePicker } from "@/components/DateTimePicker";
import { DateRangePicker } from "@/components/DateRangePicker";

function hasSelectedHighlight(): boolean {
  const dayCells = Array.from(document.querySelectorAll("[data-day]"));
  return dayCells.some((el) =>
    (el.className || "").toString().includes("bg-sky-700")
  );
}

function localDateString(day: number): string {
  const now = new Date();
  const y = now.getFullYear();
  const m = String(now.getMonth() + 1).padStart(2, "0");
  const d = String(day).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

describe("Date picker clear behavior", () => {
  it("DatePicker Clear clears value + highlight and keeps popover open; Cancel closes", () => {
    const initial = localDateString(15);
    function Harness() {
      const [value, setValue] = useState(initial);
      return <DatePicker value={value} onChange={setValue} />;
    }

    render(<Harness />);

    fireEvent.click(screen.getByRole("button"));
    expect(hasSelectedHighlight()).toBe(true);

    fireEvent.click(screen.getByRole("button", { name: /clear/i }));
    expect(screen.getByRole("button", { name: /cancel/i })).toBeInTheDocument();
    expect(hasSelectedHighlight()).toBe(false);

    fireEvent.click(screen.getByRole("button", { name: /cancel/i }));
    expect(screen.queryByRole("button", { name: /cancel/i })).not.toBeInTheDocument();
  });

  it("DateTimePicker Clear keeps popover open and clears value", () => {
    const initial = `${localDateString(15)}T10:30`;
    function Harness() {
      const [value, setValue] = useState(initial);
      return <DateTimePicker value={value} onChange={setValue} />;
    }

    render(<Harness />);
    fireEvent.click(screen.getByRole("button"));
    fireEvent.click(screen.getByRole("button", { name: /clear/i }));

    expect(screen.getByRole("button", { name: /cancel/i })).toBeInTheDocument();
    expect(hasSelectedHighlight()).toBe(false);
  });

  it("DateRangePicker Clear clears range highlight and keeps popover open", () => {
    const start = localDateString(10);
    const end = localDateString(12);
    function Harness() {
      const [from, setFrom] = useState(start);
      const [to, setTo] = useState(end);
      return (
        <DateRangePicker
          startDate={from}
          endDate={to}
          onChange={(nextFrom, nextTo) => {
            setFrom(nextFrom);
            setTo(nextTo);
          }}
        />
      );
    }

    render(<Harness />);
    fireEvent.click(screen.getByRole("button"));
    expect(hasSelectedHighlight()).toBe(true);

    fireEvent.click(screen.getByRole("button", { name: /clear/i }));
    expect(screen.getByRole("button", { name: /cancel/i })).toBeInTheDocument();
    expect(hasSelectedHighlight()).toBe(false);
  });
});
