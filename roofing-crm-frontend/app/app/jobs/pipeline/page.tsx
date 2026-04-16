"use client";

import { useSearchParams } from "next/navigation";
import JobsPipelineExperience from "@/components/pipeline/JobsPipelineExperience";

export default function JobsPipelinePage() {
  const searchParams = useSearchParams();
  const customerIdFromQuery = searchParams.get("customerId");
  return (
    <JobsPipelineExperience variant="page" customerIdFilter={customerIdFromQuery} />
  );
}
