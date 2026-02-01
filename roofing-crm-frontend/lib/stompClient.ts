/**
 * STOMP over SockJS client for real-time subscriptions.
 * Used for activity timeline updates.
 * SockJS requires http/https URLs (not ws/wss).
 */

import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";

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

export function createStompClient(
  token: string,
  onConnect: (client: Client) => void,
  onError?: (err: unknown) => void
): Client {
  const base = getSockJsBaseUrl();
  const sockjsUrl = `${base}/ws`;
  const url = token
    ? `${sockjsUrl}?token=${encodeURIComponent(token)}`
    : sockjsUrl;

  const client = new Client({
    webSocketFactory: () => new SockJS(url) as unknown as WebSocket,
    reconnectDelay: 3000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    onConnect: () => {
      console.debug("stomp connected", url);
      onConnect(client);
    },
    onStompError: (frame) => {
      console.error("[stomp] onStompError", frame);
      onError?.(frame);
    },
    onWebSocketError: (event) => {
      console.error("[stomp] onWebSocketError", event);
      onError?.(event);
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
