package com.project.mychat.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.project.mychat.domain.Message;
import com.project.mychat.event.MessageEvent;
import com.project.mychat.web.dto.MessageEventResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/** Pure unit test — no Spring context — of the event-type-to-topic/payload mapping. */
class ChatEventBroadcasterTest {

    @Test
    void relaysEachEventTypeToTheConversationTopicWithTheRightPayloadType() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        ChatEventBroadcaster broadcaster = new ChatEventBroadcaster(messagingTemplate);

        UUID conversationId = UUID.randomUUID();
        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setConversationId(conversationId);
        message.setSenderExternalUserId("user-alice");
        message.setContent("hello");

        broadcaster.onMessageEvent(new MessageEvent(MessageEvent.Type.CREATED, message));

        verify(messagingTemplate)
                .convertAndSend(eq("/topic/chat.conversations." + conversationId), any(MessageEventResponse.class));
    }

    @Test
    void mapsEventTypesToTheDocumentedEventNames() {
        UUID conversationId = UUID.randomUUID();
        Message message = new Message();
        message.setId(UUID.randomUUID());
        message.setConversationId(conversationId);
        message.setSenderExternalUserId("user-alice");
        message.setContent("hello");

        assertThat(MessageEventResponse.from(new MessageEvent(MessageEvent.Type.CREATED, message)).type())
                .isEqualTo("message.created");
        assertThat(MessageEventResponse.from(new MessageEvent(MessageEvent.Type.UPDATED, message)).type())
                .isEqualTo("message.updated");
        assertThat(MessageEventResponse.from(new MessageEvent(MessageEvent.Type.DELETED, message)).type())
                .isEqualTo("message.deleted");
    }
}
