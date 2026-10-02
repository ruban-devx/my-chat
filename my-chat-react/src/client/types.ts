/**
 * Mirrors the JSON shapes served by my-chat-spring-boot-starter's REST/WebSocket API
 * (com.project.mychat.web.dto.*). Keep these in sync by hand — there's no shared schema
 * generation between the two repos yet.
 */

export type ConversationType = "DIRECT" | "GROUP";

export type MessageType = "TEXT" | "SYSTEM";

export interface ExternalReference {
  type: string | null;
  id: string | null;
}

export interface Conversation {
  id: string;
  type: ConversationType;
  externalReference: ExternalReference | null;
  createdAt: string;
  updatedAt: string;
}

export interface Participant {
  id: string;
  externalUserId: string;
  joinedAt: string;
}

export interface Message {
  id: string;
  conversationId: string;
  senderExternalUserId: string;
  messageType: MessageType;
  content: string;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export type MessageEventType = "message.created" | "message.updated" | "message.deleted";

export interface MessageEvent {
  type: MessageEventType;
  message: Message;
}
