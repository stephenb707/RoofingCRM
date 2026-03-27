"use client";

import { useState, useRef, useEffect } from "react";
import { DayPicker } from "react-day-picker";
import { formatLocalDateInput, parseLocalDateOnly } from "@/lib/format";
import { usePopoverPlacement } from "@/lib/usePopoverPlacement";
import "react-day-picker/style.css";

export interface DatePickerProps {
  /** Date YYYY-MM-DD or empty */
  value: string;
  /** Called with YYYY-MM-DD when user confirms */
  onChange: (value: string) => void;
  placeholder?: string;
  id?: string;
  ariaLabelledBy?: string;
  className?: string;
}

export function DatePicker({
  value,
  onChange,
  placeholder = "Select date…",
  id,
  ariaLabelledBy,
  className = "",
}: DatePickerProps) {
  const [open, setOpen] = useState(false);
  const [draft, setDraft] = useState<Date | undefined>(() =>
    value ? parseLocalDateOnly(value) : undefined
  );
  const containerRef = useRef<HTMLDivElement>(null);
  const { buttonRef, popoverRef, style } = usePopoverPlacement(open, {
    maxWidthPx: 320,
    fallbackHeightPx: 420,
    collisionPaddingPx: 24,
  });

  useEffect(() => {
    if (value) {
      setDraft(parseLocalDateOnly(value));
    } else {
      setDraft(undefined);
    }
  }, [value, open]);

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

  const displayText = value
    ? (() => {
        const d = parseLocalDateOnly(value);
        return d.toLocaleDateString("en-US", {
          weekday: "short",
          month: "short",
          day: "numeric",
          year: "numeric",
        });
      })()
    : placeholder;

  const handleConfirm = () => {
    if (draft) {
      onChange(formatLocalDateInput(draft));
    } else {
      onChange("");
    }
    setOpen(false);
  };

  const handleClear = () => {
    setDraft(undefined);
    onChange("");
  };

  const handleCancel = () => {
    if (value) {
      setDraft(parseLocalDateOnly(value));
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
        aria-labelledby={ariaLabelledBy}
        onClick={() => setOpen((o) => !o)}
        className={`w-full border border-slate-300 rounded-lg px-3 py-2 text-left text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent bg-white ${className}`}
      >
        {displayText}
      </button>

      {open && (
        <div
          ref={popoverRef}
          className="w-auto max-w-[min(95vw,300px)] bg-white rounded-xl border border-slate-200 shadow-lg p-2 overflow-visible"
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
            mode="single"
            selected={draft}
            onSelect={setDraft}
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
              selected: "!bg-sky-700 !text-white hover:!bg-sky-700 focus:!bg-sky-700",
              day_selected: "!bg-sky-700 !text-white hover:!bg-sky-700 focus:!bg-sky-700",
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
