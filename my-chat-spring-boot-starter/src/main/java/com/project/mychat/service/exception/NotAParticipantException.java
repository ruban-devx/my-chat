package com.project.mychat.service.exception;

import java.util.UUID;

/** Raised when someone who isn't an active participant of a conversation tries to act on it. */
public class NotAParticipantException extends ChatException {

    public NotAParticipantException(UUID conversationId, String externalUserId) {
        super("User " + externalUserId + " is not an active participant of conversation " + conversationId);
    }
}
