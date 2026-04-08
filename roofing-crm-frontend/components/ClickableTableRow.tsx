"use client";

import { useRouter } from "next/navigation";
import type { KeyboardEvent, ReactNode } from "react";

const rowClassName =
  "hover:bg-slate-50 transition-colors cursor-pointer " +
  "focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-sky-500 focus-visible:ring-inset";

export type ClickableTableRowProps = {
  href: string;
  children: ReactNode;
  /** Announced when the row receives keyboard focus */
  "aria-label"?: string;
};

/** Table row navigation matching the Jobs list; use stopPropagation on td cells that contain nested links or controls. */
export function ClickableTableRow({
  href,
  children,
  "aria-label": ariaLabel,
}: ClickableTableRowProps) {
  const router = useRouter();

  const navigate = () => {
    router.push(href);
  };

  const onKeyDown = (e: KeyboardEvent<HTMLTableRowElement>) => {
    if (e.key === "Enter" || e.key === " ") {
      e.preventDefault();
      navigate();
    }
  };

  return (
    <tr
      tabIndex={0}
      className={rowClassName}
      onClick={navigate}
      onKeyDown={onKeyDown}
      aria-label={ariaLabel}
    >
      {children}
    </tr>
  );
}
