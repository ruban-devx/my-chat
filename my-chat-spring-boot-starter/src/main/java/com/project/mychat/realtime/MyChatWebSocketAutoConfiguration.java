package com.project.mychat.realtime;

import com.project.mychat.MyChatAutoConfiguration;
import com.project.mychat.service.ConversationService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Realtime is broadcast-only: clients keep writing through the REST API
 * ({@code ConversationController}/{@code MessageController}); this only pushes
 * {@code message.created}/{@code message.updated}/{@code message.deleted} events out to
 * subscribers, one topic per conversation. There's no {@code @MessageMapping} handler and no
 * {@code /app} destination prefix, because nothing is ever meant to arrive from the client
 * side over the socket.
 *
 * <p>Authentication on the handshake reuses the same {@code java.security.Principal} mechanism
 * as the REST layer: Spring's default {@code DefaultHandshakeHandler} already copies
 * {@code request.getUserPrincipal()} from the HTTP handshake request, so whatever the host
 * app's auth mechanism populates that with is what {@link ConversationSubscriptionInterceptor}
 * sees — no Spring Security dependency, no separate socket-auth mechanism to configure.
 */
@AutoConfiguration(after = MyChatAutoConfiguration.class)
@ConditionalOnBean(ConversationService.class)
@ConditionalOnProperty(prefix = "my-chat.realtime", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableWebSocketMessageBroker
public class MyChatWebSocketAutoConfiguration implements WebSocketMessageBrokerConfigurer {

    private final ConversationService conversationService;

    public MyChatWebSocketAutoConfiguration(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ConversationSubscriptionInterceptor(conversationService));
    }

    @Bean
    @ConditionalOnMissingBean
    public ChatEventBroadcaster chatEventBroadcaster(SimpMessagingTemplate messagingTemplate) {
        return new ChatEventBroadcaster(messagingTemplate);
    }
}
