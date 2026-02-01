"use client";

import { FormEvent, useState, useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { createTask } from "@/lib/tasksApi";
import { listUsers } from "@/lib/usersApi";
import { searchLeadsPicker, getLead } from "@/lib/leadsApi";
import { searchJobsPicker, getJob } from "@/lib/jobsApi";
import { listCustomers, getCustomer } from "@/lib/customersApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { TASK_STATUS_OPTIONS, TASK_PRIORITY_OPTIONS } from "@/lib/tasksConstants";
import { Combobox } from "@/components/Combobox";
import { DateTimePicker } from "@/components/DateTimePicker";
import type { TaskStatus, TaskPriority, CreateTaskRequest } from "@/lib/types";

function toIsoString(value: string): string | null {
  if (!value || !value.trim()) return null;
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? null : d.toISOString();
}

export default function NewTaskPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();

  const leadIdParam = searchParams.get("leadId") ?? "";
  const jobIdParam = searchParams.get("jobId") ?? "";
  const customerIdParam = searchParams.get("customerId") ?? "";

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [status, setStatus] = useState<TaskStatus>("TODO");
  const [priority, setPriority] = useState<TaskPriority>("MEDIUM");
  const [dueAtLocal, setDueAtLocal] = useState("");
  const [assignToMe, setAssignToMe] = useState(false);
  const [assignedToUserId, setAssignedToUserId] = useState("");
  const [assignedToDisplayName, setAssignedToDisplayName] = useState("");
  const [assigneeSearch, setAssigneeSearch] = useState("");
  const [debouncedAssigneeSearch, setDebouncedAssigneeSearch] = useState("");
  const [leadId, setLeadId] = useState(leadIdParam);
  const [leadSearch, setLeadSearch] = useState("");
  const [debouncedLeadSearch, setDebouncedLeadSearch] = useState("");
  const [jobId, setJobId] = useState(jobIdParam);
  const [jobSearch, setJobSearch] = useState("");
  const [debouncedJobSearch, setDebouncedJobSearch] = useState("");
  const [customerId, setCustomerId] = useState(customerIdParam);
  const [customerSearch, setCustomerSearch] = useState("");
  const [debouncedCustomerSearch, setDebouncedCustomerSearch] = useState("");

  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setLeadId(leadIdParam);
    setJobId(jobIdParam);
    setCustomerId(customerIdParam);
  }, [leadIdParam, jobIdParam, customerIdParam]);

  useEffect(() => {
    const t = setTimeout(() => setDebouncedAssigneeSearch(assigneeSearch), 300);
    return () => clearTimeout(t);
  }, [assigneeSearch]);
  useEffect(() => {
    const t = setTimeout(() => setDebouncedLeadSearch(leadSearch), 300);
    return () => clearTimeout(t);
  }, [leadSearch]);
  useEffect(() => {
    const t = setTimeout(() => setDebouncedJobSearch(jobSearch), 300);
    return () => clearTimeout(t);
  }, [jobSearch]);
  useEffect(() => {
    const t = setTimeout(() => setDebouncedCustomerSearch(customerSearch), 300);
    return () => clearTimeout(t);
  }, [customerSearch]);

  const { data: users = [] } = useQuery({
    queryKey: ["users", auth.selectedTenantId, debouncedAssigneeSearch],
    queryFn: () => listUsers(api, { q: debouncedAssigneeSearch || null, limit: 20 }),
    enabled: ready && !!auth.selectedTenantId && !assignToMe,
  });

  const { data: leads = [] } = useQuery({
    queryKey: ["leadsPicker", auth.selectedTenantId, debouncedLeadSearch],
    queryFn: () => searchLeadsPicker(api, { q: debouncedLeadSearch || null, limit: 20 }),
    enabled: ready && !!auth.selectedTenantId && !leadId,
  });

  const { data: jobs = [] } = useQuery({
    queryKey: ["jobsPicker", auth.selectedTenantId, debouncedJobSearch],
    queryFn: () => searchJobsPicker(api, { q: debouncedJobSearch || null, limit: 20 }),
    enabled: ready && !!auth.selectedTenantId && !jobId,
  });

  const { data: customersData } = useQuery({
    queryKey: ["customers", auth.selectedTenantId, debouncedCustomerSearch || null, 0],
    queryFn: () => listCustomers(api, { page: 0, size: 20, q: debouncedCustomerSearch || null }),
    enabled: ready && !!auth.selectedTenantId && !customerId,
  });
  const customers = customersData?.content ?? [];

  const { data: selectedLead } = useQuery({
    queryKey: ["lead", auth.selectedTenantId, leadId],
    queryFn: () => getLead(api, leadId),
    enabled: ready && !!leadId,
  });

  const { data: selectedJob } = useQuery({
    queryKey: ["job", auth.selectedTenantId, jobId],
    queryFn: () => getJob(api, jobId),
    enabled: ready && !!jobId,
  });

  const { data: selectedCustomer } = useQuery({
    queryKey: ["customer", auth.selectedTenantId, customerId],
    queryFn: () => getCustomer(api, customerId),
    enabled: ready && !!customerId,
  });

  const mutation = useMutation({
    mutationFn: async (payload: CreateTaskRequest) => createTask(api, payload),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ["tasks", auth.selectedTenantId] });
      router.push(`/app/tasks/${data.taskId}`);
    },
    onError: (err: unknown) => {
      console.error("Failed to create task:", err);
      setError(getApiErrorMessage(err, "Failed to create task. Please try again."));
    },
  });

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!title.trim()) {
      setError("Title is required.");
      return;
    }

    const resolvedAssignee = assignToMe && auth.userId
      ? auth.userId
      : assignedToUserId || null;

    const payload: CreateTaskRequest = {
      title: title.trim(),
      description: description.trim() || null,
      status,
      priority,
      dueAt: toIsoString(dueAtLocal),
      assignedToUserId: resolvedAssignee,
      leadId: leadId || null,
      jobId: jobId || null,
      customerId: customerId || null,
    };

    mutation.mutate(payload);
  };

  return (
    <div className="max-w-2xl mx-auto">
      <div className="mb-6">
        <Link
          href="/app/tasks"
          className="text-sm text-slate-500 hover:text-slate-700 flex items-center gap-1 mb-2"
        >
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
          Back to Tasks
        </Link>
        <h1 className="text-2xl font-bold text-slate-800">New Task</h1>
        <p className="text-sm text-slate-500 mt-1">Create a new task or follow-up</p>
      </div>

      <form onSubmit={handleSubmit}>
        <div className="bg-white rounded-xl border border-slate-200 p-6 mb-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Task Details</h2>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">
                Title <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                required
                maxLength={255}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                placeholder="e.g. Follow up with customer"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Description</label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={4}
                className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent resize-none"
                placeholder="Add details..."
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Status</label>
                <select
                  value={status}
                  onChange={(e) => setStatus(e.target.value as TaskStatus)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                >
                  {TASK_STATUS_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">Priority</label>
                <select
                  value={priority}
                  onChange={(e) => setPriority(e.target.value as TaskPriority)}
                  className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
                >
                  {TASK_PRIORITY_OPTIONS.map((opt) => (
                    <option key={opt.value} value={opt.value}>{opt.label}</option>
                  ))}
                </select>
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Due Date & Time</label>
              <DateTimePicker
                value={dueAtLocal}
                onChange={setDueAtLocal}
                placeholder="Select date and time…"
              />
            </div>

            <div>
              <label className="flex items-center gap-2 cursor-pointer mb-2">
                <input
                  type="checkbox"
                  checked={assignToMe}
                  onChange={(e) => {
                    setAssignToMe(e.target.checked);
                    if (e.target.checked) setAssignedToUserId("");
                  }}
                  className="rounded border-slate-300 text-sky-600 focus:ring-sky-500"
                />
                <span className="text-sm font-medium text-slate-700">Assign to me</span>
              </label>
              {!assignToMe && (
                assignedToUserId ? (
                  <div className="flex items-center justify-between p-3 bg-slate-50 rounded-lg border border-slate-200">
                    <span className="text-sm font-medium text-slate-800">
                      {(assignedToDisplayName || users.find((u) => u.id === assignedToUserId)?.name) ?? assignedToUserId}
                    </span>
                    <button
                      type="button"
                      onClick={() => { setAssignedToUserId(""); setAssignedToDisplayName(""); }}
                      className="text-sm font-medium text-sky-600 hover:text-sky-700"
                    >
                      Change
                    </button>
                  </div>
                ) : (
                  <Combobox
                    items={users}
                    getItemLabel={(u) => u.name || u.email}
                    getItemKey={(u) => u.id}
                    onSelect={(u) => {
                      setAssignedToUserId(u.id);
                      setAssignedToDisplayName(u.name || u.email);
                      setAssigneeSearch("");
                    }}
                    onSearchChange={setAssigneeSearch}
                    value={assigneeSearch}
                    placeholder="Search by name or email…"
                    renderItem={(u) => `${u.name || u.email}${u.email ? ` — ${u.email}` : ""}`}
                  />
                )
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Related Lead</label>
              {leadId && selectedLead ? (
                <div className="flex items-center justify-between p-3 bg-slate-50 rounded-lg border border-slate-200">
                  <span className="text-sm font-medium text-slate-800">
                    {selectedLead.customerFirstName} {selectedLead.customerLastName}
                    {selectedLead.propertyAddress?.line1 ? ` — ${selectedLead.propertyAddress.line1}` : ""}
                  </span>
                  <button
                    type="button"
                    onClick={() => { setLeadId(""); setLeadSearch(""); }}
                    className="text-sm font-medium text-sky-600 hover:text-sky-700"
                  >
                    Change
                  </button>
                </div>
              ) : (
                <Combobox
                  items={leads}
                  getItemLabel={(l) => l.label}
                  getItemKey={(l) => l.id}
                  onSelect={(l) => {
                    setLeadId(l.id);
                    setLeadSearch("");
                  }}
                  onSearchChange={setLeadSearch}
                  value={leadSearch}
                  placeholder="Search leads…"
                  renderItem={(l) => (
                    <>
                      {l.label}
                      {l.subLabel ? <span className="text-slate-500"> — {l.subLabel}</span> : null}
                    </>
                  )}
                />
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Related Job</label>
              {jobId && selectedJob ? (
                <div className="flex items-center justify-between p-3 bg-slate-50 rounded-lg border border-slate-200">
                  <span className="text-sm font-medium text-slate-800">
                    {selectedJob.type} — {selectedJob.propertyAddress?.line1 ?? selectedJob.customerFirstName}
                  </span>
                  <button
                    type="button"
                    onClick={() => { setJobId(""); setJobSearch(""); }}
                    className="text-sm font-medium text-sky-600 hover:text-sky-700"
                  >
                    Change
                  </button>
                </div>
              ) : (
                <Combobox
                  items={jobs}
                  getItemLabel={(j) => j.label}
                  getItemKey={(j) => j.id}
                  onSelect={(j) => {
                    setJobId(j.id);
                    setJobSearch("");
                  }}
                  onSearchChange={setJobSearch}
                  value={jobSearch}
                  placeholder="Search jobs…"
                  renderItem={(j) => (
                    <>
                      {j.label}
                      {j.subLabel ? <span className="text-slate-500"> — {j.subLabel}</span> : null}
                    </>
                  )}
                />
              )}
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">Related Customer</label>
              {customerId && selectedCustomer ? (
                <div className="flex items-center justify-between p-3 bg-slate-50 rounded-lg border border-slate-200">
                  <span className="text-sm font-medium text-slate-800">
                    {selectedCustomer.firstName} {selectedCustomer.lastName}
                  </span>
                  <button
                    type="button"
                    onClick={() => { setCustomerId(""); setCustomerSearch(""); }}
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
                  placeholder="Search customers…"
                  renderItem={(c) => `${c.firstName} ${c.lastName}${c.email ? ` — ${c.email}` : ""}`}
                />
              )}
            </div>
          </div>
        </div>

        {error && (
          <div className="mb-6 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-4 py-3 flex items-center gap-2">
            <svg className="w-4 h-4 flex-shrink-0" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clipRule="evenodd" />
            </svg>
            {error}
          </div>
        )}

        <div className="flex gap-3">
          <button
            type="submit"
            disabled={mutation.isPending}
            className="px-4 py-2.5 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg disabled:opacity-60 transition-colors shadow-sm"
          >
            {mutation.isPending ? (
              <span className="flex items-center gap-2">
                <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                </svg>
                Creating...
              </span>
            ) : (
              "Create Task"
            )}
          </button>
          <Link href="/app/tasks" className="px-4 py-2.5 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50 transition-colors">
            Cancel
          </Link>
        </div>
      </form>
    </div>
  );
}
