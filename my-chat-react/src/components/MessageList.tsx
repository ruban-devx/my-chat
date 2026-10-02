import type { Message } from "../client/types";

export interface MessageListProps {
  messages: Message[];
  currentUserId: string;
  onEdit?: (messageId: string, content: string) => void;
  onDelete?: (messageId: string) => void;
}

/**
 * Intentionally minimal: no avatars, timestamps formatting, or grouping — this is the default
 * shell, not a polished chat UI. Edit/delete use plain `window.prompt`/click, standing in for
 * the custom message-renderer extension point the planning docs explicitly deferred until the
 * default UI itself was built.
 */
export function MessageList({ messages, currentUserId, onEdit, onDelete }: MessageListProps) {
  return (
    <div className="mychat-message-list">
      {messages.map((message) => {
        const isOwn = message.senderExternalUserId === currentUserId;
        return (
          <div key={message.id} className={`mychat-message${isOwn ? " mychat-message-own" : ""}`}>
            {!isOwn && <div className="mychat-message-sender">{message.senderExternalUserId}</div>}
            <div className="mychat-message-content">{message.content}</div>
            {isOwn && (onEdit || onDelete) && (
              <div className="mychat-message-actions">
                {onEdit && (
                  <button
                    type="button"
                    className="mychat-message-action"
                    onClick={() => {
                      const next = window.prompt("Edit message", message.content);
                      if (next != null && next.trim() && next.trim() !== message.content) {
                        onEdit(message.id, next.trim());
                      }
                    }}
                  >
                    Edit
                  </button>
                )}
                {onDelete && (
                  <button type="button" className="mychat-message-action" onClick={() => onDelete(message.id)}>
                    Delete
                  </button>
                )}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
