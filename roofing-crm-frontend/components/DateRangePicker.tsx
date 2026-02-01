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
    setOpen(false);
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
        className={`w-full border border-slate-300 rounded-lg px-4 py-2.5 text-left text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent bg-white ${className}`}
      >
        <div>{displayText}</div>
        {summaryLine && (
          <div className="text-xs text-slate-500 mt-0.5">{summaryLine}</div>
        )}
      </button>

      {open && (
        <div
          ref={popoverRef}
          className="max-w-[min(95vw,640px)] bg-white rounded-xl border border-slate-200 shadow-lg p-4 overflow-auto"
          style={
            style
              ? {
                  position: style.position,
                  top: style.top,
                  left: style.left,
                  maxHeight: style.maxHeight,
                  zIndex: style.zIndex,
                }
              : { visibility: "hidden" as const }
          }
        >
          <DayPicker
            mode="range"
            selected={draft}
            onSelect={setDraft}
            numberOfMonths={2}
            classNames={{
              months: "flex flex-row gap-6",
              month: "flex flex-col gap-4",
            }}
          />
          <div className="flex items-center justify-between gap-2 mt-4 pt-4 border-t border-slate-100">
            <button
              type="button"
              onClick={handleClear}
              className="text-sm text-slate-600 hover:text-slate-800"
            >
              Clear
            </button>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={handleCancel}
                className="px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-100 rounded-lg"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirm}
                className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg"
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
