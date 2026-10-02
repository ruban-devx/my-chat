package com.project.mychat.service.exception;

import java.util.UUID;

/** Raised when a user tries to edit or delete a message they didn't send. */
public class ForbiddenMessageActionException extends ChatException {

    public ForbiddenMessageActionException(UUID messageId, String externalUserId) {
        super("User " + externalUserId + " may not modify message " + messageId + " sent by someone else");
    }
}
