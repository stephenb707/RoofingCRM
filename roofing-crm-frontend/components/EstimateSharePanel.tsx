"use client";

import {
  useState,
  useCallback,
  forwardRef,
  useImperativeHandle,
} from "react";
import { useRouter } from "next/navigation";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { shareEstimate, sendEstimateEmail } from "@/lib/estimatesApi";
import { createTask } from "@/lib/tasksApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { queryKeys } from "@/lib/queryKeys";
import { NextStepPromptDialog } from "@/components/NextStepPromptDialog";
import { SendEmailModal } from "@/components/SendEmailModal";

export interface EstimateSharePanelProps {
  estimateId: string;
  jobId: string;
  customerId?: string | null;
  customerEmail?: string | null;
  customerName?: string | null;
  estimateTitle?: string | null;
  canShare: boolean;
  /** Sidebar card (estimate detail) vs stacked buttons (job latest estimate). */
  variant?: "default" | "compact";
}

export type EstimateSharePanelHandle = {
  openSendEmailModal: () => void;
  /** Same flow as Generate Link / Refresh Link in the Share card. */
  generateLink: () => void;
};

export const EstimateSharePanel = forwardRef<
  EstimateSharePanelHandle,
  EstimateSharePanelProps
>(function EstimateSharePanel(
  {
    estimateId,
    jobId,
    customerId,
    customerEmail,
    customerName,
    estimateTitle,
    canShare,
    variant = "default",
  },
  ref
) {
  const { api, auth } = useAuthReady();
  const queryClient = useQueryClient();
  const router = useRouter();

  const [shareLink, setShareLink] = useState<string | null>(null);
  const [shareExpiresAt, setShareExpiresAt] = useState<string | null>(null);
  const [showSharePrompt, setShowSharePrompt] = useState(false);
  const [showEmailModal, setShowEmailModal] = useState(false);
  const [emailSuccess, setEmailSuccess] = useState<string | null>(null);

  const hasPublicLink = Boolean(shareLink);

  const shareMutation = useMutation({
    mutationFn: () => shareEstimate(api, estimateId, { expiresInDays: 14 }),
    onSuccess: async (data) => {
      const url =
        typeof window !== "undefined" ? `${window.location.origin}/estimate/${data.token}` : "";
      setShareLink(url);
      setShareExpiresAt(data.expiresAt ?? null);
      queryClient.invalidateQueries({ queryKey: queryKeys.estimate(auth.selectedTenantId, estimateId) });
      if (jobId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.estimatesForJob(auth.selectedTenantId, jobId) });
      }
      if (typeof navigator !== "undefined" && navigator.clipboard?.writeText && url) {
        try {
          await navigator.clipboard.writeText(url);
        } catch {
          // no-op
        }
      }
      setShowSharePrompt(true);
    },
  });

  const followUpMutation = useMutation({
    mutationFn: async () => {
      const dueAt = new Date(Date.now() + 2 * 24 * 60 * 60 * 1000).toISOString();
      return createTask(api, {
        title: `Follow up on ${estimateTitle?.trim() || "estimate"}`,
        description: shareLink ? `Shared estimate link: ${shareLink}` : "Follow up on shared estimate.",
        priority: "MEDIUM",
        status: "TODO",
        dueAt,
        jobId,
        customerId: customerId ?? null,
      });
    },
    onSuccess: (task) => {
      router.push(`/app/tasks/${task.taskId}`);
    },
    onError: () => {
      const customer = customerId ? `&customerId=${customerId}` : "";
      router.push(`/app/tasks/new?jobId=${jobId}${customer}`);
    },
  });

  const sendEmailMutation = useMutation({
    mutationFn: (payload: {
      recipientEmail: string;
      recipientName?: string;
      message?: string;
      expiresInDays?: number;
    }) => sendEstimateEmail(api, estimateId, payload),
    onSuccess: (data, variables) => {
      setShareLink(data.publicUrl);
      setShareExpiresAt(null);
      setEmailSuccess(`Email sent to ${variables.recipientEmail}.`);
      setShowEmailModal(false);
      queryClient.invalidateQueries({ queryKey: queryKeys.estimate(auth.selectedTenantId, estimateId) });
      if (jobId) {
        queryClient.invalidateQueries({ queryKey: queryKeys.estimatesForJob(auth.selectedTenantId, jobId) });
        queryClient.invalidateQueries({ queryKey: queryKeys.activityForEntity(auth.selectedTenantId, "JOB", jobId) });
      }
    },
  });

  const handleCopyLink = useCallback(async () => {
    if (!shareLink) return;
    try {
      if (typeof navigator !== "undefined" && navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(shareLink);
      }
    } catch {
      // no-op
    }
    setShowSharePrompt(true);
  }, [shareLink]);

  const openSendEmailModal = useCallback(() => {
    setEmailSuccess(null);
    setShowEmailModal(true);
  }, []);

  const generateLink = useCallback(() => {
    shareMutation.mutate();
  }, [shareMutation]);

  useImperativeHandle(
    ref,
    () => ({
      openSendEmailModal,
      generateLink,
    }),
    [openSendEmailModal, generateLink]
  );

  if (!canShare) {
    return null;
  }

  const actionStack = (
    <div className="flex flex-col gap-2">
      {emailSuccess && (
        <div className="text-sm text-green-700 bg-green-50 border border-green-200 rounded-lg px-3 py-2">
          {emailSuccess}
        </div>
      )}
      <button
        type="button"
        data-testid="estimate-share-send-email"
        onClick={openSendEmailModal}
        disabled={sendEmailMutation.isPending}
        className="w-full px-4 py-2.5 text-sm font-medium text-white bg-sky-600 rounded-lg hover:bg-sky-700 disabled:opacity-60"
      >
        {sendEmailMutation.isPending ? "Sending..." : "Send Email"}
      </button>
      {!hasPublicLink ? (
        <button
          type="button"
          data-testid="estimate-share-generate-link"
          onClick={generateLink}
          disabled={shareMutation.isPending}
          className="w-full px-4 py-2.5 text-sm font-medium text-sky-700 border border-sky-300 rounded-lg hover:bg-sky-50 disabled:opacity-60"
        >
          {shareMutation.isPending ? "Generating..." : "Generate Link"}
        </button>
      ) : (
        <>
          <button
            type="button"
            data-testid="estimate-share-refresh-link"
            onClick={generateLink}
            disabled={shareMutation.isPending}
            className="w-full px-4 py-2.5 text-sm font-medium text-sky-700 border border-sky-300 rounded-lg hover:bg-sky-50 disabled:opacity-60"
          >
            {shareMutation.isPending ? "Generating..." : "Refresh Link"}
          </button>
          <button
            type="button"
            data-testid="estimate-share-copy-link"
            onClick={() => void handleCopyLink()}
            className="w-full px-3 py-2 text-xs font-medium text-slate-600 border border-slate-200 rounded-lg hover:bg-slate-50"
          >
            Copy Link
          </button>
        </>
      )}
    </div>
  );

  const linkPreview =
    hasPublicLink && shareLink ? (
      <div className="mt-4 space-y-2">
        <div className="grid gap-2 sm:grid-cols-[1fr_auto] sm:items-center">
          <input
            readOnly
            value={shareLink}
            data-testid="share-link-input"
            className="w-full min-w-0 truncate border border-slate-300 rounded-lg px-3 py-2 text-sm bg-slate-50"
          />
          <span className="hidden sm:block" />
        </div>
        {shareExpiresAt && (
          <p className="text-xs text-slate-500">Expires {new Date(shareExpiresAt).toLocaleString()}</p>
        )}
      </div>
    ) : null;

  return (
    <>
      {variant === "default" ? (
        <div className="bg-white rounded-xl border border-slate-200 p-6">
          <h2 className="text-lg font-semibold text-slate-800 mb-4">Share</h2>
          {actionStack}
          {linkPreview}
        </div>
      ) : (
        <div className="space-y-2">
          {actionStack}
          {linkPreview}
        </div>
      )}

      {showSharePrompt && (
        <NextStepPromptDialog
          title="Link copied"
          description="Your estimate link is ready."
          actions={[
            {
              label: "Preview customer view",
              onClick: () => {
                if (shareLink) {
                  window.open(shareLink, "_blank", "noopener,noreferrer");
                }
              },
              testId: "share-next-step-preview",
            },
            {
              label: followUpMutation.isPending ? "Creating follow-up..." : "Set follow-up in 2 days",
              onClick: () => followUpMutation.mutate(),
              variant: "secondary",
              disabled: followUpMutation.isPending,
              testId: "share-next-step-followup",
            },
            {
              label: "Done",
              onClick: () => setShowSharePrompt(false),
              variant: "secondary",
              testId: "share-next-step-done",
            },
          ]}
          onClose={() => setShowSharePrompt(false)}
          showDismissButton={false}
        />
      )}
      {showEmailModal && (
        <SendEmailModal
          title="Send estimate by email"
          isSubmitting={sendEmailMutation.isPending}
          error={
            sendEmailMutation.isError
              ? getApiErrorMessage(sendEmailMutation.error, "Failed to send estimate email.")
              : null
          }
          initialRecipientEmail={customerEmail ?? ""}
          initialRecipientName={customerName ?? ""}
          onClose={() => setShowEmailModal(false)}
          onSubmit={(values) => sendEmailMutation.mutate(values)}
        />
      )}
    </>
  );
});
