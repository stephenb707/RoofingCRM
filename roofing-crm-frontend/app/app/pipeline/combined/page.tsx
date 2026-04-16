"use client";

import Link from "next/link";
import PipelineViewSwitcher from "@/components/pipeline/PipelineViewSwitcher";
import LeadsPipelineExperience from "@/components/pipeline/LeadsPipelineExperience";
import JobsPipelineExperience from "@/components/pipeline/JobsPipelineExperience";

export default function CombinedPipelinePage() {
  return (
    <div className="max-w-7xl mx-auto space-y-10" data-testid="combined-pipeline-page">
      <div>
        <Link
          href="/app"
          className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Dashboard
        </Link>
        <h1 className="text-2xl font-bold text-slate-800">Pipeline workspace</h1>
        <p className="text-sm text-slate-500 mt-1 max-w-2xl">
          Lead and job boards in one place. Each section keeps its own stages and drag-and-drop.
        </p>
        <div className="mt-4">
          <PipelineViewSwitcher />
        </div>
      </div>

      <LeadsPipelineExperience variant="embedded" />
      <JobsPipelineExperience variant="embedded" customerIdFilter={null} />
    </div>
  );
}
