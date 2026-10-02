import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { ChatApiClient, type ChatApiClientOptions } from "../client/ChatApiClient";
import { ChatSocketClient, type ChatSocketClientOptions } from "../client/ChatSocketClient";
import type { Conversation, ConversationType, ExternalReference } from "../client/types";

export interface ChatProviderProps {
  /**
   * The host app's own identifier for the current user — the same value it maps to
   * `senderId`/`participantId` server-side. The backend derives identity from the
   * authenticated request itself, but the UI still needs to know "which messages are mine"
   * for rendering (alignment, edit/delete controls), and chat has no user directory of its
   * own to ask.
   */
  currentUserId: string;
  apiOptions?: ChatApiClientOptions;
  socketOptions?: ChatSocketClientOptions;
  children: ReactNode;
}

export interface ChatContextValue {
  currentUserId: string;
  api: ChatApiClient;
  socket: ChatSocketClient;
  conversations: Conversation[];
  conversationsLoading: boolean;
  conversationsError: Error | null;
  refreshConversations: () => Promise<void>;
  createConversation: (
    type: ConversationType,
    participantExternalUserIds: string[],
    externalReference?: ExternalReference,
  ) => Promise<Conversation>;
}

const ChatContext = createContext<ChatContextValue | null>(null);

/**
 * The conversation list lives here, in the provider, rather than in `useConversations` itself —
 * a plain per-call-site `useState` would give every component that calls the hook its own,
 * unsynchronized copy of "the list," so a conversation created from one component (e.g. a
 * "start conversation" button elsewhere in the host app) would never appear in another (e.g.
 * `<Chat />`'s own internal list) until an unrelated remount happened to refetch it. One shared
 * copy in context, read by every `useConversations()` call, is what makes "call
 * createConversation anywhere, see it show up in `<Chat />`" actually true.
 */
export function ChatProvider({ currentUserId, apiOptions, socketOptions, children }: ChatProviderProps) {
  // apiOptions/socketOptions are read only at construction, deliberately excluded from these
  // dependency arrays — re-creating the clients on every options-object identity change would
  // tear down and reconnect the socket far more often than intended.
  const api = useMemo(() => new ChatApiClient(apiOptions), []);
  const socket = useMemo(() => new ChatSocketClient(socketOptions), []);

  useEffect(() => {
    socket.connect();
    return () => {
      void socket.disconnect();
    };
  }, [socket]);

  const [conversations, setConversations] = useState<Conversation[]>([]);
  const [conversationsLoading, setConversationsLoading] = useState(true);
  const [conversationsError, setConversationsError] = useState<Error | null>(null);

  const refreshConversations = useCallback(async () => {
    setConversationsLoading(true);
    try {
      setConversations(await api.getConversations());
      setConversationsError(null);
    } catch (e) {
      setConversationsError(e instanceof Error ? e : new Error(String(e)));
    } finally {
      setConversationsLoading(false);
    }
  }, [api]);

  useEffect(() => {
    void refreshConversations();
    // Deliberately re-fetches whenever `currentUserId` changes (host app switches which user
    // it's acting as) — refreshConversations itself is stable across re-renders otherwise.
  }, [refreshConversations, currentUserId]);

  const createConversation = useCallback(
    async (type: ConversationType, participantExternalUserIds: string[], externalReference?: ExternalReference) => {
      const conversation = await api.createConversation({
        type,
        participantExternalUserIds,
        externalReference: externalReference ?? null,
      });
      setConversations((prev) => [conversation, ...prev]);
      return conversation;
    },
    [api],
  );

  const value = useMemo<ChatContextValue>(
    () => ({
      currentUserId,
      api,
      socket,
      conversations,
      conversationsLoading,
      conversationsError,
      refreshConversations,
      createConversation,
    }),
    [currentUserId, api, socket, conversations, conversationsLoading, conversationsError, refreshConversations, createConversation],
  );

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
}

export function useChatContext(): ChatContextValue {
  const context = useContext(ChatContext);
  if (!context) {
    throw new Error("useChatContext (and every my-chat-react hook/component) must be used within a <ChatProvider>");
  }
  return context;
}
