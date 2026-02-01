"use client";

import { FormEvent, useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { createLead } from "@/lib/leadsApi";
import { listCustomers, getCustomer } from "@/lib/customersApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { formatPhone } from "@/lib/format";
import { BillingAddressAssist } from "@/components/BillingAddressAssist";
import { Combobox } from "@/components/Combobox";
import { LEAD_SOURCES, SOURCE_LABELS } from "@/lib/leadsConstants";
import { PREFERRED_CONTACT_METHODS, PREFERRED_CONTACT_LABELS } from "@/lib/preferredContactConstants";
import { LeadSource, CreateLeadRequest, AddressDto, PreferredContactMethod } from "@/lib/types";

export default function NewLeadPage() {
  const router = useRouter();
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();

  // Customer selection: existing vs new
  const [customerId, setCustomerId] = useState("");
  const [customerSearch, setCustomerSearch] = useState("");
  const [debouncedCustomerSearch, setDebouncedCustomerSearch] = useState("");

  // Customer fields (used when no existing customer selected)
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");

  // Property address fields
  const [addressLine1, setAddressLine1] = useState("");
  const [addressLine2, setAddressLine2] = useState("");
  const [city, setCity] = useState("");
  const [state, setState] = useState("");
  const [zip, setZip] = useState("");

  // Lead fields
  const [source, setSource] = useState<LeadSource | "">("");
  const [notes, setNotes] = useState("");
  const [preferredContact, setPreferredContact] = useState<PreferredContactMethod | "">("");

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedCustomerSearch(customerSearch);
    }, 300);
    return () => clearTimeout(timer);
  }, [customerSearch]);

  const { data: customersData } = useQuery({
    queryKey: ["customers", auth.selectedTenantId, debouncedCustomerSearch || null, 0],
    queryFn: () => listCustomers(api, { page: 0, size: 100, q: debouncedCustomerSearch || null }),
    enabled: ready,
  });
  const customers = customersData?.content ?? [];

  const { data: selectedCustomer } = useQuery({
    queryKey: ["customer", auth.selectedTenantId, customerId],
    queryFn: () => getCustomer(api, customerId),
    enabled: ready && !!customerId,
  });

  const handleUseBillingAddress = () => {
    if (selectedCustomer?.billingAddress) {
      const a = selectedCustomer.billingAddress;
      setAddressLine1(a.line1 ?? "");
      setAddressLine2(a.line2 ?? "");
      setCity(a.city ?? "");
      setState(a.state ?? "");
      setZip(a.zip ?? "");
    }
  };

  const handleClearAddress = () => {
    setAddressLine1("");
    setAddressLine2("");
    setCity("");
    setState("");
    setZip("");
  };

  const mutation = useMutation({
    mutationFn: async (payload: CreateLeadRequest) => {
      return createLead(api, payload);
    },
    onSuccess: (data) => {
      // Invalidate leads query
      queryClient.invalidateQueries({
        queryKey: ["leads", auth.selectedTenantId],
      });
      // Navigate to the new lead detail page
      router.push(`/app/leads/${data.id}`);
    },
    onError: (err: unknown) => {
      console.error("Failed to create lead:", err);
      setError(getApiErrorMessage(err, "Failed to create lead. Please try again."));
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!addressLine1.trim()) {
      setError("Property address is required.");
      return;
    }

    const propertyAddress: AddressDto = {
      line1: addressLine1.trim(),
      line2: addressLine2.trim() || null,
      city: city.trim() || null,
      state: state.trim() || null,
      zip: zip.trim() || null,
      countryCode: "US",
    };

    if (customerId) {
      mutation.mutate({
        customerId,
        newCustomer: null,
        source: source || null,
        leadNotes: notes.trim() || null,
        propertyAddress,
      });
      return;
    }

    if (!firstName.trim() || !lastName.trim() || !phone.trim()) {
      setError("Please fill in all required customer fields or select an existing customer.");
      return;
    }

    mutation.mutate({
      customerId: null,
      newCustomer: {
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        primaryPhone: phone.trim(),
        email: email.trim() || null,
        preferredContactMethod: preferredContact || null,
        billingAddress: null,
      },
      source: source || null,
      leadNotes: notes.trim() || null,
      propertyAddress,
    });
  };

  return (
    <div className="max-w-2xl mx-auto">
      {/* Page Header */}
      <div className="mb-6">
        <Link
          href="/app/leads"
          className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2"
        >
          <svg
            className="w-4 h-4"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M15 19l-7-7 7-7"
            />
          </svg>
          Back to Leads
        </Link>
        <h1 className="text-2xl font-bold text-slate-800">New Lead</h1>
        <p className="text-sm text-slate-500 mt-1">
          Create a new lead with customer and property information
        </p>
      </div>

      <form onSubmit={handleSubmit}>
        {/* Select existing customer (optional) */}
        <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">
            Select existing customer (optional)
          </h2>
          {customerId && selectedCustomer ? (
            <div className="flex items-center justify-between p-4 bg-slate-50 rounded-lg border border-slate-200">
              <div className="text-sm">
                <p className="font-medium text-slate-800">
                  {selectedCustomer.firstName} {selectedCustomer.lastName}
                </p>
                {(selectedCustomer.email || selectedCustomer.primaryPhone) && (
                  <p className="text-slate-600 mt-0.5">
                    {selectedCustomer.email ?? formatPhone(selectedCustomer.primaryPhone ?? null)}
                  </p>
                )}
              </div>
              <button
                type="button"
                onClick={() => {
                  setCustomerId("");
                  setCustomerSearch("");
                }}
                className="text-sm font-medium text-sky-600 hover:text-sky-700"
              >
                Change
              </button>
            </div>
          ) : (
            <Combobox
              items={customers}
              getItemLabel={(c) => `${c.firstName} ${c.lastName}`}
              getItemKey={(c) => c.id}
              onSelect={(c) => {
                setCustomerId(c.id);
                setCustomerSearch("");
              }}
              onSearchChange={setCustomerSearch}
              value={customerSearch}
              placeholder="Search by name, email, or phone…"
              renderItem={(c) => {
                const extra = formatPhone(c.primaryPhone ?? null) !== "—"
                  ? ` — ${formatPhone(c.primaryPhone ?? null)}`
                  : c.email
                    ? ` — ${c.email}`
                    : "";
                return `${c.firstName} ${c.lastName}${extra}`;
              }}
            />
          )}
        </div>

        {/* Customer Information (manual entry when no customer selected) */}
        {!customerId && (
        <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">
            Customer Information
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  First Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={firstName}
                  onChange={(e) => setFirstName(e.target.value)}
                  required
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                  placeholder="John"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  Last Name <span className="text-red-500">*</span>
                </label>
                <input
                  type="text"
                  value={lastName}
                  onChange={(e) => setLastName(e.target.value)}
                  required
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                  placeholder="Doe"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  Phone <span className="text-red-500">*</span>
                </label>
                <input
                  type="tel"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  required
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                  placeholder="(555) 123-4567"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  Email
                </label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                  placeholder="john@example.com"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  Preferred contact method
                </label>
                <select
                  value={preferredContact}
                  onChange={(e) => setPreferredContact((e.target.value || "") as PreferredContactMethod | "")}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                >
                  <option value="">Select…</option>
                  {PREFERRED_CONTACT_METHODS.map((m) => (
                    <option key={m} value={m}>
                      {PREFERRED_CONTACT_LABELS[m]}
                    </option>
                  ))}
                </select>
              </div>
            </div>
        </div>
        )}

        {/* Property Address */}
        <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">
            Property Address
          </h2>

          {customerId && (
            <BillingAddressAssist
              billingAddress={selectedCustomer?.billingAddress ?? null}
              onUseBillingAddress={handleUseBillingAddress}
              onClearAddress={handleClearAddress}
              disabled={!selectedCustomer}
              className="mb-4"
            />
          )}

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">
                Address Line 1 <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={addressLine1}
                onChange={(e) => setAddressLine1(e.target.value)}
                required
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                placeholder="123 Main Street"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">
                Address Line 2
              </label>
              <input
                type="text"
                value={addressLine2}
                onChange={(e) => setAddressLine2(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                placeholder="Apt 4B"
              />
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div className="col-span-2">
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  City
                </label>
                <input
                  type="text"
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                  placeholder="Denver"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  State
                </label>
                <input
                  type="text"
                  value={state}
                  onChange={(e) => setState(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                  placeholder="CO"
                  maxLength={2}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">
                  ZIP
                </label>
                <input
                  type="text"
                  value={zip}
                  onChange={(e) => setZip(e.target.value)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                  placeholder="80202"
                  maxLength={10}
                />
              </div>
            </div>
          </div>
        </div>

        {/* Lead Details */}
        <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">
            Lead Details
          </h2>

          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">
                Lead Source
              </label>
              <select
                value={source}
                onChange={(e) => setSource(e.target.value as LeadSource | "")}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
              >
                <option value="">Select a source</option>
                {LEAD_SOURCES.map((s) => (
                  <option key={s} value={s}>
                    {SOURCE_LABELS[s]}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">
                Notes
              </label>
              <textarea
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={4}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent resize-none"
                placeholder="Add any relevant notes about this lead..."
              />
            </div>
          </div>
        </div>

        {/* Error Message */}
        {error && (
          <div className="mb-6 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-4 py-3 flex items-center gap-2">
            <svg
              className="w-4 h-4 flex-shrink-0"
              fill="currentColor"
              viewBox="0 0 20 20"
            >
              <path
                fillRule="evenodd"
                d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z"
                clipRule="evenodd"
              />
            </svg>
            {error}
          </div>
        )}

        {/* Actions */}
        <div className="flex gap-3">
          <button
            type="submit"
            disabled={mutation.isPending}
            className="px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg disabled:opacity-60 transition-colors shadow-sm"
          >
            {mutation.isPending ? (
              <span className="flex items-center gap-2">
                <svg
                  className="animate-spin h-4 w-4"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    className="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    strokeWidth="4"
                  />
                  <path
                    className="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                  />
                </svg>
                Creating...
              </span>
            ) : (
              "Create Lead"
            )}
          </button>
          <Link
            href="/app/leads"
            className="px-4 py-2.5 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors"
          >
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
