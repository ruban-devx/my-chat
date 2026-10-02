package com.project.mychat.realtime;

import com.project.mychat.event.MessageEvent;
import com.project.mychat.web.dto.MessageEventResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Relays {@link MessageEvent}s to STOMP subscribers. Listens {@code AFTER_COMMIT} deliberately
 * — {@code MessageService}'s methods are {@code @Transactional}, so a plain {@code @EventListener}
 * would fire mid-transaction and could broadcast a message that a later failure then rolls back,
 * announcing something that was never actually persisted.
 */
public class ChatEventBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatEventBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageEvent(MessageEvent event) {
        String destination = "/topic/chat.conversations." + event.message().getConversationId();
        messagingTemplate.convertAndSend(destination, MessageEventResponse.from(event));
    }
}
