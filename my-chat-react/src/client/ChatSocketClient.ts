import { Client, type StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import type { MessageEvent } from "./types";

export interface ChatSocketClientOptions {
  /** Default "/ws/chat" — matches the starter's default SockJS/STOMP endpoint. */
  socketUrl?: string;
  /**
   * STOMP CONNECT headers. Cookie/session auth needs nothing here (the browser attaches
   * cookies to SockJS's underlying XHR automatically); token-based auth can pass a header
   * here, e.g. `{ Authorization: "Bearer ..." }`.
   */
  connectHeaders?: Record<string, string>;
}

interface TopicSubscription {
  listeners: Set<(event: MessageEvent) => void>;
  live?: StompSubscription;
}

/**
 * Wraps @stomp/stompjs for the one thing this starter's realtime layer does: broadcast-only
 * per-conversation topics. There's no `send`/publish method here on purpose — writes always go
 * through {@link ChatApiClient}; this only ever subscribes.
 *
 * <p>Each conversation topic supports multiple independent listeners (a {@code Set}, not a
 * single callback) — two components both watching the same conversation (e.g. `useMessages`
 * called from two places, deliberately or via a remount race) each get their own event stream
 * without one silently displacing the other's callback.
 */
export class ChatSocketClient {
  private readonly client: Client;
  private readonly subscriptions = new Map<string, TopicSubscription>();

  constructor(options: ChatSocketClientOptions = {}) {
    const socketUrl = options.socketUrl ?? "/ws/chat";
    this.client = new Client({
      webSocketFactory: () => new SockJS(socketUrl) as WebSocket,
      connectHeaders: options.connectHeaders,
      reconnectDelay: 5000,
    });
    // Re-registers on every (re)connect, so a dropped connection resubscribes automatically.
    this.client.onConnect = () => {
      for (const [conversationId, subscription] of this.subscriptions) {
        subscription.live = this.subscribeOnBroker(conversationId, subscription.listeners);
      }
    };
  }

  connect(): void {
    if (!this.client.active) {
      this.client.activate();
    }
  }

  async disconnect(): Promise<void> {
    await this.client.deactivate();
  }

  /** Returns an unsubscribe function. Safe to call before the socket has connected. */
  subscribeToConversation(conversationId: string, onEvent: (event: MessageEvent) => void): () => void {
    let subscription = this.subscriptions.get(conversationId);
    if (!subscription) {
      subscription = { listeners: new Set() };
      this.subscriptions.set(conversationId, subscription);
    }
    subscription.listeners.add(onEvent);

    if (this.client.connected && !subscription.live) {
      subscription.live = this.subscribeOnBroker(conversationId, subscription.listeners);
    }

    return () => {
      const current = this.subscriptions.get(conversationId);
      if (!current) return;
      current.listeners.delete(onEvent);
      if (current.listeners.size === 0) {
        current.live?.unsubscribe();
        this.subscriptions.delete(conversationId);
      }
    };
  }

  private subscribeOnBroker(conversationId: string, listeners: Set<(event: MessageEvent) => void>): StompSubscription {
    return this.client.subscribe(topicFor(conversationId), (message) => {
      const event = JSON.parse(message.body) as MessageEvent;
      for (const listener of listeners) {
        listener(event);
      }
    });
  }
}

function topicFor(conversationId: string): string {
  return `/topic/chat.conversations.${conversationId}`;
}
