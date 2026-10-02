package com.project.mychat.service.exception;

import java.util.UUID;

public class MessageNotFoundException extends ChatException {

    public MessageNotFoundException(UUID messageId) {
        super("Message not found: " + messageId);
    }
}
