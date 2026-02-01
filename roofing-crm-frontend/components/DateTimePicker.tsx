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
    setOpen(false);
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
          <DayPicker
            mode="single"
            selected={draftDate}
            onSelect={setDraftDate}
          />
          <div className="mt-4 pt-4 border-t border-slate-100">
            <label className="block text-xs font-medium text-slate-500 mb-1.5">
              Time
            </label>
            <input
              type="time"
              value={draftTime}
              onChange={(e) => setDraftTime(e.target.value)}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            />
          </div>
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
