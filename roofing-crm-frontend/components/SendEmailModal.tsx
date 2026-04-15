"use client";

import { useState } from "react";

export interface SendEmailModalValues {
  recipientEmail: string;
  recipientName?: string;
  subject?: string;
  message?: string;
  expiresInDays?: number;
}

interface SendEmailModalProps {
  title: string;
  isSubmitting: boolean;
  error?: string | null;
  initialRecipientEmail?: string;
  initialRecipientName?: string;
  initialSubject?: string;
  initialMessage?: string;
  showSubjectField?: boolean;
  showExpiresField?: boolean;
  onClose: () => void;
  onSubmit: (values: SendEmailModalValues) => void | Promise<void>;
}

export function SendEmailModal({
  title,
  isSubmitting,
  error,
  initialRecipientEmail = "",
  initialRecipientName = "",
  initialSubject = "",
  initialMessage = "",
  showSubjectField = false,
  showExpiresField = true,
  onClose,
  onSubmit,
}: SendEmailModalProps) {
  const titleId = "send-email-modal-title";
  const [recipientEmail, setRecipientEmail] = useState(initialRecipientEmail);
  const [recipientName, setRecipientName] = useState(initialRecipientName);
  const [subject, setSubject] = useState(initialSubject);
  const [message, setMessage] = useState(initialMessage);
  const [expiresInDays, setExpiresInDays] = useState("14");

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className="bg-white rounded-xl shadow-xl max-w-lg w-full mx-4 p-6"
      >
        <h3 id={titleId} className="text-lg font-semibold text-slate-800 mb-4">
          {title}
        </h3>
        <form
          onSubmit={(e) => {
            e.preventDefault();
            onSubmit({
              recipientEmail: recipientEmail.trim(),
              recipientName: recipientName.trim() || undefined,
              subject: subject.trim() || undefined,
              message: message.trim() || undefined,
              expiresInDays:
                showExpiresField && expiresInDays.trim() ? Number(expiresInDays) : undefined,
            });
          }}
          className="space-y-4"
        >
          <div>
            <label htmlFor="send-email-recipient" className="block text-sm font-medium text-slate-700 mb-1">
              Recipient email
            </label>
            <input
              id="send-email-recipient"
              type="email"
              value={recipientEmail}
              onChange={(e) => setRecipientEmail(e.target.value)}
              required
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          <div>
            <label htmlFor="send-email-name" className="block text-sm font-medium text-slate-700 mb-1">
              Recipient name (optional)
            </label>
            <input
              id="send-email-name"
              type="text"
              value={recipientName}
              onChange={(e) => setRecipientName(e.target.value)}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          {showSubjectField && (
            <div>
              <label htmlFor="send-email-subject" className="block text-sm font-medium text-slate-700 mb-1">
                Subject (optional)
              </label>
              <input
                id="send-email-subject"
                type="text"
                value={subject}
                onChange={(e) => setSubject(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
              />
            </div>
          )}
          <div>
            <label htmlFor="send-email-message" className="block text-sm font-medium text-slate-700 mb-1">
              Message (optional)
            </label>
            <textarea
              id="send-email-message"
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              rows={4}
              className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
            />
          </div>
          {showExpiresField && (
            <div>
              <label htmlFor="send-email-expires" className="block text-sm font-medium text-slate-700 mb-1">
                Link expires in days
              </label>
              <input
                id="send-email-expires"
                type="number"
                min={1}
                max={365}
                value={expiresInDays}
                onChange={(e) => setExpiresInDays(e.target.value)}
                className="w-full border border-slate-300 rounded-lg px-3 py-2 text-sm"
              />
            </div>
          )}
          {error && <p className="text-sm text-red-600">{error}</p>}
          <div className="flex gap-3 justify-end pt-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-sm font-medium text-slate-700 border border-slate-300 rounded-lg hover:bg-slate-50"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={!recipientEmail.trim() || isSubmitting}
              className="px-4 py-2 text-sm font-medium text-white bg-sky-600 rounded-lg hover:bg-sky-700 disabled:opacity-50"
            >
              {isSubmitting ? "Sending..." : "Send email"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
