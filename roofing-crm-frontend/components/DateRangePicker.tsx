"use client";

import { useState, useRef, useEffect } from "react";
import { DayPicker } from "react-day-picker";
import type { DateRange } from "react-day-picker";
import { formatLocalDateInput, formatDateShortWeekday, parseLocalDateOnly } from "@/lib/format";
import { usePopoverPlacement } from "@/lib/usePopoverPlacement";
import "react-day-picker/style.css";

export interface DateRangePickerProps {
  /** Start date YYYY-MM-DD or empty */
  startDate: string;
  /** End date YYYY-MM-DD or empty */
  endDate: string;
  /** Called with (startDate, endDate) when user confirms */
  onChange: (startDate: string, endDate: string) => void;
  placeholder?: string;
  id?: string;
  className?: string;
}

export function DateRangePicker({
  startDate,
  endDate,
  onChange,
  placeholder = "Select date range…",
  id,
  className = "",
}: DateRangePickerProps) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<DateRange | undefined>(() => {
    if (startDate) {
      const from = parseLocalDateOnly(startDate);
      const to = endDate ? parseLocalDateOnly(endDate) : undefined;
      return { from, to };
    }
    return undefined;
  });
  const containerRef = useRef<HTMLDivElement>(null);
  const { buttonRef, popoverRef, style } = usePopoverPlacement(open, {
    maxWidthPx: 640,
    fallbackHeightPx: 420,
    collisionPaddingPx: 24,
  });

  useEffect(() => {
    if (startDate) {
      const from = parseLocalDateOnly(startDate);
      const to = endDate ? parseLocalDateOnly(endDate) : undefined;
      setDraft({ from, to });
    } else {
      setDraft(undefined);
    }
  }, [startDate, endDate, open]);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    if (open) {
      document.addEventListener("mousedown", handleClickOutside);
      return () => document.removeEventListener("mousedown", handleClickOutside);
    }
  }, [open]);

  const startStr = startDate ? formatDateShortWeekday(startDate) : "";
  const endStr = endDate ? formatDateShortWeekday(endDate) : "";
  const dayCount =
    startDate && endDate
      ? Math.floor(
          (parseLocalDateOnly(endDate).getTime() - parseLocalDateOnly(startDate).getTime()) / (24 * 60 * 60 * 1000)
        ) + 1
      : 0;

  const displayText =
    startDate && endDate
      ? `${formatLocalDateInput(parseLocalDateOnly(startDate))} – ${formatLocalDateInput(parseLocalDateOnly(endDate))}`
      : startDate
        ? formatLocalDateInput(parseLocalDateOnly(startDate))
        : placeholder;

  const summaryLine =
    startDate && endDate && dayCount > 0
      ? `${dayCount} day${dayCount !== 1 ? "s" : ""} • ${startStr} → ${endStr}`
      : startDate
        ? startStr
        : null;

  const handleConfirm = () => {
    if (draft?.from) {
      const start = formatLocalDateInput(draft.from);
      const end = draft.to ? formatLocalDateInput(draft.to) : start;
      onChange(start, end);
    } else {
      onChange("", "");
    }
    setOpen(false);
  };

  const handleClear = () => {
    setDraft(undefined);
    onChange("", "");
  };

  const handleCancel = () => {
    if (startDate) {
      const from = parseLocalDateOnly(startDate);
      const to = endDate ? parseLocalDateOnly(endDate) : undefined;
      setDraft({ from, to });
    } else {
      setDraft(undefined);
    }
    setOpen(false);
  };

  return (
    <div ref={containerRef} className="relative">
      <button
        ref={buttonRef}
        type="button"
        id={id}
        onClick={() => setOpen((o) => !o)}
        className={`w-full border border-slate-300 rounded-lg px-3 py-2 text-left text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent bg-white ${className}`}
      >
        <div>{displayText}</div>
        {summaryLine && (
          <div className="text-xs text-slate-500 mt-0.5">{summaryLine}</div>
        )}
      </button>

      {open && (
        <div
          ref={popoverRef}
          className="w-auto max-w-[min(95vw,640px)] bg-white rounded-xl border border-slate-200 shadow-lg p-2 overflow-visible"
          style={
            style
              ? {
                  position: style.position,
                  top: style.top,
                  left: style.left,
                  zIndex: style.zIndex,
                }
              : { visibility: "hidden" as const }
          }
        >
          <DayPicker
            mode="range"
            selected={draft}
            onSelect={setDraft}
            numberOfMonths={1}
            showOutsideDays
            className="text-sm"
            classNames={{
              month: "space-y-2",
              caption: "flex items-center justify-between px-1",
              caption_label: "text-sm font-medium",
              nav: "flex items-center gap-1",
              nav_button: "h-7 w-7 rounded-md border border-slate-200 hover:bg-slate-50",
              table: "w-full border-collapse",
              head_row: "flex",
              head_cell: "w-8 text-[11px] font-medium text-slate-500",
              row: "flex w-full mt-1",
              cell: "relative w-8 h-8 p-0 text-center",
              day: "h-8 w-8 rounded-md p-0 text-sm hover:bg-slate-100",
              selected: "!bg-sky-700 !text-white",
              day_selected: "!bg-sky-700 !text-white",
              range_start: "!bg-sky-700 !text-white hover:!bg-sky-700 rounded-md",
              range_end: "!bg-sky-700 !text-white hover:!bg-sky-700 rounded-md",
              range_middle: "!bg-sky-600/80 !text-white hover:!bg-sky-600/80",
              day_range_start: "!bg-sky-700 !text-white hover:!bg-sky-700 rounded-md",
              day_range_end: "!bg-sky-700 !text-white hover:!bg-sky-700 rounded-md",
              day_range_middle: "!bg-sky-600/80 !text-white hover:!bg-sky-600/80",
              today: "font-semibold text-sky-700",
              day_today: "font-semibold text-sky-700",
              outside: "text-slate-300",
              day_outside: "text-slate-300",
            }}
          />
          <div className="flex items-center justify-between gap-2 mt-2 pt-2 border-t border-slate-100">
            <button
              type="button"
              onClick={handleClear}
              className="text-xs text-slate-600 hover:text-slate-800"
            >
              Clear
            </button>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={handleCancel}
                className="px-3 py-1.5 text-xs font-medium text-slate-700 hover:bg-slate-100 rounded-lg"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirm}
                className="px-3 py-1.5 bg-sky-600 hover:bg-sky-700 text-white text-xs font-medium rounded-lg"
              >
                Confirm
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
