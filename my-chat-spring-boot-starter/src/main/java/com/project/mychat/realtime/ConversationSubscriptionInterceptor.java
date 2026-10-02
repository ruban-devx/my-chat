package com.project.mychat.realtime;

import com.project.mychat.service.ConversationService;
import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;

/**
 * Rejects STOMP SUBSCRIBE frames unless the connected principal is an active participant of
 * the conversation named in the destination. Realtime here is broadcast-only — clients never
 * publish app messages over the socket, they only subscribe — so SUBSCRIBE is the only command
 * that needs an authorization check.
 */
class ConversationSubscriptionInterceptor implements ChannelInterceptor {

    static final Pattern CONVERSATION_TOPIC = Pattern.compile("^/topic/chat\\.conversations\\.([0-9a-fA-F-]{36})$");

    private final ConversationService conversationService;

    ConversationSubscriptionInterceptor(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.SUBSCRIBE != accessor.getCommand()) {
            return message;
        }

        String destination = accessor.getDestination();
        Matcher matcher = destination == null ? null : CONVERSATION_TOPIC.matcher(destination);
        if (matcher == null || !matcher.matches()) {
            throw new MessagingException("Unknown or malformed subscription destination: " + destination);
        }

        UUID conversationId = UUID.fromString(matcher.group(1));
        Principal principal = accessor.getUser();
        if (principal == null || !conversationService.isActiveParticipant(conversationId, principal.getName())) {
            throw new MessagingException("Not authorized to subscribe to conversation " + conversationId);
        }

        return message;
    }
}
