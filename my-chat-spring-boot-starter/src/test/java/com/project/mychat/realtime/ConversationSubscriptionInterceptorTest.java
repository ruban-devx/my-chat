package com.project.mychat.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.project.mychat.TestApplication;
import com.project.mychat.domain.Conversation;
import com.project.mychat.domain.ConversationType;
import com.project.mychat.service.ConversationService;
import java.security.Principal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Exercises the SUBSCRIBE-authorization logic directly against a real, Testcontainers-backed
 * {@link ConversationService} — no fake/mock participant lookups, and no real WebSocket
 * transport needed since {@link ConversationSubscriptionInterceptor} only ever looks at the
 * STOMP {@link Message} headers.
 */
@Testcontainers
@SpringBootTest(classes = TestApplication.class, properties = "spring.jpa.hibernate.ddl-auto=validate")
class ConversationSubscriptionInterceptorTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    ConversationService conversationService;

    private static Principal principal(String name) {
        return () -> name;
    }

    private static Message<byte[]> subscribe(String destination, Principal principal) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("test-session");
        accessor.setSubscriptionId("sub-0");
        accessor.setDestination(destination);
        if (principal != null) {
            accessor.setUser(principal);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void allowsAnActiveParticipantToSubscribeToTheirOwnConversation() {
        Conversation conversation =
                conversationService.createConversation(ConversationType.DIRECT, null, List.of("user-alice"));
        ConversationSubscriptionInterceptor interceptor = new ConversationSubscriptionInterceptor(conversationService);

        Message<byte[]> frame = subscribe("/topic/chat.conversations." + conversation.getId(), principal("user-alice"));
        assertThat(interceptor.preSend(frame, null)).isSameAs(frame);
    }

    @Test
    void rejectsANonParticipant() {
        Conversation conversation =
                conversationService.createConversation(ConversationType.DIRECT, null, List.of("user-alice"));
        ConversationSubscriptionInterceptor interceptor = new ConversationSubscriptionInterceptor(conversationService);

        Message<byte[]> frame = subscribe("/topic/chat.conversations." + conversation.getId(), principal("user-eve"));
        assertThatThrownBy(() -> interceptor.preSend(frame, null)).isInstanceOf(MessagingException.class);
    }

    @Test
    void rejectsAMissingPrincipal() {
        Conversation conversation =
                conversationService.createConversation(ConversationType.DIRECT, null, List.of("user-alice"));
        ConversationSubscriptionInterceptor interceptor = new ConversationSubscriptionInterceptor(conversationService);

        Message<byte[]> frame = subscribe("/topic/chat.conversations." + conversation.getId(), null);
        assertThatThrownBy(() -> interceptor.preSend(frame, null)).isInstanceOf(MessagingException.class);
    }

    @Test
    void rejectsAMalformedDestination() {
        ConversationSubscriptionInterceptor interceptor = new ConversationSubscriptionInterceptor(conversationService);

        Message<byte[]> frame = subscribe("/topic/something-else", principal("user-alice"));
        assertThatThrownBy(() -> interceptor.preSend(frame, null)).isInstanceOf(MessagingException.class);
    }

    @Test
    void nonSubscribeCommandsPassThroughUnchecked() {
        ConversationSubscriptionInterceptor interceptor = new ConversationSubscriptionInterceptor(conversationService);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setLeaveMutable(true);
        Message<byte[]> frame = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        assertThat(interceptor.preSend(frame, null)).isSameAs(frame);
    }
}
