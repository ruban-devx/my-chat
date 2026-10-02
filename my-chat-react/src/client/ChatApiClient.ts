import type { Conversation, ConversationType, ExternalReference, Message, MessageType, PageResponse, Participant } from "./types";

export class ChatApiError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message);
    this.name = "ChatApiError";
  }
}

export interface ChatApiClientOptions {
  /** Default "/api/chat" — matches the starter's default REST base path. */
  baseUrl?: string;
  /**
   * Merged into every request. Defaults to `{ credentials: "include" }`, which is enough for
   * cookie/session-based auth with zero extra config. For token-based auth, supply headers
   * here (e.g. `{ headers: { Authorization: "Bearer ..." } }`) — re-create the client (or a
   * fresh options object) if the token can change during the session.
   */
  fetchOptions?: RequestInit;
}

export interface CreateConversationRequest {
  type: ConversationType;
  externalReference?: ExternalReference | null;
  participantExternalUserIds?: string[];
}

/** Thin wrapper over the REST API — one method per endpoint, no framework dependency. */
export class ChatApiClient {
  private readonly baseUrl: string;
  private readonly fetchOptions: RequestInit;

  constructor(options: ChatApiClientOptions = {}) {
    this.baseUrl = options.baseUrl ?? "/api/chat";
    this.fetchOptions = options.fetchOptions ?? { credentials: "include" };
  }

  createConversation(request: CreateConversationRequest): Promise<Conversation> {
    return this.request<Conversation>("/conversations", { method: "POST", body: JSON.stringify(request) });
  }

  getConversations(): Promise<Conversation[]> {
    return this.request<Conversation[]>("/conversations");
  }

  addParticipant(conversationId: string, externalUserId: string): Promise<Participant> {
    return this.request<Participant>(`/conversations/${encodeURIComponent(conversationId)}/participants`, {
      method: "POST",
      body: JSON.stringify({ externalUserId }),
    });
  }

  removeParticipant(conversationId: string, externalUserId: string): Promise<void> {
    return this.request<void>(
      `/conversations/${encodeURIComponent(conversationId)}/participants/${encodeURIComponent(externalUserId)}`,
      { method: "DELETE" },
    );
  }

  sendMessage(conversationId: string, content: string, type: MessageType = "TEXT"): Promise<Message> {
    return this.request<Message>(`/conversations/${encodeURIComponent(conversationId)}/messages`, {
      method: "POST",
      body: JSON.stringify({ content, type }),
    });
  }

  /** Matches the backend's default/max page size expectations; newest-first, like the API. */
  getMessages(conversationId: string, page = 0, size = 50): Promise<PageResponse<Message>> {
    return this.request<PageResponse<Message>>(
      `/conversations/${encodeURIComponent(conversationId)}/messages?page=${page}&size=${size}`,
    );
  }

  editMessage(messageId: string, content: string): Promise<Message> {
    return this.request<Message>(`/messages/${encodeURIComponent(messageId)}`, {
      method: "PATCH",
      body: JSON.stringify({ content }),
    });
  }

  deleteMessage(messageId: string): Promise<void> {
    return this.request<void>(`/messages/${encodeURIComponent(messageId)}`, { method: "DELETE" });
  }

  private async request<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(`${this.baseUrl}${path}`, {
      ...this.fetchOptions,
      ...init,
      headers: {
        "Content-Type": "application/json",
        ...this.fetchOptions.headers,
        ...init?.headers,
      },
    });

    if (!response.ok) {
      throw new ChatApiError(response.status, await extractErrorDetail(response));
    }
    if (response.status === 204) {
      return undefined as T;
    }
    return (await response.json()) as T;
  }
}

async function extractErrorDetail(response: Response): Promise<string> {
  try {
    const problem = (await response.json()) as { detail?: string };
    if (problem?.detail) {
      return problem.detail;
    }
  } catch {
    // Response body wasn't JSON (or was empty) — fall through to the status text.
  }
  return response.statusText || `Request failed with status ${response.status}`;
}
