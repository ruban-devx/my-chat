package com.project.mychat.web.dto;

import com.project.mychat.domain.Message;
import com.project.mychat.domain.MessageType;
import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID conversationId,
        String senderExternalUserId,
        MessageType messageType,
        String content,
        Instant createdAt,
        Instant updatedAt) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderExternalUserId(),
                message.getMessageType(),
                message.getContent(),
                message.getCreatedAt(),
                message.getUpdatedAt());
    }
}
