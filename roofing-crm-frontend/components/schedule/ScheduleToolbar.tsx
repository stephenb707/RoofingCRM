import { format } from "date-fns";
import { parseLocalDateOnly } from "@/lib/format";

type ScheduleToolbarProps = {
  viewMonth: Date;
  onPrevMonth: () => void;
  onNextMonth: () => void;
  onThisMonth: () => void;
  rangeFrom: string;
  rangeTo: string;
};

export function ScheduleToolbar({
  viewMonth,
  onPrevMonth,
  onNextMonth,
  onThisMonth,
  rangeFrom,
  rangeTo,
}: ScheduleToolbarProps) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-3 mb-4">
      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={onPrevMonth}
          className="px-3 py-2 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50"
        >
          ← Prev
        </button>
        <h2 className="text-lg font-semibold text-slate-800 min-w-[10rem] text-center">
          {format(viewMonth, "MMMM yyyy")}
        </h2>
        <button
          type="button"
          onClick={onNextMonth}
          className="px-3 py-2 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50"
        >
          Next →
        </button>
        <button
          type="button"
          onClick={onThisMonth}
          className="px-3 py-2 text-sm font-medium text-sky-700 border border-sky-200 bg-sky-50 rounded-lg hover:bg-sky-100"
        >
          This month
        </button>
      </div>
      <div className="flex items-center gap-2 text-sm text-slate-600">
        <span className="hidden sm:inline">Viewing</span>
        <span className="font-medium text-slate-800">
          {format(parseLocalDateOnly(rangeFrom), "MMM d")} –{" "}
          {format(parseLocalDateOnly(rangeTo), "MMM d, yyyy")}
        </span>
      </div>
    </div>
  );
}
