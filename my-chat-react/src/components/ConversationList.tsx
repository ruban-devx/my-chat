import type { Conversation } from "../client/types";

export interface ConversationListProps {
  conversations: Conversation[];
  selectedConversationId?: string | null;
  onSelect: (conversation: Conversation) => void;
}

export function ConversationList({ conversations, selectedConversationId, onSelect }: ConversationListProps) {
  return (
    <div className="mychat-conversation-list">
      {conversations.map((conversation) => (
        <button
          key={conversation.id}
          type="button"
          className={`mychat-conversation-item${conversation.id === selectedConversationId ? " mychat-conversation-item-selected" : ""}`}
          onClick={() => onSelect(conversation)}
        >
          {describeConversation(conversation)}
        </button>
      ))}
    </div>
  );
}

/**
 * Chat has no display-name concept (see "reuse mechanics, not business meaning" in
 * PROJECTBRAIN) — externalReference is the only product-supplied context available, so that's
 * what's shown when present; otherwise this falls back to the bare conversation type.
 */
function describeConversation(conversation: Conversation): string {
  if (conversation.externalReference?.id) {
    return `${conversation.externalReference.type ?? "Reference"} ${conversation.externalReference.id}`;
  }
  return conversation.type === "DIRECT" ? "Direct conversation" : "Group conversation";
}
