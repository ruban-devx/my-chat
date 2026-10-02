package com.project.mychat.web;

import com.project.mychat.domain.Message;
import com.project.mychat.domain.MessageType;
import com.project.mychat.service.MessageService;
import com.project.mychat.web.dto.EditMessageRequest;
import com.project.mychat.web.dto.MessageResponse;
import com.project.mychat.web.dto.PageResponse;
import com.project.mychat.web.dto.SendMessageRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Message paths are top-level ({@code /api/chat/messages/{id}}) for edit/delete rather than
 * nested under a conversation, because {@link MessageService} identifies a message by its own
 * id alone — nesting under conversationId would invite a path/body mismatch that's never
 * actually checked.
 */
@RestController
@RequestMapping("/api/chat")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/conversations/{conversationId}/messages")
    public MessageResponse sendMessage(
            @PathVariable UUID conversationId, @Valid @RequestBody SendMessageRequest request, Principal principal) {
        String callerId = CallerId.require(principal);
        MessageType type = request.type() != null ? request.type() : MessageType.TEXT;
        Message message = messageService.sendMessage(conversationId, callerId, request.content(), type);
        return MessageResponse.from(message);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public PageResponse<MessageResponse> getMessages(
            @PathVariable UUID conversationId, Principal principal, @PageableDefault(size = 50) Pageable pageable) {
        String callerId = CallerId.require(principal);
        return PageResponse.from(messageService.getMessages(conversationId, callerId, pageable).map(MessageResponse::from));
    }

    @PatchMapping("/messages/{messageId}")
    public MessageResponse editMessage(
            @PathVariable UUID messageId, @Valid @RequestBody EditMessageRequest request, Principal principal) {
        String callerId = CallerId.require(principal);
        Message message = messageService.editMessage(messageId, callerId, request.content());
        return MessageResponse.from(message);
    }

    @DeleteMapping("/messages/{messageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMessage(@PathVariable UUID messageId, Principal principal) {
        String callerId = CallerId.require(principal);
        messageService.deleteMessage(messageId, callerId);
    }
}
