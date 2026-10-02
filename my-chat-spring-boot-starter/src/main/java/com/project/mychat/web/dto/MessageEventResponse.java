package com.project.mychat.web.dto;

import com.project.mychat.event.MessageEvent;

/** Wire shape broadcast over {@code /topic/chat.conversations.{conversationId}}. */
public record MessageEventResponse(String type, MessageResponse message) {

    public static MessageEventResponse from(MessageEvent event) {
        String type =
                switch (event.type()) {
                    case CREATED -> "message.created";
                    case UPDATED -> "message.updated";
                    case DELETED -> "message.deleted";
                };
        return new MessageEventResponse(type, MessageResponse.from(event.message()));
    }
}
