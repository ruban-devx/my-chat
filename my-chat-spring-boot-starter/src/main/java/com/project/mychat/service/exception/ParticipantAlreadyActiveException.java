package com.project.mychat.service.exception;

import java.util.UUID;

public class ParticipantAlreadyActiveException extends ChatException {

    public ParticipantAlreadyActiveException(UUID conversationId, String externalUserId) {
        super("User " + externalUserId + " is already an active participant of conversation " + conversationId);
    }
}
