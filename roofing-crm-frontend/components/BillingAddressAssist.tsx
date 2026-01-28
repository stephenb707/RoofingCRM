"use client";

import { formatAddress } from "@/lib/format";
import type { AddressDto } from "@/lib/types";

export interface BillingAddressAssistProps {
  billingAddress: AddressDto | null | undefined;
  onUseBillingAddress: () => void;
  onClearAddress: () => void;
  disabled?: boolean;
  className?: string;
}

export function BillingAddressAssist({
  billingAddress,
  onUseBillingAddress,
  onClearAddress,
  disabled = false,
  className = "",
}: BillingAddressAssistProps) {
  if (billingAddress == null || (typeof billingAddress === "object" && !billingAddress.line1 && !billingAddress.city)) {
    return null;
  }

  const formatted = formatAddress(billingAddress);
  if (!formatted || formatted === "—") return null;

  return (
    <div
      className={`rounded-lg border border-slate-200 bg-slate-50/50 p-4 ${className}`}
      data-testid="billing-address-assist"
    >
      <h3 className="text-sm font-medium text-slate-700 mb-2">Billing address on file</h3>
      <p className="text-sm text-slate-600 mb-3">{formatted}</p>
      <div className="flex flex-wrap gap-2">
        <button
          type="button"
          onClick={onUseBillingAddress}
          disabled={disabled}
          className="px-3 py-1.5 text-sm font-medium rounded-lg bg-sky-600 hover:bg-sky-700 text-white disabled:opacity-50 transition-colors"
        >
          Use billing address
        </button>
        <button
          type="button"
          onClick={onClearAddress}
          disabled={disabled}
          className="px-3 py-1.5 text-sm font-medium rounded-lg border border-slate-300 text-slate-700 hover:bg-slate-50 disabled:opacity-50 transition-colors"
        >
          Clear address
        </button>
      </div>
    </div>
  );
}
