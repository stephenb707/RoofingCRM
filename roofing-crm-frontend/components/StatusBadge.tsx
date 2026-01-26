interface StatusBadgeProps {
  label: string;
  className?: string;
}

/**
 * Small status badge. Pages pass className from constants (e.g. STATUS_COLORS).
 */
export function StatusBadge({ label, className = "" }: StatusBadgeProps) {
  return (
    <span
      className={`inline-flex px-2.5 py-1 text-xs font-medium rounded-full ${className}`}
    >
      {label}
    </span>
  );
}
