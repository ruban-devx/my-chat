package com.project.mychat.event;

import com.project.mychat.domain.Message;

/**
 * Published by {@code MessageService} after a message is created, edited, or deleted. Plain
 * Spring application event — deliberately has no dependency on messaging/WebSocket, so the
 * service layer stays technology-agnostic; something in the {@code realtime} package is what
 * turns this into a STOMP broadcast, if realtime is enabled at all.
 */
public record MessageEvent(Type type, Message message) {

    public enum Type {
        CREATED,
        UPDATED,
        DELETED
    }
}
