"use client";

import type { ReactNode } from "react";
import { useId } from "react";
import { DatePicker } from "@/components/DatePicker";

export interface DatePickerFieldProps {
  label: ReactNode;
  value: string | null | undefined;
  onChange: (next: string) => void;
  placeholder?: string;
  id?: string;
  className?: string;
}

export function DatePickerField({
  label,
  value,
  onChange,
  placeholder = "Select date…",
  id,
  className = "",
}: DatePickerFieldProps) {
  const generatedId = useId();
  const fieldId = id ?? `date-field-${generatedId}`;
  const labelId = `${fieldId}-label`;
  const normalizedValue = value ?? "";

  return (
    <div className={className}>
      <label id={labelId} htmlFor={fieldId} className="block text-sm font-medium text-slate-700 mb-1">
        {label}
      </label>
      <input
        id={fieldId}
        type="text"
        value={normalizedValue}
        onChange={(e) => onChange(e.target.value)}
        className="sr-only"
        tabIndex={-1}
        aria-hidden="true"
      />
      <DatePicker
        id={`${fieldId}-trigger`}
        ariaLabelledBy={labelId}
        value={normalizedValue}
        onChange={onChange}
        placeholder={placeholder}
      />
    </div>
  );
}
