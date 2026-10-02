import { useCallback, useEffect, useState } from "react";
import { useChatContext } from "../context/ChatProvider";
import type { Message } from "../client/types";
import { applyMessageEvent } from "./applyMessageEvent";

export interface UseMessagesResult {
  messages: Message[];
  loading: boolean;
  error: Error | null;
  sendMessage: (content: string) => Promise<Message>;
  editMessage: (messageId: string, content: string) => Promise<Message>;
  deleteMessage: (messageId: string) => Promise<void>;
}

/**
 * Loads the first page of history on mount/conversation change, then layers realtime events
 * on top via the socket. Reconciling "what happened between page load and socket connect" is
 * deliberately not solved here — see PROJECTBRAIN's realtime section; a single page-0 fetch is
 * the honest V1 behavior, not a hidden gap.
 */
export function useMessages(conversationId: string | null): UseMessagesResult {
  const { api, socket } = useChatContext();
  const [messages, setMessages] = useState<Message[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (!conversationId) {
      setMessages([]);
      return;
    }
    let cancelled = false;
    setLoading(true);
    api
      .getMessages(conversationId)
      .then((page) => {
        if (cancelled) return;
        // The API returns newest-first (for cheap "latest page" pagination); the UI wants
        // oldest-first, top to bottom.
        setMessages([...page.content].reverse());
        setError(null);
      })
      .catch((e) => {
        if (!cancelled) setError(e instanceof Error ? e : new Error(String(e)));
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [api, conversationId]);

  useEffect(() => {
    if (!conversationId) return;
    return socket.subscribeToConversation(conversationId, (event) => {
      setMessages((prev) => applyMessageEvent(prev, event));
    });
  }, [socket, conversationId]);

  const sendMessage = useCallback(
    async (content: string) => {
      if (!conversationId) {
        throw new Error("useMessages: no conversation selected");
      }
      const message = await api.sendMessage(conversationId, content);
      setMessages((prev) => applyMessageEvent(prev, { type: "message.created", message }));
      return message;
    },
    [api, conversationId],
  );

  const editMessage = useCallback(
    async (messageId: string, content: string) => {
      const message = await api.editMessage(messageId, content);
      setMessages((prev) => applyMessageEvent(prev, { type: "message.updated", message }));
      return message;
    },
    [api],
  );

  const deleteMessage = useCallback(
    async (messageId: string) => {
      await api.deleteMessage(messageId);
      setMessages((prev) => prev.filter((m) => m.id !== messageId));
    },
    [api],
  );

  return { messages, loading, error, sendMessage, editMessage, deleteMessage };
}
