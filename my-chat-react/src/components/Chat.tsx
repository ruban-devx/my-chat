import { useState } from "react";
import { useChatContext } from "../context/ChatProvider";
import { useConversations } from "../hooks/useConversations";
import { useMessages } from "../hooks/useMessages";
import type { Conversation } from "../client/types";
import { ConversationList } from "./ConversationList";
import { MessageComposer } from "./MessageComposer";
import { MessageList } from "./MessageList";

/**
 * The default, near-zero-config chat screen: `<ChatProvider currentUserId="..."><Chat /></ChatProvider>`.
 *
 * Deliberately renders only conversations that already exist — there's no "start a new
 * conversation" UI here. Chat doesn't own a user directory (see the ownership boundary in
 * PROJECTBRAIN), so it has no way to let someone pick who to message; that's the host
 * product's own UI, calling `useConversations().createConversation(...)` from wherever in its
 * app that decision actually gets made (e.g. a "Message seller" button on a listing page).
 */
export function Chat() {
  const { currentUserId } = useChatContext();
  const { conversations, loading: conversationsLoading } = useConversations();
  const [selected, setSelected] = useState<Conversation | null>(null);
  const { messages, sendMessage, editMessage, deleteMessage } = useMessages(selected?.id ?? null);

  return (
    <div className="mychat-shell">
      <div className="mychat-sidebar">
        {conversationsLoading ? (
          <div className="mychat-loading">Loading conversations…</div>
        ) : (
          <ConversationList conversations={conversations} selectedConversationId={selected?.id} onSelect={setSelected} />
        )}
      </div>
      <div className="mychat-main">
        {selected ? (
          <>
            <MessageList messages={messages} currentUserId={currentUserId} onEdit={editMessage} onDelete={deleteMessage} />
            <MessageComposer onSend={sendMessage} />
          </>
        ) : (
          <div className="mychat-empty-state">Select a conversation to start chatting.</div>
        )}
      </div>
    </div>
  );
}
