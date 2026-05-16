import type { PipelineStatusDefinitionDto } from "@/lib/pipelineStatusesApi";

type ScheduleFiltersProps = {
  jobPipelineDefs: PipelineStatusDefinitionDto[];
  statusFilter: string;
  onStatusFilterChange: (value: string) => void;
  crewFilter: string;
  onCrewFilterChange: (value: string) => void;
  includeUnscheduled: boolean;
  onIncludeUnscheduledChange: (value: boolean) => void;
  hasActiveFilters: boolean;
  onClearFilters: () => void;
};

export function ScheduleFilters({
  jobPipelineDefs,
  statusFilter,
  onStatusFilterChange,
  crewFilter,
  onCrewFilterChange,
  includeUnscheduled,
  onIncludeUnscheduledChange,
  hasActiveFilters,
  onClearFilters,
}: ScheduleFiltersProps) {
  return (
    <div className="bg-white rounded-xl border border-slate-200 p-4 mb-6">
      <div className="flex flex-wrap items-end gap-4">
        <div>
          <label
            htmlFor="schedule-status"
            className="block text-sm font-medium text-slate-700 mb-1"
          >
            Status
          </label>
          <select
            id="schedule-status"
            value={statusFilter}
            onChange={(e) => onStatusFilterChange(e.target.value)}
            className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          >
            <option value="">All</option>
            {jobPipelineDefs
              .filter((d) => d.active)
              .map((d) => (
                <option key={d.id} value={d.id}>
                  {d.label}
                </option>
              ))}
          </select>
        </div>
        <div>
          <label
            htmlFor="schedule-crew"
            className="block text-sm font-medium text-slate-700 mb-1"
          >
            Crew
          </label>
          <input
            id="schedule-crew"
            type="text"
            placeholder="Filter by crew"
            value={crewFilter}
            onChange={(e) => onCrewFilterChange(e.target.value)}
            className="border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>
        <div className="flex items-center gap-2">
          <input
            id="schedule-unscheduled"
            type="checkbox"
            checked={includeUnscheduled}
            onChange={(e) => onIncludeUnscheduledChange(e.target.checked)}
            className="rounded border-slate-300 text-sky-600 focus:ring-sky-500"
          />
          <label
            htmlFor="schedule-unscheduled"
            className="text-sm font-medium text-slate-700"
          >
            Include unscheduled
          </label>
        </div>
        {hasActiveFilters && (
          <button
            type="button"
            onClick={onClearFilters}
            className="text-sm text-slate-500 hover:text-slate-700 underline"
          >
            Clear filters
          </button>
        )}
      </div>
    </div>
  );
}
