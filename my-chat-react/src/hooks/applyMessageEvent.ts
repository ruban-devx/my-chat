import type { Message, MessageEvent } from "../client/types";

/**
 * Folds one realtime event into a message list. Exported standalone (not inlined in
 * useMessages) because it's the one piece of genuinely trick logic in the realtime/REST
 * reconciliation — worth a direct unit test rather than only exercising it through a hook.
 */
export function applyMessageEvent(messages: Message[], event: MessageEvent): Message[] {
  switch (event.type) {
    case "message.created":
      // The sender already gets their own message appended from the REST response; this
      // guards against also double-adding it when their own broadcast echo arrives.
      return messages.some((m) => m.id === event.message.id) ? messages : [...messages, event.message];
    case "message.updated":
      return messages.map((m) => (m.id === event.message.id ? event.message : m));
    case "message.deleted":
      return messages.filter((m) => m.id !== event.message.id);
    default:
      return messages;
  }
}
