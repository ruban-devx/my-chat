import { useChatContext } from "../context/ChatProvider";
import type { Conversation, ConversationType, ExternalReference } from "../client/types";

export interface UseConversationsResult {
  conversations: Conversation[];
  loading: boolean;
  error: Error | null;
  refresh: () => Promise<void>;
  /**
   * Chat has no user directory of its own — the host app is the one that knows who "user-42"
   * or "seller-7" is, so it's the host app's own UI (not this library's) that decides when to
   * call this, e.g. from a "Message seller" button elsewhere in the product.
   */
  createConversation: (
    type: ConversationType,
    participantExternalUserIds: string[],
    externalReference?: ExternalReference,
  ) => Promise<Conversation>;
}

/**
 * Thin facade over the conversation list that lives in {@link ChatProvider} — every call site
 * (this component, that component, `<Chat />`'s own internals) reads and updates the same
 * shared list, so a conversation created from anywhere shows up everywhere immediately. See
 * ChatProvider's doc comment for why this isn't its own local `useState`.
 */
export function useConversations(): UseConversationsResult {
  const { conversations, conversationsLoading, conversationsError, refreshConversations, createConversation } = useChatContext();
  return {
    conversations,
    loading: conversationsLoading,
    error: conversationsError,
    refresh: refreshConversations,
    createConversation,
  };
}
