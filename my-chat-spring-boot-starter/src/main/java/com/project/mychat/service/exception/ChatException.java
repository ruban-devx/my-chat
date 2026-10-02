package com.project.mychat.service.exception;

/** Base type for all chat domain-rule violations raised by the service layer. */
public abstract class ChatException extends RuntimeException {

    protected ChatException(String message) {
        super(message);
    }
}
