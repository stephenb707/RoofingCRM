interface PaginationProps {
  page: number; // 0-based
  totalPages: number;
  onPrev: () => void;
  onNext: () => void;
  isFetching?: boolean;
}

/**
 * Pagination controls matching the style used on leads/jobs pages.
 */
export function Pagination({
  page,
  totalPages,
  onPrev,
  onNext,
  isFetching,
}: PaginationProps) {
  if (totalPages <= 1) return null;

  return (
    <div className="flex items-center justify-between mt-4">
      <p className="text-sm text-slate-500">
        {isFetching ? (
          <span className="text-slate-500">Loading…</span>
        ) : (
          <>
            Page {page + 1} of {totalPages}
          </>
        )}
      </p>
      <div className="flex gap-2">
        <button
          onClick={onPrev}
          disabled={page === 0}
          className="px-3 py-1.5 text-sm font-medium border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Previous
        </button>
        <button
          onClick={onNext}
          disabled={page >= totalPages - 1}
          className="px-3 py-1.5 text-sm font-medium border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Next
        </button>
      </div>
    </div>
  );
}
