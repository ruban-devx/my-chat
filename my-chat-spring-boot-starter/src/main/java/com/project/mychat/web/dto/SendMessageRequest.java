package com.project.mychat.web.dto;

import com.project.mychat.domain.MessageType;
import jakarta.validation.constraints.NotBlank;

/** {@code type} defaults to {@link MessageType#TEXT} when omitted. */
public record SendMessageRequest(@NotBlank String content, MessageType type) {
}
