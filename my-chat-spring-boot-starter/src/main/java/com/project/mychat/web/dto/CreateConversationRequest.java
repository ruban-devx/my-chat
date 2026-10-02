package com.project.mychat.web.dto;

import com.project.mychat.domain.ConversationType;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * The caller (from the request's authenticated principal) is always added as a participant,
 * in addition to whatever's listed here — a conversation you create always includes you.
 */
public record CreateConversationRequest(
        @NotNull ConversationType type, ExternalReferenceDto externalReference, List<String> participantExternalUserIds) {
}
