"use client";

import { useSearchParams } from "next/navigation";
import { ReportBuilderForm } from "../ReportBuilderForm";

export default function NewCustomerPhotoReportPage() {
  const searchParams = useSearchParams();
  const customerId = searchParams.get("customerId");

  return (
    <div className="max-w-4xl mx-auto pt-2">
      <h1 className="text-2xl font-bold text-slate-800 mb-6">New customer photo report</h1>
      <ReportBuilderForm initialCustomerId={customerId} />
    </div>
  );
}
