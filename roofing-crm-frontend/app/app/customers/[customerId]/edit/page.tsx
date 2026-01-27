"use client";

import { FormEvent, useState, useEffect } from "react";
import { useParams, useRouter } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuth } from "@/lib/AuthContext";
import { getCustomer, updateCustomer } from "@/lib/customersApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { queryKeys } from "@/lib/queryKeys";
import type { AddressDto } from "@/lib/types";

export default function EditCustomerPage() {
  const params = useParams();
  const router = useRouter();
  const customerId = params.customerId as string;
  const { api, auth } = useAuth();
  const queryClient = useQueryClient();

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [primaryPhone, setPrimaryPhone] = useState("");
  const [email, setEmail] = useState("");
  const [line1, setLine1] = useState("");
  const [line2, setLine2] = useState("");
  const [city, setCity] = useState("");
  const [state, setState] = useState("");
  const [zip, setZip] = useState("");
  const [notes, setNotes] = useState("");
  const [formError, setFormError] = useState<string | null>(null);

  const { data: customer, isLoading, isError, error } = useQuery({
    queryKey: queryKeys.customer(auth.selectedTenantId, customerId),
    queryFn: () => getCustomer(api, customerId),
    enabled: !!auth.selectedTenantId && !!customerId,
  });

  useEffect(() => {
    if (customer) {
      setFirstName(customer.firstName);
      setLastName(customer.lastName);
      setPrimaryPhone(customer.primaryPhone ?? "");
      setEmail(customer.email ?? "");
      const addr = customer.billingAddress;
      setLine1(addr?.line1 ?? "");
      setLine2(addr?.line2 ?? "");
      setCity(addr?.city ?? "");
      setState(addr?.state ?? "");
      setZip(addr?.zip ?? "");
      setNotes(customer.notes ?? "");
    }
  }, [customer]);

  const mutation = useMutation({
    mutationFn: (payload: {
      firstName: string;
      lastName: string;
      primaryPhone: string;
      email?: string | null;
      billingAddress?: AddressDto | null;
      notes?: string | null;
    }) => updateCustomer(api, customerId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.customer(auth.selectedTenantId, customerId) });
      queryClient.invalidateQueries({ queryKey: ["customers", auth.selectedTenantId] });
      router.push(`/app/customers/${customerId}`);
    },
    onError: (err: unknown) => {
      console.error("Failed to update customer:", err);
      setFormError(getApiErrorMessage(err, "Failed to update customer. Please try again."));
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setFormError(null);
    
    if (!firstName.trim() || !lastName.trim() || !primaryPhone.trim()) {
      setFormError("First name, last name, and primary phone are required.");
      return;
    }

    const billingAddress: AddressDto | null = line1.trim()
      ? {
          line1: line1.trim(),
          line2: line2.trim() || undefined,
          city: city.trim() || undefined,
          state: state.trim() || undefined,
          zip: zip.trim() || undefined,
        }
      : null;

    mutation.mutate({
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      primaryPhone: primaryPhone.trim(),
      email: email.trim() || null,
      billingAddress,
      notes: notes.trim() || null,
    });
  };

  if (isLoading) {
    return (
      <div className="max-w-2xl mx-auto">
        <div className="bg-white rounded-xl border border-slate-200 p-12 text-center">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-sky-600 mx-auto mb-4" />
          <p className="text-sm text-slate-500">Loading customer…</p>
        </div>
      </div>
    );
  }

  if (isError || !customer) {
    return (
      <div className="max-w-2xl mx-auto">
        <Link href={`/app/customers/${customerId}`} className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-4">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Customer
        </Link>
        <div className="bg-red-50 border border-red-200 rounded-xl p-6">
          <h3 className="text-sm font-medium text-red-800">Failed to load customer</h3>
          <p className="text-sm text-red-600 mt-1">{getApiErrorMessage(error, "The customer could not be found.")}</p>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-2xl mx-auto">
      <div className="mb-6">
        <Link href={`/app/customers/${customerId}`} className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Customer
        </Link>
        <h1 className="text-2xl font-bold text-slate-800">Edit Customer</h1>
        <p className="text-sm text-slate-500 mt-1">Update customer details</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {formError && (
          <div className="text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-4 py-3">{formError}</div>
        )}

        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Customer details</h2>
          <div className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">First name <span className="text-red-500">*</span></label>
                <input
                  type="text"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  required
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Last name <span className="text-red-500">*</span></label>
                <input
                  type="text"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  required
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                />
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Primary phone <span className="text-red-500">*</span></label>
              <input
                type="tel"
                value={primaryPhone}
                onChange={(e) => setPrimaryPhone(e.target.value)}
                required
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
              />
            </div>
            <div>
              <label htmlFor="notes" className="block text-sm font-medium text-slate-700 mb-1.5">Notes</label>
              <textarea
                id="notes"
                name="notes"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={4}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
              />
            </div>
          </div>
        </div>

        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Billing address</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Address line 1</label>
              <input
                type="text"
                value={line1}
                onChange={(e) => setLine1(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Address line 2</label>
              <input
                type="text"
                value={line2}
                onChange={(e) => setLine2(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
              />
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">City</label>
                <input
                  type="text"
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">State</label>
                <input
                  type="text"
                  value={state}
                  onChange={(e) => setState(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">ZIP</label>
                <input
                  type="text"
                  value={zip}
                  onChange={(e) => setZip(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                />
              </div>
            </div>
          </div>
        </div>

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={mutation.isPending}
            className="px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg disabled:opacity-60"
          >
            {mutation.isPending ? "Saving…" : "Save Changes"}
          </button>
          <Link
            href={`/app/customers/${customerId}`}
            className="px-4 py-2.5 border border-slate-300 rounded-lg text-sm font-medium text-slate-700 hover:bg-slate-50"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
