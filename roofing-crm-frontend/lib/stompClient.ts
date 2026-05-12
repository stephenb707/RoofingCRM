/**
 * STOMP over SockJS for real-time activity subscriptions.
 * SockJS requires http/https URLs (not ws/wss).
 */

import { Client, TickerStrategy } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { isJwtExpired } from "./jwtAccessToken";
import {
  formatWebSocketCloseDiagnostics,
  stompErrorLooksLikeAuthFailure,
  stompFrameSummary,
} from "./stompDiagnostics";

function getSockJsBaseUrl(): string {
  const apiBase = process.env.NEXT_PUBLIC_API_BASE_URL;
  if (apiBase) {
    return new URL(apiBase).origin;
  }
  if (typeof window !== "undefined") {
    return window.location.origin;
  }
  return "http://localhost:8080";
}

const verboseStompLogs = process.env.NODE_ENV !== "production";

export type StompConnectionIssue =
  | { kind: "stomp_error"; summary: string; headers?: Record<string, string>; body?: string }
  | { kind: "websocket_error"; summary: string }
  | { kind: "websocket_close"; summary: string; code?: number; reason?: string; wasClean?: boolean }
  | { kind: "heartbeat_lost"; summary: string }
  | { kind: "auth"; summary: string; terminal: true };

export {
  formatWebSocketCloseDiagnostics,
  stompErrorLooksLikeAuthFailure,
  stompFrameSummary,
} from "./stompDiagnostics";

export interface CreateStompClientOptions {
  /** Called for every new WebSocket (including reconnects) — must return the current access token. */
  getToken: () => string | null | undefined;
  /** Optional silent refresh hook used before reconnecting with an expired/missing token. */
  refreshAccessToken?: () => Promise<string | null>;
  onConnect: (client: Client) => void;
  /** Non-fatal diagnostics; {@link StompConnectionIssue.kind} === "auth" with terminal ends reconnects. */
  onConnectionIssue?: (issue: StompConnectionIssue) => void;
}

export function createStompClient(options: CreateStompClientOptions): Client {
  const { getToken, refreshAccessToken, onConnect, onConnectionIssue } = options;
  const base = getSockJsBaseUrl();
  const sockjsPath = `${base}/ws`;

  const client = new Client({
    reconnectDelay: 5000,
    heartbeatIncoming: 10_000,
    heartbeatOutgoing: 10_000,
    /** Reduces false "heartbeat lost" when the browser throttles timers in background tabs. */
    heartbeatStrategy:
      typeof Worker !== "undefined" ? TickerStrategy.Worker : TickerStrategy.Interval,
    /** Slightly more tolerant than default (2) for delayed PONGs under load / tab backgrounding. */
    heartbeatToleranceMultiplier: 3,
    /** Spring simple broker can chunk large STOMP frames; recommended in stompjs docs for Java brokers. */
    splitLargeFrames: true,
    webSocketFactory: () => {
      const token = getToken();
      const url = token
        ? `${sockjsPath}?token=${encodeURIComponent(token)}`
        : sockjsPath;
      return new SockJS(url) as unknown as WebSocket;
    },
    beforeConnect: async (c) => {
      let token = getToken();
      if ((!token || isJwtExpired(token)) && refreshAccessToken) {
        token = await refreshAccessToken();
      }
      if (!token) {
        onConnectionIssue?.({
          kind: "auth",
          summary: "No access token; stopping activity WebSocket reconnect loop.",
          terminal: true,
        });
        c.reconnectDelay = 0;
        await c.deactivate();
        return;
      }
      if (isJwtExpired(token)) {
        onConnectionIssue?.({
          kind: "auth",
          summary: "Access token expired and refresh failed; live activity updates are paused until you sign in again.",
          terminal: true,
        });
        c.reconnectDelay = 0;
        await c.deactivate();
        return;
      }
    },
    onConnect: () => {
      if (verboseStompLogs) {
        console.debug("[stomp] connected", sockjsPath);
      }
      onConnect(client);
    },
    onDisconnect: () => {
      if (verboseStompLogs) {
        console.debug("[stomp] DISCONNECT frame or broker closed session");
      }
    },
    onStompError: (frame) => {
      const { command, body, headers } = stompFrameSummary(frame);
      const summary = `[stomp] broker ERROR${command ? ` command=${command}` : ""}${
        body && body.length ? `: ${body}` : headers?.message ? `: ${headers.message}` : " (no body / message header)"
      }`;
      onConnectionIssue?.({
        kind: "stomp_error",
        summary,
        headers,
        body: body || undefined,
      });
      if (verboseStompLogs) {
        console.warn(summary, headers && Object.keys(headers).length ? { headers } : "");
      } else if (stompErrorLooksLikeAuthFailure({ headers, body })) {
        console.warn(summary);
      }
      if (stompErrorLooksLikeAuthFailure({ headers, body })) {
        client.reconnectDelay = 0;
        void client.deactivate();
      }
    },
    onWebSocketError: (event) => {
      const summary = `[stomp] WebSocket transport error (${event?.type ?? "error"})`;
      onConnectionIssue?.({ kind: "websocket_error", summary });
      if (verboseStompLogs) {
        console.warn(summary);
      }
    },
    onWebSocketClose: (evt) => {
      const detail = formatWebSocketCloseDiagnostics(evt);
      const anyEvt = evt as CloseEvent;
      onConnectionIssue?.({
        kind: "websocket_close",
        summary: `[stomp] WebSocket closed: ${detail}`,
        code: typeof anyEvt.code === "number" ? anyEvt.code : undefined,
        reason: anyEvt.reason || undefined,
        wasClean: typeof anyEvt.wasClean === "boolean" ? anyEvt.wasClean : undefined,
      });
      if (verboseStompLogs) {
        console.debug("[stomp]", detail);
      }
    },
    onHeartbeatLost: () => {
      const summary =
        "[stomp] Server heartbeat not received in time (likely network or tab throttling); reconnecting if still active.";
      onConnectionIssue?.({ kind: "heartbeat_lost", summary });
      if (verboseStompLogs) {
        console.debug(summary);
      }
    },
  });

  return client;
}

/**
 * Topic for activity events on an entity.
 * Format: /topic/tenants/{tenantId}/activity/{entityType}/{entityId}
 */
export function activityTopic(
  tenantId: string,
  entityType: string,
  entityId: string
): string {
  return `/topic/tenants/${tenantId}/activity/${entityType}/${entityId}`;
}
