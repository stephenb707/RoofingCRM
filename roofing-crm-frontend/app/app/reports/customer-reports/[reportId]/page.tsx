"use client";

import { useParams } from "next/navigation";
import { ReportBuilderForm } from "../ReportBuilderForm";

export default function EditCustomerPhotoReportPage() {
  const params = useParams();
  const reportId = typeof params.reportId === "string" ? params.reportId : "";

  return (
    <div className="max-w-4xl mx-auto pt-2">
      <h1 className="text-2xl font-bold text-slate-800 mb-6">Edit photo report</h1>
      {reportId ? <ReportBuilderForm reportId={reportId} /> : null}
    </div>
  );
}
