package com.project.mychat.web.dto;

import com.project.mychat.domain.Conversation;
import com.project.mychat.domain.ConversationType;
import com.project.mychat.domain.ExternalReference;
import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID id, ConversationType type, ExternalReferenceDto externalReference, Instant createdAt, Instant updatedAt) {

    public static ConversationResponse from(Conversation conversation) {
        ExternalReference reference = conversation.getExternalReference();
        ExternalReferenceDto referenceDto =
                (reference == null || (reference.getType() == null && reference.getId() == null))
                        ? null
                        : new ExternalReferenceDto(reference.getType(), reference.getId());
        return new ConversationResponse(
                conversation.getId(), conversation.getType(), referenceDto, conversation.getCreatedAt(), conversation.getUpdatedAt());
    }
}
