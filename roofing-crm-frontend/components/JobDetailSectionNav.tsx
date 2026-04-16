"use client";

export type DetailSectionNavIcon =
  | "customer"
  | "overview"
  | "activity"
  | "tasks"
  | "invoices"
  | "accounting"
  | "attachments"
  | "communication"
  | "jobs"
  | "leads"
  | "location"
  | "notes";

export interface DetailSectionNavItem {
  id: string;
  label: string;
  icon: DetailSectionNavIcon;
}

export interface DetailSectionNavProps {
  items: DetailSectionNavItem[];
  activeSectionId?: string | null;
  onNavigate?: (id: string) => void;
  className?: string;
  variant?: "rail" | "inline";
}

export function DetailSectionNav({
  items,
  activeSectionId = null,
  onNavigate,
  className = "",
  variant = "rail",
}: DetailSectionNavProps) {
  if (items.length === 0) {
    return null;
  }

  const containerClassName =
    variant === "rail"
      ? "rounded-2xl border border-slate-200 bg-white/95 p-3 backdrop-blur-sm"
      : "rounded-xl border border-slate-200 bg-white p-4";

  const navClassName = variant === "rail" ? "mt-3 space-y-1.5" : "mt-3 flex flex-wrap gap-2";

  return (
    <div className={`${containerClassName} ${className}`.trim()} data-testid={`detail-section-nav-${variant}`}>
      <p className="px-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">On This Page</p>
      <nav aria-label="Job detail sections" className={navClassName}>
        {items.map((item) => {
          const isActive = item.id === activeSectionId;

          return (
            <a
              key={item.id}
              href={`#${item.id}`}
              aria-current={isActive ? "location" : undefined}
              onClick={(event) => {
                if (!onNavigate) {
                  return;
                }
                event.preventDefault();
                onNavigate(item.id);
              }}
              className={
                variant === "rail"
                  ? `group flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors ${
                      isActive
                        ? "bg-sky-50 text-sky-700"
                        : "text-slate-600 hover:bg-slate-50 hover:text-sky-700"
                    }`
                  : `inline-flex items-center gap-2 rounded-full border px-3 py-1.5 text-sm font-medium transition-colors ${
                      isActive
                        ? "border-sky-200 bg-sky-50 text-sky-700"
                        : "border-slate-200 bg-white text-slate-600 hover:border-sky-200 hover:bg-sky-50 hover:text-sky-700"
                    }`
              }
            >
              <SectionNavIcon icon={item.icon} active={isActive} />
              <span>{item.label}</span>
            </a>
          );
        })}
      </nav>
    </div>
  );
}

function SectionNavIcon({ icon, active }: { icon: DetailSectionNavIcon; active: boolean }) {
  const className = `h-4 w-4 shrink-0 ${active ? "text-sky-600" : "text-slate-400 group-hover:text-sky-600"}`;

  switch (icon) {
    case "customer":
      return (
        <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM5 21a7 7 0 0114 0"
          />
        </svg>
      );
    case "overview":
      return (
        <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 17v-6m3 6V7m3 10v-3m3 7H6a2 2 0 01-2-2V5a2 2 0 012-2h12a2 2 0 012 2v14a2 2 0 01-2 2z"
          />
        </svg>
      );
    case "activity":
      return (
        <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M3 12h4l3-8 4 16 3-8h4"
          />
        </svg>
      );
    case "tasks":
      return (
        <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 5H7a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"
          />
        </svg>
      );
    case "invoices":
      return (
        <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 14h6m-6 4h6M7 4h10a2 2 0 012 2v12l-2-1-2 1-2-1-2 1-2-1-2 1V6a2 2 0 012-2z"
          />
        </svg>
      );
    case "accounting":
      return (
        <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M12 8c-2.21 0-4 .895-4 2s1.79 2 4 2 4 .895 4 2-1.79 2-4 2m0-10V5m0 14v-1m8-6a8 8 0 11-16 0 8 8 0 0116 0z"
          />
        </svg>
      );
    case "attachments":
      return (
        <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15.172 7l-6.586 6.586a2 2 0 102.828 2.828L18 9.828a4 4 0 10-5.657-5.657L5.757 10.757a6 6 0 108.486 8.486L20 13.485"
          />
        </svg>
      );
    case "communication":
      return (
        <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M8 10h8m-8 4h5m-7 6l-4 1 1-4V7a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2H8z"
          />
        </svg>
      );
    case "jobs":
      return (
        <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 6V4a1 1 0 011-1h4a1 1 0 011 1v2m-9 4h12m-13 9h14a1 1 0 001-1V8a1 1 0 00-1-1H5a1 1 0 00-1 1v10a1 1 0 001 1z"
          />
        </svg>
      );
    case "leads":
      return (
        <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M17 20h5V18a4 4 0 00-5-3.874M17 20H7m10 0v-2c0-.653-.126-1.277-.356-1.848M7 20H2V18a4 4 0 015-3.874M7 20v-2c0-.653.126-1.277.356-1.848m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"
          />
        </svg>
      );
    case "location":
      return (
        <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M17.657 16.657L13.414 20.9a1.998 1.998 0 01-2.827 0l-4.244-4.243a8 8 0 1111.314 0z"
          />
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M15 11a3 3 0 11-6 0 3 3 0 016 0z"
          />
        </svg>
      );
    case "notes":
      return (
        <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
          />
        </svg>
      );
    default:
      return null;
  }
}
