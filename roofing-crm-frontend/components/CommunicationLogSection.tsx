"use client";

import { useState } from "react";
import { formatDateTime } from "@/lib/format";
import type {
  CommunicationChannel,
  CommunicationDirection,
  CommunicationLogDto,
  CreateCommunicationLogRequest,
} from "@/lib/types";

const CHANNELS: CommunicationChannel[] = ["NOTE", "CALL", "SMS", "EMAIL"];
const DIRECTIONS: CommunicationDirection[] = ["INBOUND", "OUTBOUND"];

function defaultSubjectForChannel(channel: CommunicationChannel): string {
  switch (channel) {
    case "CALL":
      return "Call";
    case "SMS":
      return "SMS";
    case "EMAIL":
      return "Email";
    case "NOTE":
      return "Note";
    default:
      return "Communication";
  }
}

function toLocalDatetimeInput(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return "";
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export interface CommunicationLogSectionProps {
  title: string;
  logs: CommunicationLogDto[];
  onAdd: (payload: CreateCommunicationLogRequest) => void | Promise<void>;
  isLoading?: boolean;
  isSubmitting?: boolean;
  errorMessage?: string | null;
}

export function CommunicationLogSection({
  title,
  logs,
  onAdd,
  isLoading = false,
  isSubmitting = false,
  errorMessage = null,
}: CommunicationLogSectionProps) {
  const [channel, setChannel] = useState<CommunicationChannel>("NOTE");
  const [direction, setDirection] = useState<CommunicationDirection | "">("");
  const [occurredAt, setOccurredAt] = useState<string>(() =>
    toLocalDatetimeInput(new Date().toISOString())
  );
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const occurred =
      occurredAt.trim() !== ""
        ? new Date(occurredAt).toISOString()
        : new Date().toISOString();
    const effectiveSubject =
      subject.trim() || defaultSubjectForChannel(channel);
    onAdd({
      channel,
      direction: direction || null,
      subject: effectiveSubject,
      body: body.trim() || null,
      occurredAt: occurred,
    });
    setSubject("");
    setBody("");
    setOccurredAt(toLocalDatetimeInput(new Date().toISOString()));
  };

  const sortedLogs = [...logs].sort(
    (a, b) => new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime()
  );

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-6">
      <h2 className="text-lg font-semibold text-slate-800 mb-4">{title}</h2>

      {errorMessage && (
        <div className="mb-4 text-sm text-red-600 bg-red-50 border border-red-100 rounded-lg px-3 py-2">
          {errorMessage}
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-4 mb-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          <div>
            <label htmlFor="comm-channel" className="block text-sm font-medium text-slate-700 mb-1">
              Channel
            </label>
            <select
              id="comm-channel"
              value={channel}
              onChange={(e) => setChannel(e.target.value as CommunicationChannel)}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              {CHANNELS.map((c) => (
                <option key={c} value={c}>
                  {c}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="comm-direction" className="block text-sm font-medium text-slate-700 mb-1">
              Direction
            </label>
            <select
              id="comm-direction"
              value={direction}
              onChange={(e) => setDirection(e.target.value as CommunicationDirection | "")}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            >
              <option value="">—</option>
              {DIRECTIONS.map((d) => (
                <option key={d} value={d}>
                  {d}
                </option>
              ))}
            </select>
          </div>
        </div>
        <div>
          <label htmlFor="comm-occurred" className="block text-sm font-medium text-slate-700 mb-1">
            Occurred at
          </label>
          <input
            id="comm-occurred"
            type="datetime-local"
            value={occurredAt}
            onChange={(e) => setOccurredAt(e.target.value)}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
          />
        </div>
        <div>
          <label htmlFor="comm-subject" className="block text-sm font-medium text-slate-700 mb-1">
            Subject
          </label>
          <input
            id="comm-subject"
            type="text"
            value={subject}
            onChange={(e) => setSubject(e.target.value)}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            placeholder="Auto-filled from channel if blank"
          />
        </div>
        <div>
          <label htmlFor="comm-body" className="block text-sm font-medium text-slate-700 mb-1">
            Body
          </label>
          <textarea
            id="comm-body"
            value={body}
            onChange={(e) => setBody(e.target.value)}
            rows={3}
            className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500"
            placeholder="Optional notes"
          />
        </div>
        <button
          type="submit"
          disabled={isSubmitting}
          className="px-4 py-2.5 text-sm font-medium text-white bg-sky-600 hover:bg-sky-700 rounded-lg disabled:opacity-60"
        >
          {isSubmitting ? "Adding…" : "Add log"}
        </button>
      </form>

      {isLoading ? (
        <p className="text-sm text-slate-500">Loading communication logs…</p>
      ) : sortedLogs.length === 0 ? (
        <p className="text-sm text-slate-500">No communication logs yet.</p>
      ) : (
        <ul className="space-y-4">
          {sortedLogs.map((log) => (
            <li key={log.id} className="py-3 border-b border-slate-100 last:border-0">
              <div className="flex flex-wrap items-center gap-2 mb-1">
                <span className="text-xs font-medium text-slate-600">
                  {formatDateTime(log.occurredAt)}
                </span>
                <span className="inline-flex px-2 py-0.5 text-xs font-medium rounded bg-slate-100 text-slate-700">
                  {log.channel}
                </span>
                {log.direction && (
                  <span className="inline-flex px-2 py-0.5 text-xs font-medium rounded bg-slate-100 text-slate-600">
                    {log.direction}
                  </span>
                )}
              </div>
              {log.subject && (
                <p className="text-sm font-medium text-slate-800">{log.subject}</p>
              )}
              {log.body && (
                <p className="text-sm text-slate-600 whitespace-pre-wrap mt-0.5">{log.body}</p>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
