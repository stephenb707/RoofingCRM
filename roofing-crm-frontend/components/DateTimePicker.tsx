"use client";

import { useState, useRef, useEffect } from "react";
import { DayPicker } from "react-day-picker";
import { formatLocalDateInput, parseLocalDateOnly } from "@/lib/format";
import { usePopoverPlacement } from "@/lib/usePopoverPlacement";
import "react-day-picker/style.css";

export interface DateTimePickerProps {
  /** ISO or YYYY-MM-DDTHH:mm string, or empty */
  value: string;
  /** Called with local datetime string YYYY-MM-DDTHH:mm (for ISO conversion) or empty */
  onChange: (value: string) => void;
  placeholder?: string;
  id?: string;
  className?: string;
}

function parseValue(v: string): { date: Date; time: string } | null {
  if (!v || !v.trim()) return null;
  const d = new Date(v);
  if (Number.isNaN(d.getTime())) return null;
  const date = new Date(d.getFullYear(), d.getMonth(), d.getDate());
  const hours = String(d.getHours()).padStart(2, "0");
  const minutes = String(d.getMinutes()).padStart(2, "0");
  return { date, time: `${hours}:${minutes}` };
}

function toDatetimeLocal(date: Date, time: string): string {
  const [h, m] = time.split(":").map(Number);
  const y = date.getFullYear();
  const mon = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const hour = String(h || 0).padStart(2, "0");
  const min = String(m || 0).padStart(2, "0");
  return `${y}-${mon}-${day}T${hour}:${min}`;
}

export function DateTimePicker({
  value,
  onChange,
  placeholder = "Select date and time…",
  id,
  className = "",
}: DateTimePickerProps) {
  const [open, setOpen] = useState(false);
  const parsed = parseValue(value);
  const [draftDate, setDraftDate] = useState<Date | undefined>(
    parsed?.date ?? undefined
  );
  const [draftTime, setDraftTime] = useState(parsed?.time ?? "09:00");
  const containerRef = useRef<HTMLDivElement>(null);
  const { buttonRef, popoverRef, style } = usePopoverPlacement(open, {
    maxWidthPx: 320,
    fallbackHeightPx: 420,
    collisionPaddingPx: 24,
  });

  useEffect(() => {
    const p = parseValue(value);
    if (p) {
      setDraftDate(p.date);
      setDraftTime(p.time);
    } else {
      setDraftDate(undefined);
      setDraftTime("09:00");
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
        const d = new Date(value);
        if (Number.isNaN(d.getTime())) return placeholder;
        return d.toLocaleString("en-US", {
          weekday: "short",
          month: "short",
          day: "numeric",
          year: "numeric",
          hour: "numeric",
          minute: "2-digit",
        });
      })()
    : placeholder;

  const handleConfirm = () => {
    if (draftDate) {
      onChange(toDatetimeLocal(draftDate, draftTime));
    } else {
      onChange("");
    }
    setOpen(false);
  };

  const handleClear = () => {
    setDraftDate(undefined);
    setDraftTime("09:00");
    onChange("");
  };

  const handleCancel = () => {
    const p = parseValue(value);
    if (p) {
      setDraftDate(p.date);
      setDraftTime(p.time);
    } else {
      setDraftDate(undefined);
      setDraftTime("09:00");
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
            selected={draftDate}
            onSelect={setDraftDate}
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
          <div className="mt-2 pt-2 border-t border-slate-100">
            <label className="block text-xs font-medium text-slate-500 mb-1">
              Time
            </label>
            <input
              type="time"
              value={draftTime}
              onChange={(e) => setDraftTime(e.target.value)}
              className="w-full border border-slate-300 rounded-lg px-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            />
          </div>
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
