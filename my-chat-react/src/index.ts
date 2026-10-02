import "./styles.css";

export { ChatProvider, useChatContext } from "./context/ChatProvider";
export type { ChatProviderProps, ChatContextValue } from "./context/ChatProvider";

export { Chat } from "./components/Chat";
export { ConversationList } from "./components/ConversationList";
export type { ConversationListProps } from "./components/ConversationList";
export { MessageList } from "./components/MessageList";
export type { MessageListProps } from "./components/MessageList";
export { MessageComposer } from "./components/MessageComposer";
export type { MessageComposerProps } from "./components/MessageComposer";

export { useConversations } from "./hooks/useConversations";
export type { UseConversationsResult } from "./hooks/useConversations";
export { useMessages } from "./hooks/useMessages";
export type { UseMessagesResult } from "./hooks/useMessages";
export { applyMessageEvent } from "./hooks/applyMessageEvent";

export { ChatApiClient, ChatApiError } from "./client/ChatApiClient";
export type { ChatApiClientOptions, CreateConversationRequest } from "./client/ChatApiClient";
export { ChatSocketClient } from "./client/ChatSocketClient";
export type { ChatSocketClientOptions } from "./client/ChatSocketClient";

export type {
  Conversation,
  ConversationType,
  ExternalReference,
  Message,
  MessageEvent,
  MessageEventType,
  MessageType,
  PageResponse,
  Participant,
} from "./client/types";
