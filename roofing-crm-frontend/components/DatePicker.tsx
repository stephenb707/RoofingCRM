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
  className?: string;
}

export function DatePicker({
  value,
  onChange,
  placeholder = "Select date…",
  id,
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
    setOpen(false);
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
        onClick={() => setOpen((o) => !o)}
        className={`w-full border border-slate-300 rounded-lg px-4 py-2.5 text-left text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent bg-white ${className}`}
      >
        {displayText}
      </button>

      {open && (
        <div
          ref={popoverRef}
          className="max-w-[min(95vw,320px)] bg-white rounded-xl border border-slate-200 shadow-lg p-4 overflow-auto"
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
          <DayPicker mode="single" selected={draft} onSelect={setDraft} />
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
