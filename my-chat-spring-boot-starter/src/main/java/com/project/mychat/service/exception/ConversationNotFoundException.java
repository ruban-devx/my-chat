package com.project.mychat.service.exception;

import java.util.UUID;

public class ConversationNotFoundException extends ChatException {

    public ConversationNotFoundException(UUID conversationId) {
        super("Conversation not found: " + conversationId);
    }
}
