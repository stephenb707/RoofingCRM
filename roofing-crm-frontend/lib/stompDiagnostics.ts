function headersCopy(raw: unknown): Record<string, string> | undefined {
  if (!raw || typeof raw !== "object") return undefined;
  const out: Record<string, string> = {};
  for (const [k, v] of Object.entries(raw as Record<string, unknown>)) {
    if (v != null && (typeof v === "string" || typeof v === "number" || typeof v === "boolean")) {
      out[k] = String(v);
    }
  }
  return Object.keys(out).length ? out : undefined;
}

/** Pulls useful fields from @stomp/stompjs IMessage / frame objects (they don't stringify well). */
export function stompFrameSummary(frame: unknown): {
  command?: string;
  body?: string;
  headers?: Record<string, string>;
} {
  if (frame == null || typeof frame !== "object") {
    return { body: frame === undefined || frame === null ? "(empty)" : String(frame) };
  }
  const f = frame as {
    command?: string;
    body?: string;
    headers?: Record<string, string>;
  };
  return {
    command: f.command,
    body: f.body,
    headers: f.headers ? { ...f.headers } : headersCopy(f),
  };
}

export function formatWebSocketCloseDiagnostics(evt: Event): string {
  const anyEvt = evt as CloseEvent;
  const parts: string[] = [];
  if (typeof anyEvt.code === "number") parts.push(`code=${anyEvt.code}`);
  if (anyEvt.reason) parts.push(`reason="${anyEvt.reason}"`);
  if (typeof anyEvt.wasClean === "boolean") parts.push(`wasClean=${anyEvt.wasClean}`);
  if (evt?.type) parts.push(`type=${evt.type}`);
  return parts.length ? parts.join(" ") : "no close details";
}

/** True when headers/body suggest broker rejected credentials / access. */
export function stompErrorLooksLikeAuthFailure(summary: {
  headers?: Record<string, string>;
  body?: string;
}): boolean {
  const h = summary.headers ?? {};
  const blob = `${h.message ?? ""} ${h["message"] ?? ""} ${summary.body ?? ""}`.toLowerCase();
  return (
    blob.includes("unauthor") ||
    blob.includes("access denied") ||
    blob.includes("forbidden") ||
    blob.includes("401") ||
    blob.includes("403")
  );
}
