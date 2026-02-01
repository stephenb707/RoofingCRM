"use client";

import { useState, useEffect, useRef } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useAuthReady } from "@/lib/AuthContext";
import { listActivity, createNote } from "@/lib/activityApi";
import { getApiErrorMessage } from "@/lib/apiError";
import { queryKeys } from "@/lib/queryKeys";
import { formatDateTime } from "@/lib/format";
import {
  createStompClient,
  activityTopic,
} from "@/lib/stompClient";
import type {
  ActivityEntityType,
  ActivityEventDto,
  PageResponse,
} from "@/lib/types";

function getActorLabel(
  evt: ActivityEventDto,
  currentUserId: string | null
): string {
  if (evt.createdByUserId == null) return "System";
  if (currentUserId && evt.createdByUserId === currentUserId) return "You";
  if (evt.createdByName) return evt.createdByName;
  return "User";
}

const EVENT_TYPE_LABELS: Record<string, string> = {
  NOTE: "Note",
  LEAD_STATUS_CHANGED: "Status changed",
  JOB_STATUS_CHANGED: "Status changed",
  JOB_SCHEDULE_CHANGED: "Schedule updated",
  TASK_CREATED: "Task created",
  TASK_STATUS_CHANGED: "Task updated",
  LEAD_CONVERTED_TO_JOB: "Converted to job",
  ATTACHMENT_ADDED: "Attachment added",
};

export interface ActivitySectionProps {
  entityType: ActivityEntityType;
  entityId: string;
}

export function ActivitySection({ entityType, entityId }: ActivitySectionProps) {
  const { api, auth, ready } = useAuthReady();
  const queryClient = useQueryClient();
  const [noteBody, setNoteBody] = useState("");
  const [noteError, setNoteError] = useState<string | null>(null);

  const activityKey = queryKeys.activityForEntity(
    auth.selectedTenantId,
    entityType,
    entityId
  );

  // Subscribe to real-time activity updates for this entity
  const subRef = useRef<{ unsubscribe: () => void } | null>(null);
  useEffect(() => {
    if (
      typeof window === "undefined" ||
      !auth.token ||
      !auth.selectedTenantId ||
      !entityType ||
      !entityId
    ) {
      return;
    }
    const token = auth.token;
    const tenantId = auth.selectedTenantId;
    const topic = activityTopic(tenantId, entityType, entityId);
    const keyToInvalidate = queryKeys.activityForEntity(
      tenantId,
      entityType,
      entityId
    );
    subRef.current = null;

    const client = createStompClient(
      token,
      (c) => {
        subRef.current = c.subscribe(topic, () => {
          queryClient.invalidateQueries({ queryKey: keyToInvalidate });
        });
      },
      (err) => {
        console.error("[ActivitySection] STOMP error:", err);
      }
    );
    client.activate();

    return () => {
      if (subRef.current) {
        subRef.current.unsubscribe();
        subRef.current = null;
      }
      client.deactivate();
    };
  }, [auth.token, auth.selectedTenantId, entityType, entityId, queryClient]);

  const { data, isLoading, isError, error } = useQuery({
    queryKey: activityKey,
    queryFn: () =>
      listActivity(api, { entityType, entityId, page: 0, size: 20 }),
    enabled: ready && !!entityId,
    refetchOnWindowFocus: false,
  });

  const createNoteMutation = useMutation({
    mutationFn: (body: string) =>
      createNote(api, { entityType, entityId, body }),
    onMutate: async (body) => {
      await queryClient.cancelQueries({ queryKey: activityKey });
      const previous = queryClient.getQueryData<typeof data>(activityKey);
      const optimisticEvent: ActivityEventDto = {
        activityId: `temp-${Date.now()}`,
        entityType,
        entityId,
        eventType: "NOTE",
        message: body,
        createdAt: new Date().toISOString(),
        createdByUserId: auth.userId ?? null,
        createdByName: auth.fullName ?? null,
        metadata: null,
      };
      queryClient.setQueryData(activityKey, (old: typeof data) => {
        if (!old) {
          return {
            content: [optimisticEvent],
            totalElements: 1,
            totalPages: 1,
            number: 0,
            size: 20,
            first: true,
            last: true,
          } as PageResponse<ActivityEventDto>;
        }
        return {
          ...old,
          content: [optimisticEvent, ...old.content],
          totalElements: old.totalElements + 1,
        };
      });
      return { previous };
    },
    onSuccess: () => {
      setNoteBody("");
      setNoteError(null);
    },
    onError: (err, _body, context) => {
      setNoteError(getApiErrorMessage(err, "Failed to add note"));
      if (context?.previous) {
        queryClient.setQueryData(activityKey, context.previous);
      }
    },
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: activityKey });
    },
  });

  const handleSubmitNote = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = noteBody.trim();
    if (!trimmed) return;
    createNoteMutation.mutate(trimmed);
  };

  if (isLoading) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">Activity</h2>
        <p className="text-sm text-slate-500">Loading activity…</p>
      </div>
    );
  }

  if (isError) {
    return (
      <div className="bg-white rounded-xl border border-slate-200 p-6">
        <h2 className="text-lg font-semibold text-slate-800 mb-4">Activity</h2>
        <p className="text-sm text-red-600">
          {getApiErrorMessage(error, "Failed to load activity")}
        </p>
      </div>
    );
  }

  const events = data?.content ?? [];

  return (
    <div className="bg-white rounded-xl border border-slate-200 p-6">
      <h2 className="text-lg font-semibold text-slate-800 mb-4">Activity</h2>

      <form onSubmit={handleSubmitNote} className="mb-4">
        <textarea
          value={noteBody}
          onChange={(e) => setNoteBody(e.target.value)}
          placeholder="Add a note…"
          rows={2}
          className="w-full border border-slate-300 rounded-lg px-4 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-sky-500 focus:border-transparent"
          disabled={createNoteMutation.isPending}
        />
        <div className="mt-2 flex items-center gap-2">
          <button
            type="submit"
            disabled={!noteBody.trim() || createNoteMutation.isPending}
            className="px-4 py-2 bg-sky-600 hover:bg-sky-700 text-white text-sm font-medium rounded-lg disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {createNoteMutation.isPending ? "Adding…" : "Add note"}
          </button>
          {noteError && (
            <span className="text-sm text-red-600">{noteError}</span>
          )}
        </div>
      </form>

      <ul className="divide-y divide-slate-100 space-y-0">
        {events.length === 0 ? (
          <li className="py-2 text-sm text-slate-500">No activity yet</li>
        ) : (
          events.map((evt) => (
            <li key={evt.activityId} className="py-3 first:pt-0">
              <div className="flex items-start justify-between gap-2">
                <div>
                  <span className="text-xs font-medium text-slate-500 uppercase">
                    {EVENT_TYPE_LABELS[evt.eventType] ?? evt.eventType}
                  </span>
                  <span className="text-xs text-slate-400 ml-2">
                    {formatDateTime(evt.createdAt)} · {getActorLabel(evt, auth.userId ?? null)}
                  </span>
                </div>
              </div>
              <p className="mt-1 text-sm text-slate-700 whitespace-pre-wrap">
                {evt.message}
              </p>
            </li>
          ))
        )}
      </ul>
    </div>
  );
}
